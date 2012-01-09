package uk.me.parabola.splitter;

import java.util.Arrays;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.ints.Int2ShortFunction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class SparseInt2ShortMapInline  implements Int2ShortFunction {
	static final int CHUNK_SIZE = 64; // MUST be <= 64.
	static final int SIZE_INCR = 65536;
	/** What to return on unassigned indices */
	short unassigned = -1;
	
	ObjectArrayList<short []> valschunks;
	LongArrayList bitmasks;
	int capacity;
	int size;
	int lastTrim = 0;


	void arrayPush(short [] array, int index) {
		for (int j=array.length-1 ; j > index ; j--)
			array[j]=array[j-1];
	}
	void arrayUnpush(short [] array,int index) {
		for (int j=index ; j < array.length ; j++)
			array[j]=array[j+1];
		array[array.length-1] = unassigned;
	}

	void arrayCopyFill(short [] from, short [] to) {
		int j=0;
		for ( ; j < from.length ; j++)
			to[j]=from[j];
		for ( ; j < to.length ; j++)
			to[j] = unassigned;
	}
	
	short []chunkAdd(short[] array, int index, short val) {
		if (array[array.length-1] != unassigned) {
			short tmp[] = new short[array.length+4];
			arrayCopyFill(array,tmp);
			array = tmp;
		}
		arrayPush(array,index);
		array[index] = val;
		return array;
	}
	short []chunkMake() {
		short out[] = new short[4];
		Arrays.fill(out,(short)4);
		return out;
		}
	void chunkSet(short[] array, int index, short val) {
		array[index] = val;
	}
	short chunkGet(short[] array, int index) {
		return array[index];
	}
	void chunkRem(short[] array, int index) {
		arrayUnpush(array,index);
	}
	
	void chunkAdd(ShortArrayList arraylist, int index, short val) {
		arraylist.add(index,val);
	}
	void chunkSet(ShortArrayList arraylist, int index, short val) {
		arraylist.set(index,val);
	}
	short chunkGet(ShortArrayList arraylist, int index) {
		return arraylist.get(index);
	}
	void chunkRem(ShortArrayList arraylist, int index) {
		arraylist.rem(index);
	}
	
	SparseInt2ShortMapInline() {
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
			valschunks.set(chunkid, chunkMake());
		short[] chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) != 0) {
			// Already in the array, find the offset and store.
			short out = chunkGet(chunk,countUnder(chunkmask,chunkoffset));
			chunkSet(chunk,countUnder(chunkmask,chunkoffset), val);
			//System.out.println("Returning found key "+out+" from put "+ key + " " + val);
			return out;
		} else {
			size++;
			// Not in the array. Time to insert.
			int offset = countUnder(chunkmask,chunkoffset);
			valschunks.set(chunkid,chunkAdd(chunk,offset,val));
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
		short[] chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) == 0) {
			return unassigned;
		} else {
			return chunkGet(chunk,countUnder(chunkmask,chunkoffset));
		}
	}

	public short remove(int key) {
		int chunkid = key/CHUNK_SIZE;
		int chunkoffset = key%CHUNK_SIZE;
		if (chunkid >= valschunks.size())
			return unassigned;
		short[] chunk = valschunks.get(chunkid);
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
			short out = chunkGet(chunk,offset);
			chunkRem(chunk,offset);
			bitmasks.set(chunkid, (~elementmask) & chunkmask);
			return out;
		}		
	}

	@Override
	public void clear() {
		valschunks = new ObjectArrayList<short[]>();
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
