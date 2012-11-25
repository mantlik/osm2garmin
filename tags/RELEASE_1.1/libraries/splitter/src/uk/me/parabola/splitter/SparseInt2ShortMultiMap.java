package uk.me.parabola.splitter;

import it.unimi.dsi.fastutil.ints.Int2ShortFunction;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortLists;

import java.util.BitSet;
import java.util.Collections;

public class SparseInt2ShortMultiMap {
	final Inner delegate;
	int size;
	
	public SparseInt2ShortMultiMap(short unassigned, int countDense) {
		delegate = new Inner(unassigned,0,countDense);
	}
	
	public ShortList get(int key) {
		return delegate.get(key);
	}
	
	public void addTo(int key, BitSet out) {
		delegate.addTo(key,out);
	}
		
	public void put(int key, short val) {
		size++;
		delegate.put(key,val);
		if (size %1000000 == 0) {
			delegate.stats();
		}
	}
	
	public int size() {
		return size;
	}
	
	public void stats() {
		delegate.stats();
	}

	class Inner {
		final Int2ShortFunction map;
		Inner overflow;
		final short unassigned;
		final int depth;
		final int countDense;
		boolean isReadOnly = false;

		public Inner(short unassigned, int depth, int countDense) {
			System.out.println("Making SparseMultiMap");
			this.unassigned = unassigned;
			this.countDense = countDense;
			this.depth = depth;
			if (false && depth == 0)
				map = new DenseInt2ShortMap();
			else if (depth < countDense)
				map = new SparseInt2ShortMapInline();
			else
				map = new Int2ShortOpenHashMap();
			map.defaultReturnValue(unassigned);
		}

		Inner getOverflow() {
			if (overflow != null)
				return overflow;
			overflow = new Inner(unassigned,depth+1,countDense);
			return overflow;
		}

		public void put(int key, short val) {
			if (isReadOnly) {
				throw new IllegalArgumentException("Map has been marked read only");
			}
			if (map.containsKey(key)) {
				getOverflow().put(key,val);
			} else {
				map.put(key,val);
			}
		}

		public ShortList get(int key) {
			if (!map.containsKey(key))
				return ShortLists.EMPTY_LIST;
			ShortArrayList out = new ShortArrayList(1);
			addTo(key,out);
			return out;
		}

		public void addTo(int key, BitSet out) {
			short val = map.get(key);
			if (val == unassigned)
				return;
			out.set(val);
			if (overflow != null)
				overflow.addTo(key,out);
		}

		public void addTo(int key, ShortArrayList out) {
			if (!map.containsKey(key))
				return;
			out.add(map.get(key));
			if (overflow != null)
				overflow.addTo(key,out);
		}

		public void stats() {
			System.out.println("MAP occupancy: "+map.size());
			if (overflow != null)
				overflow.stats();
		}

		public void makeReadOnly() {
			isReadOnly = true;
			if (overflow != null)
				overflow.makeReadOnly();
		}
	}
}
