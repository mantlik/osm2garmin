package uk.me.parabola.splitter;

//import it.unimi.dsi.fastutil.longs.Long2ShortFunction;

/**
 * Stores long/short pairs. 
 * 
 */
interface SparseLong2ShortMapFunction {
	
	public short put(long key, short val);
	public void clear();
	public boolean containsKey(long key);
	public short get(long key);
	public void stats(int msgLevel);
	public int size();
	public short defaultReturnValue();
	public void defaultReturnValue(short arg0);
}
