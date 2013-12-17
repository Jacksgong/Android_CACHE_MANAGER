package cn.dreamtobe.library.cache;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import cn.dreamtobe.library.cache.util.Util;

/**
 * @describe 自动内存缓存(自动管理缓存大小)
 * @Tips 全局变量有效管理
 * 
 * @author Jacksgong
 * @since 2013-7-11 上午8:51:58
 * @Web http://blog.dreamtobe.cn/1470.html
 */
public class MemoryCache extends CacheBase {

	/**
	 * 小于0抛错
	 */
	private final int INIT_CAPACITY = 10;

	private final float LOAD_FACTOR = 0.75f;

	/**
	 * 对于LinkedHashMap而言，它继承于HashMap、底层使用哈希表与双向链表(重新定义保存元素的Entry实现双向)来保存所有元素
	 * 参数说明
	 * int 初始链表容量
	 * float 负载因子 当前数据容量/总容量 （作用：超过此值自动扩张原容量的一倍）（注意：最大值为0.75，超过此值，底层会修改为此值）
	 * boolean true 从后往前 使用频率逐渐减少(LRU) ; false 存放循环队列按照插入顺序
	 */
	private Map<String, byte[]> mCache = Collections.synchronizedMap(new LinkedHashMap<String, byte[]>(INIT_CAPACITY, LOAD_FACTOR, true));

	private Map<String, WeakReference<byte[]>> mWeakCache = new ConcurrentHashMap<String, WeakReference<byte[]>>();
	/**
	 * ConcurrentHashMap 允许在不阻塞线程 （block threads）的情况下，几个线程同时修改Map。
	 * Collections.synchronizedMap(map) 创建一个阻塞Map (blocking map)， 这会降低map性能。如果需要确保数据的 一致性，使得每个线程都有Map 的即时视图，那么可以使用它
	 * 
	 * <url>http://www.java-forums.org/new-java/13840-hashmap-vs-skiplistmap.html</url>
	 * 在4线程1.6万数据的条件下，ConcurrentHashMap 存取速度是ConcurrentSkipListMap 的4倍左右。
	 * 但ConcurrentSkipListMap有几个ConcurrentHashMap 不能比拟的优点：
	 * 1、ConcurrentSkipListMap 的key是有序的。
	 * 2、ConcurrentSkipListMap 支持更高的并发。ConcurrentSkipListMap 的存取时间是log（N），和线程数几乎无关。也就是说在数据量一定的情况下，并发的线程越多，ConcurrentSkipListMap越能体现出他的优势。
	 * 
	 * 使用Vector或Collections.synchronizedList(List<T>)的方式来解决该问题。但是这并没有效果!虽然在列表上add(),remove()和get()方法现在对线程是安全的，但遍历时仍然会抛出ConcurrentModificationException！在你遍历在列表时，你需要在该列表上使用同步，同时，在使用Quartz修改它时，也需要使用同步机制。
	 * 
	 * *重点提下，所有的线程安全都是对于内部而言
	 */

	private long mSize = 0; // 当前缓存分配的大小

	private long mLimit = 1000000; // 允许字节数

	private MemoryCache() {
		// 默认可用大小
		setLimit(Runtime.getRuntime().maxMemory() / 10);
	}

	private final static class HolderClass {
		private final static MemoryCache INSTANCE = new MemoryCache();
	}

	public static MemoryCache getInstance() {
		return HolderClass.INSTANCE;
	}

	// ----------------- get
	public byte[] getBytes(String key) {
		try {
			if (Util.isNullOrNil(key)) {
				return null;
			}

			return mCache.get(key);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public byte[] getWeakBytes(String key) {
		try {
			if (Util.isNullOrNil(key)) {
				return null;
			}

			if (mWeakCache.get(key) != null) {
				return mWeakCache.get(key).get();
			} else {
				mWeakCache.remove(key);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	// ---------- removeCache
	public byte[] removeCache(String key) {
		if (Util.isNullOrNil(key)) {
			return null;
		}

		if (mCache.containsKey(key)) {
			byte[] b = mCache.get(key);
			mSize -= b.length;
			mCache.remove(key);
			return b;
		}
		return null;
	}

	/**
	 * @deprecated 弱引用虚拟机内部控制。不建议外界还去控制
	 * @param key
	 * @return
	 */
	public byte[] removeWeakCache(String key) {
		if (Util.isNullOrNil(key)) {
			return null;
		}

		if (mWeakCache.containsKey(key)) {
			byte[] b = mWeakCache.get(key) == null ? null : mWeakCache.get(key).get();
			mWeakCache.remove(key);
			return b;
		}
		return null;
	}

	// ---------- InCahce
	public String inCache(String key, byte[] bytes) {
		try {
			if (bytes == null) {
				return null;
			}
			if (Util.isNullOrNil(key)) {
				key = getSoleKey(bytes);
			}

			if (mCache.containsKey(key)) {
				mSize -= mCache.get(key).length;
			}
			// LinkedHashMap * 如果已有一个相同的key值则新的键值对覆盖旧的键值对
			mCache.put(key, bytes);
			mSize += bytes.length;
			checkSize();
			return key;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String inWeakCache(String key, byte[] bytes) {
		try {

			if (bytes == null) {
				return null;
			}

			if (Util.isNullOrNil(key)) {
				key = getSoleKey(bytes);
			}

			// *如果之前已有这key，将被替换
			mWeakCache.put(key, new WeakReference<byte[]>(bytes));
			return key;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// ---------------- Others
	public boolean containsKey(String key) {
		if (Util.isNullOrNil(key)) {
			// Debug.
			return false;
		}
		return mCache.containsKey(key);
	}

	/**
	 * 设置限制缓存大小
	 * 
	 * @param new_limit
	 */
	public void setLimit(long new_limit) {
		if (new_limit <= 0) {
			return;
		}
		this.mLimit = new_limit;
		commonLog("MemoryCache will use up to " + mLimit / 1024. / 1024. + "MB");
	}

	/**
	 * 控制堆内存 LRU机制
	 */
	private void checkSize() {
		commonLog("cache size=" + mSize + " length=" + mCache.size());
		if (mSize > mLimit) {
			// 先遍历最近最少使用的元素
			Iterator<Entry<String, byte[]>> iter = mCache.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, byte[]> entry = iter.next();
				mSize -= entry.getValue().length;
				iter.remove();
				if (mSize <= mLimit) {
					break;
				}

				commonLog("Clean cache. New size " + mCache.size());
			}
		}

	}

	public void clear() {
		mCache.clear();
	}

	private String getSoleKey(byte[] bytes) {
		return String.valueOf(bytes.hashCode()) + "_" + String.valueOf(System.currentTimeMillis());
	}

}
