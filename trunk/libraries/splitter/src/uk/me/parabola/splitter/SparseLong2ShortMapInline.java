package uk.me.parabola.splitter;

import java.util.Arrays;

import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.longs.Long2IntFunction;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SparseLong2ShortMapInline  implements Long2IntFunction {
	static final int CHUNK_SIZE = 64; // MUST be <= 64.
	static final long SIZE_INCR = 65536;
	/** What to return on unassigned indices */
	int unassigned = -1;
	
	ObjectArrayList<int []> valschunks;
	LongArrayList bitmasks;
	long capacity;
	int size;
	long lastTrim = 0;


	void arrayPush(int [] array, int index) {
		for (int j=array.length-1 ; j > index ; j--)
			array[j]=array[j-1];
	}
	void arrayUnpush(int [] array,int index) {
		for (int j=index ; j < array.length ; j++)
			array[j]=array[j+1];
		array[array.length-1] = unassigned;
	}

	void arrayCopyFill(int [] from, int [] to) {
		int j=0;
		for ( ; j < from.length ; j++)
			to[j]=from[j];
		for ( ; j < to.length ; j++)
			to[j] = unassigned;
	}
	
	int []chunkAdd(int[] array, int index, int val) {
		if (array[array.length-1] != unassigned) {
			int tmp[] = new int[array.length+4];
			arrayCopyFill(array,tmp);
			array = tmp;
		}
		arrayPush(array,index);
		array[index] = val;
		return array;
	}
	int []chunkMake() {
		int out[] = new int[4];
		Arrays.fill(out,(int)4);
		return out;
		}
	void chunkSet(int[] array, int index, int val) {
		array[index] = val;
	}
	int chunkGet(int[] array, int index) {
		return array[index];
	}
	void chunkRem(int[] array, int index) {
		arrayUnpush(array,index);
	}
	
	void chunkAdd(IntArrayList arraylist, int index, int val) {
		arraylist.add(index,val);
	}
	void chunkSet(IntArrayList arraylist, int index, int val) {
		arraylist.set(index,val);
	}
	int chunkGet(IntArrayList arraylist, int index) {
		return arraylist.get(index);
	}
	void chunkRem(IntArrayList arraylist, int index) {
		arraylist.rem(index);
	}
	
	SparseLong2ShortMapInline() {
		clear();
	}
	
	void resizeTo(long key) {
		//for (int i = lastTrim ; i < capacity ; i += CHUNK_SIZE) {
		//	ShortArrayList tmp = valschunks.get(i/CHUNK_SIZE);
		//	if (tmp != null)
		//		valschunks.get(i/CHUNK_SIZE).trim();
		//}
		lastTrim = capacity;
		
		if (key <= capacity)
			return;
		capacity = key + key/8 + SIZE_INCR;
		bitmasks.size((int) (1+capacity/CHUNK_SIZE));
		valschunks.size((int) (1+capacity/CHUNK_SIZE));
	}

	/** Count how many of the lowest X bits in mask are set 
	 * @return */
	int countUnder(long mask, int lowest) {
		return Fast.count(mask & ((1L << lowest) -1));
	}
	
    @Override
	public boolean containsKey(long key) {
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		if (chunkid >= valschunks.size())
			return false;
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		return (chunkmask & elementmask) != 0;
	}
	
    @Override
	public int put(long key, int val) {
		if (val == unassigned) {
			throw new IllegalArgumentException("Cannot store the value that is reserved as being unassigned. val="+val);
		}
		if (key < 0) {
			throw new IllegalArgumentException("Cannot store the negative key,"+key);
		}

		resizeTo(key);
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		if (valschunks.get(chunkid) == null)
			valschunks.set(chunkid, chunkMake());
		int[] chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) != 0) {
			// Already in the array, find the offset and store.
			int out = chunkGet(chunk,countUnder(chunkmask,chunkoffset));
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

    @Override
	public int get(long key) {
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		if (key <= 0 || chunkid >= valschunks.size())
			return unassigned;
		int[] chunk = valschunks.get(chunkid);
		long chunkmask = bitmasks.get(chunkid);
		long elementmask = 1L << chunkoffset;
		if ((chunkmask & elementmask) == 0) {
			return unassigned;
		} else {
			return chunkGet(chunk,countUnder(chunkmask,chunkoffset));
		}
	}

    @Override
	public int remove(long key) {
		int chunkid = (int) (key/CHUNK_SIZE);
		int chunkoffset = (int) (key%CHUNK_SIZE);
		if (chunkid >= valschunks.size())
			return unassigned;
		int[] chunk = valschunks.get(chunkid);
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
			int out = chunkGet(chunk,offset);
			chunkRem(chunk,offset);
			bitmasks.set(chunkid, (~elementmask) & chunkmask);
			return out;
		}		
	}

	@Override
	public void clear() {
		valschunks = new ObjectArrayList<int[]>();
		bitmasks = new LongArrayList();	
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
		return put(arg0.intValue(),arg1.shortValue());
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
}
