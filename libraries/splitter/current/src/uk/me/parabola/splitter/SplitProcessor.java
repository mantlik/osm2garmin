/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.splitter;

import uk.me.parabola.splitter.Relation.Member;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Splits a map into multiple areas.
 */
class SplitProcessor implements MapProcessor {
	private final static int DICT_START = -1 * (Short.MIN_VALUE + 1);
	private final short unassigned = Short.MIN_VALUE;
	private SparseLong2ShortMapFunction coords;
	private SparseLong2ShortMapFunction ways;
	private final Grid grid;

	private final OSMWriter[] writers;
	private final InputQueueInfo[] writerInputQueues;
	private final BlockingQueue<InputQueueInfo> toProcess;
	private final ArrayList<Thread> workerThreads;
	private final InputQueueInfo STOP_MSG = new InputQueueInfo(null);
	private final WriterDictionary writerDictionary;


	// private int currentNodeAreaSet;
	private BitSet currentWayAreaSet;
	private BitSet currentRelAreaSet;
	private BitSet usedWriters;
	
	private final int maxThreads;

	//	for statistics
	//private long countQuickTest = 0;
	//private long countFullTest = 0;
	private long countCoords = 0;
	private long countWays = 0;
	
	SplitProcessor(OSMWriter[] writers, int maxThreads) {
		this.writers = writers;
		usedWriters = new BitSet();
		writerDictionary = new WriterDictionary();
		this.grid = new Grid();
		this.coords = new SparseLong2ShortMapInline();
		this.ways   = new SparseLong2ShortMapInline();
		this.coords.defaultReturnValue(unassigned);
		this.ways.defaultReturnValue(unassigned);
		this.maxThreads = maxThreads;
		this.toProcess = new ArrayBlockingQueue<InputQueueInfo>(writers.length);
		this.writerInputQueues = new InputQueueInfo[writers.length];
		for (int i = 0; i < writerInputQueues.length; i++) {
			writerInputQueues[i] = new InputQueueInfo(this.writers[i]);
		}
		currentWayAreaSet = new BitSet(writers.length);
		currentRelAreaSet = new BitSet(writers.length);

		int noOfWorkerThreads = this.maxThreads - 1;
		workerThreads = new ArrayList<Thread>(noOfWorkerThreads);
		for (int i = 0; i < noOfWorkerThreads; i++) {
			Thread worker = new Thread(new OSMWriterWorker());
			worker.setName("worker-" + i);
			workerThreads.add(worker);
			worker.start();
		}
	}

	@Override
	public boolean isStartNodeOnly() {
		return false;
	}

	@Override
	public void boundTag(Area bounds) {
	}

	@Override
	public void processNode(Node n) {
		try {
			writeNode(n);
		} catch (IOException e) {
			throw new RuntimeException("failed to write node " + n.getId(), e);
		}
	}

	@Override
	public void processWay(Way w) {
		short oldclIndex = unassigned;
		for (long id : w.getRefs()) {
			// Get the list of areas that the way is in. 
			short clIdx = coords.get(id);
			if (clIdx != unassigned){
				if (oldclIndex != clIdx){ 
					BitSet cl = writerDictionary.getBitSet(clIdx);
					currentWayAreaSet.or(cl);
					oldclIndex = clIdx;
				}
			}
		}
		try {
			writeWay(w);
			currentWayAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write way " + w.getId(), e);
		}
	}

