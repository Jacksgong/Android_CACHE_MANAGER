package cn.dreamtobe.library.cache.proxy;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import cn.dreamtobe.library.cache.FileCache;
import cn.dreamtobe.library.cache.MemoryCache;
import cn.dreamtobe.library.cache.util.TransUtil;

/**
 * 
 * @describe Proxy
 * 
 * @author Jacksgong
 * @since 2013-12-16 下午8:47:17
 * @Web http://blog.dreamtobe.cn
 */
public class CacheProxy {

	private final static class HoldClass {
		private final static MemoryCache MEMORY_CACHE = MemoryCache.getInstance();
		private final static FileCache FILE_CACHE = FileCache.getInstance();
	}

	// ---------------- get
	// *File
	public byte[] getBtsFromFile(String absolutePath) {
		return HoldClass.FILE_CACHE.getFile(absolutePath);
	}

	// *Strong
	public byte[] getBtsFromMemory(String key) {
		return HoldClass.MEMORY_CACHE.getBytes(key);
	}

	public Bitmap getBmpFromMemory(String key) {
		return TransUtil.bytes2Bimap(getBtsFromMemory(key));
	}

	public Parcel getPclFromMemory(String key) {
		return TransUtil.bytes2Parcelable(getBtsFromMemory(key));
	}

	// *Weak
	public byte[] getBtsFromWeakM(String key) {
		return HoldClass.MEMORY_CACHE.getWeakBytes(key);
	}

	public Bitmap getBmpFromWeakM(String key) {
		return TransUtil.bytes2Bimap(getBtsFromWeakM(key));
	}

	public Parcel getPclFromWeakM(String key) {
		return TransUtil.bytes2Parcelable(getBtsFromWeakM(key));
	}

	// --------------- InCache
	// byte[]
	public String putToMemory(String key, byte[] b) {
		return HoldClass.MEMORY_CACHE.inCache(key, b);
	}

	public String putToMemory(byte[] b) {
		return putToMemory(null, b);
	}

	// Bitmap
	public String putToMemory(String key, Bitmap b) {
		return HoldClass.MEMORY_CACHE.inCache(key, TransUtil.bitmap2Bytes(b));
	}

	public String putToMemory(Bitmap b) {
		return putToMemory(null, b);
	}

	// Parcelable
	/**
	 * 
	 * @param key
	 * @param p
	 *            这里选用Parcelable的原因
	 *            *1. Object必须为可序列化对象才可存储为byte[]
	 *            2.在使用内存的时候，Parcelable 类比Serializable性能高，所以推荐使用Parcelable类。
	 *            3.Serializable在序列化的时候会产生大量的临时变量，从而引起频繁的GC。
	 *            4.Parcelable不能使用在要将数据存储在磁盘上的情况，因为Parcelable不能很好的保证数据的持续性在外界有变化的情况下。尽管Serializable效率低点， 也不提倡用，但在这种情况下，还是建议你用Serializable 。
	 * @return
	 */
	public String putToMemory(String key, Parcelable p) {
		return HoldClass.MEMORY_CACHE.inCache(key, TransUtil.parcelable2Bytes(p));
	}

	public String putToMemory(Parcelable p) {
		return putToMemory(null, p);
	}

	// *Weak
	// bytes
	public String putToWeakM(String key, byte[] b) {
		return HoldClass.MEMORY_CACHE.inWeakCache(key, b);
	}

	public String putToWeakM(byte[] b) {
		return putToWeakM(null, b);
	}

	// Bitmap
	public String putToWeakM(String key, Bitmap b) {
		return HoldClass.MEMORY_CACHE.inWeakCache(key, TransUtil.bitmap2Bytes(b));
	}

	public String putToWeakM(Bitmap b) {
		return putToWeakM(null, b);
	}

	// Parcelable
	public String putToWeakM(String key, Parcelable p) {
		return HoldClass.MEMORY_CACHE.inWeakCache(key, TransUtil.parcelable2Bytes(p));
	}

	public String putToWeakM(Parcelable p) {
		return putToWeakM(null, p);
	}

	// --------- remove
	public boolean deleteFile(String absolutePath) {
		return HoldClass.FILE_CACHE.deleteFile(absolutePath);
	}

	public byte[] removeCache(String key) {
		return HoldClass.MEMORY_CACHE.removeCache(key);
	}

	// ---------- Memory Tool.
	public void setMemoryLimit(long limit) {
		if (limit <= 0) {
			// Warn
			return;
		}
		HoldClass.MEMORY_CACHE.setLimit(limit);
	}

	public boolean isMemoryContain(String key) {
		return HoldClass.MEMORY_CACHE.containsKey(key);
	}

	public void clearMemoryCache() {
		HoldClass.MEMORY_CACHE.clear();
	}

	// ------------ File Tool.

	public boolean isFileExist(String absolutePath) {
		return HoldClass.FILE_CACHE.isFileExist(absolutePath);
	}

	public boolean autoDeleteFile(String absolutePath, int KeepNum) {
		return HoldClass.FILE_CACHE.autoDeleteCache(absolutePath, KeepNum);
	}

}
