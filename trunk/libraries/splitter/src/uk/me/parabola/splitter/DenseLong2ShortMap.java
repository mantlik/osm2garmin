package uk.me.parabola.splitter;

import java.util.Arrays;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.longs.Long2IntFunction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class DenseLong2ShortMap  implements Long2IntFunction {
	static final int CHUNK_SIZE = 2048;
	static final int SIZE_INCR = 256*1024;
	/** What to return on unassigned indices */
	int unassigned = -1;
	
	ObjectArrayList<int[]> valschunks;
	long capacity;
	int size;
	int lastTrim = 0;

	
	DenseLong2ShortMap() {
		clear();
	}
	
	void resizeTo(long key) {
		if (key <= capacity)
			return;
		capacity = key + key/4 + SIZE_INCR;
		valschunks.size((int) (1+capacity/CHUNK_SIZE));
	}

	public boolean containsKey(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (chunkid >= valschunks.size() || valschunks.get(chunkid) == null)
			return false;
		return valschunks.get(chunkid)[chunkoffset] != unassigned;
	}
	
	public int put(long key, short val) {
		if (val == unassigned) {
			throw new IllegalArgumentException("Cannot store the value that is reserved as being unassigned. val="+val);
		}
		if (key < 0) {
			throw new IllegalArgumentException("Cannot store the negative key,"+key);
		}

		resizeTo(key);
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		int[] chunk = valschunks.get(chunkid);
		if (chunk == null) {
			chunk = new int[CHUNK_SIZE];
			Arrays.fill(chunk, unassigned);
			valschunks.set(chunkid, chunk);
		}
		int out = chunk[chunkoffset];
		if (out == unassigned) {
			size++;
		}
		chunk[chunkoffset] = val;
		return out;
	}

    @Override
	public int get(long key) {
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		if (key <= 0 || chunkid >= valschunks.size())
			return unassigned;
		int[] chunk = valschunks.get(chunkid);
		if (chunk == null) {
			return unassigned;
		} else {
			return chunk[chunkoffset];
		}
	}

    @Override
	public int remove(long key) {
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		if (key <= 0 || chunkid >= valschunks.size())
			return unassigned;
		int[]  chunk = valschunks.get(chunkid);
		if (chunk == null)
			return unassigned;
		int out = chunk[chunkoffset];
		if (out != unassigned) {
			size--;
			// In the array. Time to insert.
			chunk[chunkoffset]=unassigned;
		}
		return out;
	}

	@Override
	public void clear() {
		valschunks = new ObjectArrayList<int[]>();
		capacity = 0;
		size = 0;
	}

	@Override
	public boolean containsKey(Object arg0) {
		throw new UnsupportedOperationException("TODO: Implement");
	}

	@Override
	public Integer get(Object arg0) {
		throw new UnsupportedOperationException("TODO: Implement");
	}

	@Override
	public Integer put(Long arg0, Integer arg1) {
		return put(arg0.longValue(),arg1.intValue());
	}

	@Override
	public Integer remove(Object arg0) {
		throw new UnsupportedOperationException("TODO: Implement");
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int defaultReturnValue() {
		return unassigned;
	}

	@Override
	public void defaultReturnValue(int arg0) {
		unassigned = arg0;
	}

    @Override
    public int put(long l, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