	@Override
	public void processRelation(Relation r) {
		short oldclIndex = unassigned;
		short oldwlIndex = unassigned;
		try {
			for (Member mem : r.getMembers()) {
				// String role = mem.getRole();
				long id = mem.getRef();
				if (mem.getType().equals("node")) {
					short clIdx = coords.get(id);

					if (clIdx != unassigned){
						if (oldclIndex != clIdx){ 
							BitSet wl = writerDictionary.getBitSet(clIdx);
							currentRelAreaSet.or(wl);
						}
						oldclIndex = clIdx;

					}

				} else if (mem.getType().equals("way")) {
					short wlIdx = ways.get(id);

					if (wlIdx != unassigned){
						if (oldwlIndex != wlIdx){ 
							BitSet wl = writerDictionary.getBitSet(wlIdx);
							currentRelAreaSet.or(wl);
			}
						oldwlIndex = wlIdx;
					}
				}
			}

			writeRelation(r);
			currentRelAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write relation " + r.getId(),
					e);
		}
	}

	@Override
	public void endMap() {
		System.out.println("***********************************************************");
		System.out.println("Final statistics");
		System.out.println("***********************************************************");
		System.out.println("Needed dictionary entries: " + writerDictionary.size() + " of " + ((1<<16) - 1));
		System.out.println("coords occupancy");
		System.out.println("MAP occupancy: " + Utils.format(countCoords));
		coords.stats(1);
		System.out.println("ways occupancy");
		System.out.println("MAP occupancy: " + Utils.format(countWays));
		ways.stats(1);
		System.out.println("");
		//System.out.println("Full Node tests:  " + Util.format(countFullTest));
		//System.out.println("Quick Node tests: " + Util.format(countQuickTest));
		for (int i = 0; i < writerInputQueues.length; i++) {
			try {
				writerInputQueues[i].stop();
			} catch (InterruptedException e) {
				throw new RuntimeException(
						"Failed to add the stop element for worker thread " + i,
						e);
			}
		}
		try {
			if (maxThreads > 1)
				toProcess.put(STOP_MSG);// Magic flag used to indicate that all data is done.

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (Thread workerThread : workerThreads) {
			try {
				workerThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException("Failed to join for thread "
						+ workerThread.getName(), e);
			}
		}
		for (OSMWriter writer : writers) {
			writer.finishWrite();
		}
	}

	private void writeNode(Node currentNode) throws IOException {
		int countWriters = 0;
		short lastUsedWriter = unassigned;
		
		GridResult writerCandidates = grid.get(currentNode);
		if (writerCandidates == null) {
			return;
		}
		if (writerCandidates.l.size() > 1)
			usedWriters.clear();
		for (int i = 0; i < writerCandidates.l.size(); i++) {
			int n = writerCandidates.l.get(i);
			OSMWriter w = writers[n];
			boolean found;
			if (writerCandidates.testNeeded){
				found = w.nodeBelongsToThisArea(currentNode);
				//++countFullTest;
			}
			else{ 
				found = true;
				//++countQuickTest;
			}
			if (found) {
				usedWriters.set(n);
				++countWriters;
				lastUsedWriter = (short) n;
				if (maxThreads > 1) {
					addToWorkingQueue(n, currentNode);
				} else {
					w.write(currentNode);
				}
			}
		}
		if (countWriters > 0){
			short writersID;
			if (countWriters > 1)
				writersID = writerDictionary.translate(usedWriters);
			else  
				writersID = (short) (lastUsedWriter  - DICT_START); // no need to do lookup in the dictionary 
			coords.put(currentNode.getId(), writersID);
			++countCoords;
			if (countCoords % 1000000 == 0){
				System.out.println("MAP occupancy: " + Utils.format(countCoords) + ", number of area dictionary entries: " + writerDictionary.size() + " of " + ((1<<16) - 1));
				coords.stats(0);
	}
		}
	}

	private boolean seenWay;

	private void writeWay(Way currentWay) throws IOException {
		if (!seenWay) {
			seenWay = true;
			System.out.println("Writing ways " + new Date());
		}
		
		if (!currentWayAreaSet.isEmpty()) {
				for (int n = currentWayAreaSet.nextSetBit(0); n >= 0; n = currentWayAreaSet.nextSetBit(n + 1)) {
					if (maxThreads > 1) {
						addToWorkingQueue(n, currentWay);
					} else {
						writers[n].write(currentWay);
					}
				}
			short idx;
			// store these areas in ways map
			idx = writerDictionary.translate(currentWayAreaSet);
			ways.put(currentWay.getId(), idx);
			++countWays;
			if (countWays % 1000000 == 0){
				System.out.println("MAP occupancy: " + Utils.format(countWays) + ", number of area dictionary entries: " + writerDictionary.size() + " of " + ((1<<16) - 1));
				ways.stats(0);
		}
	}
	}

	private boolean seenRel;

	private void writeRelation(Relation currentRelation) throws IOException {
		if (!seenRel) {
			seenRel = true;
			System.out.println("Writing relations " + new Date());
		}
		for (int n = currentRelAreaSet.nextSetBit(0); n >= 0; n = currentRelAreaSet.nextSetBit(n + 1)) {
			// if n is out of bounds, then something has gone wrong
			if (maxThreads > 1) {
				addToWorkingQueue(n, currentRelation);
			} else {
				writers[n].write(currentRelation);
			}
		}
	}

	private void addToWorkingQueue(int writerNumber, Element element) {
		try {
			writerInputQueues[writerNumber].put(element);
		} catch (InterruptedException e) {
			// throw new RuntimeException("Failed to write node " +
			// element.getId() + " to worker thread " + writerNumber, e);
		}
	}

	private class InputQueueInfo {
		private final OSMWriter writer;
		private ArrayList<Element> staging;
		private final BlockingQueue<ArrayList<Element>> inputQueue;

		public InputQueueInfo(OSMWriter writer) {
			inputQueue =  new ArrayBlockingQueue<ArrayList<Element>>(NO_ELEMENTS);
			this.writer = writer;
			this.staging = new ArrayList<Element>(STAGING_SIZE);
		}

		void put(Element e) throws InterruptedException {
			staging.add(e);
			if (staging.size() < STAGING_SIZE)
				return;
			flush();
		}

		void flush() throws InterruptedException {
			// System.out.println("Flush");
			inputQueue.put(staging);
			staging = new ArrayList<Element>(STAGING_SIZE);
			toProcess.put(this);
		}

		void stop() throws InterruptedException {
			flush();
		}
	}

	public static final int NO_ELEMENTS = 3;
	final int STAGING_SIZE = 300;

	private class OSMWriterWorker implements Runnable {

		public void processElement(Element element, OSMWriter writer)
				throws IOException {
			if (element instanceof Node) {
				writer.write((Node) element);
			} else if (element instanceof Way) {
				writer.write((Way) element);
			} else if (element instanceof Relation) {
				writer.write((Relation) element);
			}
		}

		@Override
		public void run() {
			boolean finished = false;
			while (!finished) {
				InputQueueInfo workPackage = null;
				try {
					workPackage = toProcess.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					continue;
				}
				if (workPackage == STOP_MSG) {
					try {
						toProcess.put(STOP_MSG); // Re-inject it so that other
													// threads know that we're
													// exiting.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					finished = true;
				} else {
					synchronized (workPackage) {
					while (!workPackage.inputQueue.isEmpty()) {
							ArrayList<Element> elements = null;
						try {
							elements = workPackage.inputQueue.poll();
								for (Element element : elements) {
								processElement(element, workPackage.writer);
							}

						} catch (IOException e) {
								throw new RuntimeException("Thread "
										+ Thread.currentThread().getName()
										+ " failed to write element ", e);
						}
					}
					}

				}
			}
			System.out.println("Thread " + Thread.currentThread().getName()
					+ " has finished");
		}
	}

	private class GridResult{
		ShortArrayList l;
		boolean testNeeded;
	
	}
	
	private class Grid{
		private final static int gridDimLon = 512; 
		private final static int gridDimLat = 512; 
		private int gridDivLon, gridDivLat;
		private int gridMinLat, gridMinLon; 
		// bounds of the complete grid
		private Area bounds;
		private short [][] grid;
		private boolean [][] gridTest;
		final GridResult r;

		Grid(){
			r = new GridResult();
			long start = System.currentTimeMillis();
			makeWriterGrid();
			System.out.println("Grid was created in " + (System.currentTimeMillis() - start) + " ms");
		}

		void makeWriterGrid() {
			int gridStepLon, gridStepLat;
			int minLat = Integer.MAX_VALUE, maxLat = Integer.MIN_VALUE;
			int minLon = Integer.MAX_VALUE, maxLon = Integer.MIN_VALUE;

			for (OSMWriter w : writers) {
				if (w.getExtendedBounds().getMinLat() < minLat)
					minLat = w.getExtendedBounds().getMinLat();
				if (w.getExtendedBounds().getMinLong() < minLon)
					minLon = w.getExtendedBounds().getMinLong();
				if (w.getExtendedBounds().getMaxLat() > maxLat)
					maxLat = w.getExtendedBounds().getMaxLat();
				if (w.getExtendedBounds().getMaxLong() > maxLon)
					maxLon = w.getExtendedBounds().getMaxLong();
			}

			// save these results for later use
			gridMinLon = minLon;
			gridMinLat = minLat;
			bounds = new Area(minLat, minLon, maxLat, maxLon);

			int diffLon = maxLon - minLon;
			int diffLat = maxLat - minLat;
			gridDivLon = Math.round((diffLon / gridDimLon + 0.5f) );
			gridDivLat = Math.round((diffLat / gridDimLat + 0.5f));

			gridStepLon = Math.round(((diffLon) / gridDimLon) + 0.5f);
			gridStepLat = Math.round(((diffLat) / gridDimLat) + 0.5f);
			assert gridStepLon * gridDimLon >= diffLon : "gridStepLon is too small";
			assert gridStepLat * gridDimLat >= diffLat : "gridStepLat is too small";
			grid = new short[gridDimLon + 1][gridDimLat + 1];
			gridTest = new boolean[gridDimLon + 1][gridDimLat + 1];

			int maxWriterSearch = 0;
			BitSet writerSet = new BitSet(); 

			// start identifying the writer areas that intersect each grid tile
			for (int lon = 0; lon <= gridDimLon; lon++) {
				int testMinLon = gridMinLon + gridStepLon * lon;
				for (int lat = 0; lat <= gridDimLon; lat++) {
					int testMinLat = gridMinLat + gridStepLat * lat;
					writerSet.clear();
					int len = 0;
					
					for (int j = 0; j < writers.length; j++) {
						OSMWriter w = writers[j];
						// find grid areas that intersect with the writers' area
						int tminLat = Math.max(testMinLat, w.extendedBounds.getMinLat());
						int tminLon = Math.max(testMinLon, w.extendedBounds.getMinLong());
						int tmaxLat = Math.min(testMinLat + gridStepLat,w.extendedBounds.getMaxLat());
						int tmaxLon = Math.min(testMinLon + gridStepLon,w.extendedBounds.getMaxLong());
						if (tminLat <= tmaxLat && tminLon <= tmaxLon) {
							// yes, they intersect
							len++;
							writerSet.set(j);
							if (!w.extendedBounds.contains(testMinLat, testMinLon)|| !w.extendedBounds.contains(testMinLat+ gridStepLat, testMinLon+ gridStepLon)){
								// grid area is completely within writer area 
								gridTest[lon][lat] = true;
							}
						}
					}
					maxWriterSearch = Math.max(maxWriterSearch, len);
					if (len >  0)
						grid[lon][lat] = writerDictionary.translate(writerSet);
					else 
						grid[lon][lat] = unassigned;
				}
			}

			System.out.println("Grid [" + gridDimLon + "][" + gridDimLat + "] for grid area " + bounds + 
					" requires max. " + maxWriterSearch + " checks for each node.");
		}

		public GridResult get(final Node n){
			int nMapLon = n.getMapLon();
			int nMapLat = n.getMapLat();
			if (!bounds.contains(nMapLat, nMapLon)) 
				return null;
			int gridLonIdx = (nMapLon - gridMinLon ) / gridDivLon; 
			int gridLatIdx = (nMapLat - gridMinLat ) / gridDivLat;

			// get list of writer candidates from grid
			short idx = grid[gridLonIdx][gridLatIdx];
			if (idx == unassigned) return null;
			r.testNeeded = gridTest[gridLonIdx][gridLatIdx];
			r.l = writerDictionary.getList(idx);
			return r; 		
		}
	}
	
	private class WriterDictionary{
		
		private final ObjectArrayList<BitSet> sets; 
		private final ObjectArrayList<ShortArrayList> arrays; 
		HashMap<BitSet, Short> index;
		WriterDictionary (){
			sets = new ObjectArrayList<BitSet>();
			arrays = new ObjectArrayList<ShortArrayList>();
			index = new HashMap<BitSet, Short>();
			init();
		}
		private void init(){
			BitSet b = new BitSet();
			for (int i=0; i < writers.length; i++){
				b.set(i);
				translate(b);
				b.clear();
			}

		}
		public short translate(final BitSet writerSet){
			Short combiIndex = index.get(writerSet);
			if (combiIndex == null){
				BitSet bnew = new BitSet();
				
				bnew.or(writerSet);
				ShortArrayList a = new ShortArrayList();
				for (int i = writerSet.nextSetBit(0); i >= 0; i = writerSet.nextSetBit(i + 1)) {
					a.add((short) i);
				}
				combiIndex = (short) (sets.size() - DICT_START);
				if (combiIndex == Short.MAX_VALUE){
					throw new RuntimeException("writerDictionary is full. Decrease --max-areas value");
				}
				sets.add(bnew);
				arrays.add(a);
				index.put(bnew, combiIndex);
			}
			return combiIndex;
		}
		
		public BitSet getBitSet (final short idx){
			return sets.get(idx + DICT_START);
		}
		public ShortArrayList getList (final short idx){
			return arrays.get(idx + DICT_START);
		}
		public int size(){
			return sets.size();
		}
		
	}
	
}
