package uk.me.parabola.splitter;

import it.unimi.dsi.fastutil.longs.Long2IntFunction;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.BitSet;
import java.util.Collections;

public class SparseLong2ShortMultiMap {
	final Inner delegate;
	int size;
	
	public SparseLong2ShortMultiMap(short unassigned, int countDense) {
		delegate = new Inner(unassigned,0,countDense);
	}
	
	public IntList get(long key) {
		return delegate.get(key);
	}
	
	public void addTo(long key, BitSet out) {
		delegate.addTo(key,out);
	}
		
	public void put(long key, short val) {
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
		final Long2IntFunction map;
		Inner overflow;
		final short unassigned;
		final int depth;
		final int countDense;
		boolean isReadOnly = false;

		public Inner(short unassigned, int depth, int countDense) {
			System.err.println("Making SparseMultiMap");
			this.unassigned = unassigned;
			this.countDense = countDense;
			this.depth = depth;
			if (false && depth == 0)
				map = new DenseLong2ShortMap();
			else if (depth < countDense)
				map = new SparseLong2ShortMapInline();
			else
				map = new Long2IntOpenHashMap();
			map.defaultReturnValue(unassigned);
		}

		Inner getOverflow() {
			if (overflow != null)
				return overflow;
			overflow = new Inner(unassigned,depth+1,countDense);
			return overflow;
		}

		public void put(long key, short val) {
			if (isReadOnly) {
				throw new IllegalArgumentException("Map has been marked read only");
			}
			if (map.containsKey(key)) {
				getOverflow().put(key,val);
			} else {
				map.put(key,val);
			}
		}

		public IntList get(long key) {
			if (!map.containsKey(key))
				return IntLists.EMPTY_LIST;
			IntArrayList out = new IntArrayList(1);
			addTo(key,out);
			return out;
		}

		public void addTo(long key, BitSet out) {
			int val = map.get(key);
			if (val == unassigned)
				return;
			out.set(val);
			if (overflow != null)
				overflow.addTo(key,out);
		}

		public void addTo(long key, IntArrayList out) {
			if (!map.containsKey(key))
				return;
			out.add(map.get(key));
			if (overflow != null)
				overflow.addTo(key,out);
		}

		public void stats() {
			System.err.println("MAP occupancy: "+map.size());
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
