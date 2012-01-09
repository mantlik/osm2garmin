package uk.me.parabola.splitter;

import java.util.Arrays;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.ints.Int2ShortFunction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class DenseInt2ShortMap  implements Int2ShortFunction {
	static final int CHUNK_SIZE = 2048;
	static final int SIZE_INCR = 256*1024;
	/** What to return on unassigned indices */
	short unassigned = -1;
	
	ObjectArrayList<short[]> valschunks;
	int capacity;
	int size;
	int lastTrim = 0;

	
	DenseInt2ShortMap() {
		clear();
	}
	
	void resizeTo(int key) {
		if (key <= capacity)
			return;
		capacity = key + key/4 + SIZE_INCR;
		valschunks.size(1+capacity/CHUNK_SIZE);
	}

	public boolean containsKey(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (chunkid >= valschunks.size() || valschunks.get(chunkid) == null)
			return false;
		return valschunks.get(chunkid)[chunkoffset] != unassigned;
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
		short[] chunk = valschunks.get(chunkid);
		if (chunk == null) {
			chunk = new short[CHUNK_SIZE];
			Arrays.fill(chunk, unassigned);
			valschunks.set(chunkid, chunk);
		}
		short out = chunk[chunkoffset];
		if (out == unassigned) {
			size++;
		}
		chunk[chunkoffset] = val;
		return out;
	}

	public short get(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (key <= 0 || chunkid >= valschunks.size())
			return unassigned;
		short[] chunk = valschunks.get(chunkid);
		if (chunk == null) {
			return unassigned;
		} else {
			return chunk[chunkoffset];
		}
	}

	public short remove(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (key <= 0 || chunkid >= valschunks.size())
			return unassigned;
		short[]  chunk = valschunks.get(chunkid);
		if (chunk == null)
			return unassigned;
		short out = chunk[chunkoffset];
		if (out != unassigned) {
			size--;
			// In the array. Time to insert.
			chunk[chunkoffset]=unassigned;
		}
		return out;
	}

	@Override
	public void clear() {
		valschunks = new ObjectArrayList<short[]>();
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
