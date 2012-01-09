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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Splits a map into multiple areas.
 */
class SplitProcessor implements MapProcessor {

	private final SparseInt2ShortMultiMap coords = new SparseInt2ShortMultiMap((short) -1,2);
	private final SparseInt2ShortMultiMap ways = new SparseInt2ShortMultiMap((short) -1,2);

	private final OSMWriter[] writers;
	private final InputQueueInfo[] writerInputQueues;
	private final BlockingQueue<InputQueueInfo> toProcess;
	private final ArrayList<Thread> workerThreads;
	private final InputQueueInfo STOP_MSG = new InputQueueInfo(null);

	private int currentNodeAreaSet;
	private BitSet currentWayAreaSet;
	private BitSet currentRelAreaSet;
	
	private final int maxThreads;

	int lats[];
	ArrayList<OSMWriter> writersets[];
	
	HashMap<OSMWriter,Integer> writerToID = new HashMap<OSMWriter,Integer>();
	
	void makeWriterMap() {
		TreeMap<Integer,ArrayList<OSMWriter>> writermap = new TreeMap<Integer,ArrayList<OSMWriter>>();
		for (int i = 0 ; i < writers.length ; i++) {
			writerToID.put(writers[i],i);
		}
		
		for (OSMWriter w : writers) {
			writermap.put(w.getExtendedBounds().getMinLat(),new ArrayList<OSMWriter>());
			writermap.put(w.getExtendedBounds().getMaxLat(),new ArrayList<OSMWriter>());
		}
		// Sentinel keys
		writermap.put(Integer.MIN_VALUE,new ArrayList<OSMWriter>());
		writermap.put(Integer.MAX_VALUE,new ArrayList<OSMWriter>());

		for (OSMWriter w: writers) {
			int minlat = w.getExtendedBounds().getMinLat();
			int maxlat = w.getExtendedBounds().getMaxLat();
			for (Integer i = minlat ; i != null && i <= maxlat;  i = writermap.higherKey(i)) {
				writermap.get(i).add(w);
			}
		}
		lats = new int[writermap.size()];
		writersets = new ArrayList[writermap.size()];
		int i = 0;
		for (Entry<Integer, ArrayList<OSMWriter>> e : writermap.entrySet()) {
			lats[i] =  e.getKey();
			writersets[i] = e.getValue();
			i++;
		}
	}

	SplitProcessor(OSMWriter[] writers, int maxThreads) {
		this.writers = writers;
		makeWriterMap();
		this.maxThreads = maxThreads;
		this.toProcess = new ArrayBlockingQueue<InputQueueInfo>(writers.length); 
		this.writerInputQueues = new InputQueueInfo[writers.length];
		for (int i = 0; i < writerInputQueues.length;i++) {
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
			currentNodeAreaSet = 0;
		} catch (IOException e) {
			throw new RuntimeException("failed to write node " + n.getId(), e);
		}
	}

	@Override
	public void processWay(Way w) {

		for (int id: w.getRefs()) {
			// Get the list of areas that the node is in.  A node may be in
			// more than one area because of overlap.
			coords.addTo(id, currentWayAreaSet);
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
		try {
			for (Member mem : r.getMembers()) {
				String role = mem.getRole();
				int id = mem.getRef();
				if (mem.getType().equals("node")) {
					coords.addTo(id, currentRelAreaSet);
				} else if (mem.getType().equals("way")) {
					ways.addTo(id, currentRelAreaSet);
				}			
			}
			
			writeRelation(r);
			currentRelAreaSet.clear();
		} catch (IOException e) {
			throw new RuntimeException("failed to write relation " + r.getId(), e);
		}
	}

	@Override
	public void endMap() {
		System.out.println("coords occupancy");
		coords.stats();
		System.out.println("ways occupancy");
		ways.stats();
		for (int i = 0; i < writerInputQueues.length; i++) {
			try {
				writerInputQueues[i].stop();
			} catch (InterruptedException e) {
				throw new RuntimeException("Failed to add the stop element for worker thread " + i, e);
			}
		}
		try {
			toProcess.put(STOP_MSG);// Magic flag used to indicate that all data is done.

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 
		
		for (Thread workerThread : workerThreads) {
			try {
				workerThread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException("Failed to join for thread " + workerThread.getName(), e);
			}
		}
		for (OSMWriter writer : writers) {
			writer.finishWrite();
		}
	}

	private void writeNode(Node currentNode) throws IOException {
		int index = Arrays.binarySearch(lats, currentNode.getMapLat());
		if (index < 0)
			index = -index-1;

		//System.out.println("Send to "+entry.getValue().size());
		for (OSMWriter w : writersets[index]) {
			int n = writerToID.get(w);
			boolean found = w.nodeBelongsToThisArea(currentNode); 
			if (found) {
				if (maxThreads > 1) {
					addToWorkingQueue(n, currentNode);
				} else {
					w.write(currentNode);
				}
				coords.put(currentNode.getId(), (short)n);
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
				// this way falls into 4 or less areas (the normal case). Store these areas in the ways map
				for (int n = currentWayAreaSet.nextSetBit(0); n >= 0; n = currentWayAreaSet.nextSetBit(n + 1)) {
					if (maxThreads > 1) {
						addToWorkingQueue(n, currentWay);
					} else {
						writers[n].write(currentWay);
					}
					// add one to the area so we're in the range 1-255. This is because we treat 0 as the
					// equivalent of a null
					ways.put(currentWay.getId(), (short) n);
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
			//throw new RuntimeException("Failed to write node " + element.getId() + " to worker thread " + writerNumber, e);
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
			//System.out.println("Flush");
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

		public void processElement(Element element, OSMWriter writer) throws IOException {
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
				if (workPackage==STOP_MSG) {
					try {
						toProcess.put(STOP_MSG); // Re-inject it so that other threads know that we're exiting.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					finished=true;
				} else {
					synchronized (workPackage) {
					while (!workPackage.inputQueue.isEmpty()) {
						ArrayList<Element> elements =null;
						try {
							elements = workPackage.inputQueue.poll();
							for (Element element : elements ) {
								processElement(element, workPackage.writer);
							}
							
						} catch (IOException e) {
							throw new RuntimeException("Thread " + Thread.currentThread().getName() + " failed to write element ", e);
						}
					}
					}

				}
			}
			System.out.println("Thread " + Thread.currentThread().getName() + " has finished");
		}
	}
}