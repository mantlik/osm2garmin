package uk.me.parabola.splitter;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.ints.Int2ShortFunction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class SparseInt2ShortMap  implements Int2ShortFunction {
	static final int CHUNK_SIZE = 64; // MUST be <= 64.
	static final int SIZE_INCR = 65536;
	/** What to return on unassigned indices */
	short unassigned = -1;
	
	ObjectArrayList<ShortArrayList> valschunks;
	LongArrayList bitmasks;
	int capacity;
	int size;
	int lastTrim = 0;

	
	SparseInt2ShortMap() {
		clear();
	}
	
	void resizeTo(int key) {
		//for (int i = lastTrim ; i < capacity ; i += CHUNK_SIZE) {
		//	ShortArrayList tmp = valschunks.get(i/CHUNK_SIZE);
		//	if (tmp != null)
		//		valschunks.get(i/CHUNK_SIZE).trim();
		//}
		lastTrim = capacity;
		
		if (key <= capacity)
			return;
		capacity = key + key/8 + SIZE_INCR;
		bitmasks.size(1+capacity/CHUNK_SIZE);
		valschunks.size(1+capacity/CHUNK_SIZE);
	}

	/** Count how many of the lowest X bits in mask are set 
	 * @return */
	int countUnder(long mask, int lowest) {
		return Fast.count(mask & ((1L << lowest) -1));
	}
	
	public boolean containsKey(int key) {
		if (key < 0) 
			return false;
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (chunkid >= valschunks.size())
			return false;
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		return (chunkmask & elementmask) != 0;
	}
	
	public short put(int key, short val) {
		if (val == unassigned) {
			throw new IllegalArgumentException("Cannot store the value that is reserved as being unassigned. val="+val);
		}
		if (key < 0) {
			throw new IllegalArgumentException("Cannot store the negative key,"+key);
		}

		resizeTo(key);
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (valschunks.get(chunkid) == null)
			valschunks.set(chunkid, new ShortArrayList(1));
		ShortArrayList chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) != 0) {
			// Already in the array, find the offset and store.
			short out = chunk.get(countUnder(chunkmask,chunkoffset));
			chunk.set(countUnder(chunkmask,chunkoffset), val);
			//System.out.println("Returning found key "+out+" from put "+ key + " " + val);
			return out;
		} else {
			size++;
			// Not in the array. Time to insert.
			int offset = countUnder(chunkmask,chunkoffset);
			chunk.add(offset,val);
			bitmasks.set(chunkid, elementmask | chunkmask);
			//System.out.println("Returning unassigned from put "+ key + " " + val);
			return unassigned;
		}
	}

	public short get(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (key <= 0 || chunkid >= valschunks.size())
			return unassigned;
		ShortArrayList chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) == 0) {
			return unassigned;
		} else {
			return chunk.get(countUnder(chunkmask,chunkoffset));
		}
	}

	public short remove(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (chunkid >= valschunks.size())
			return unassigned;
		ShortArrayList chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) == 0) {
			// Not in the array.
			// Do nothing;
			return unassigned;
		} else {
			size--;
			// In the array. Time to insert.
			int offset = countUnder(chunkmask,chunkoffset);
			short out = chunk.get(offset);
			chunk.rem(offset);
			bitmasks.set(chunkid, (~elementmask) & chunkmask);
			return out;
		}		
	}

	@Override
	public void clear() {
		valschunks = new ObjectArrayList<ShortArrayList>();
		bitmasks = new LongArrayList();	
		capacity = 0;
		size = 0;
	}

	@Override
	public boolean containsKey(Object arg0) {
		throw new UnsupportedOperationException("TODO: Implement");
	}

	@Override
	public Short get(Object arg0) {
		throw new UnsupportedOperationException("TODO: Implement");
	}

	@Override
	public Short put(Integer arg0, Short arg1) {
		return put(arg0.intValue(),arg1.shortValue());
	}

	@Override
	public Short remove(Object arg0) {
		throw new UnsupportedOperationException("TODO: Implement");
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public short defaultReturnValue() {
		return unassigned;
	}

	@Override
	public void defaultReturnValue(short arg0) {
		unassigned = arg0;
	}
}
