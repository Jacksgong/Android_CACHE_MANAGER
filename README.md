Android_CACHE_MANAGER
=====================

### Android文件、缓存管理，这个library很早就想写了。
由于之前考虑到一些非final全局变量泛滥、而为得到有效的管理，而且如进入某个相对稳定的页面，该页面的数据来自网络，考虑到可以先将这部分的缓存存在缓存池中，或者缓存在文件管理器下，下次进入时，先从缓冲取数据，再到服务器上更新，或对比版本号，以此来做到一些有效的优化。那么目前来看这个library主要的工作可以缓存缓存数据（图片缓存、网络数据缓存等）。

### 目前缓存仅提供两类缓存池，一个池存储的是强引用缓存，一个池存储的是弱引用缓存。
其中强引用缓存提供设置最大缓存池大小方法，在每次插入时都会进行大小检查，每次都通过LRU原则，保证缓存池大小小于最大缓存大小。

### 两个缓存池支持并发操作，但这里的支持并发，对于本library而言，外部多线程使用时，注意同步。

这里有几个需要考究的地方，一个就是缓存池类型的选用，主要考究参考如下：
   
    
    ConcurrentHashMap 允许在不阻塞线程 （block threads）的情况下，几个线程同时修改Map。
    Collections.synchronizedMap(map) 创建一个阻塞Map (blocking map)， 这会降低map性能。如果需要确保数据的 一致性，使得每个线程都有Map 的即时视图，那么可以使用它
     
参考网友测试数据：[http://www.java-forums.org/new-java/13840-hashmap-vs-skiplistmap.html](http://www.java-forums.org/new-java/13840-hashmap-vs-skiplistmap.html)
    
    在4线程1.6万数据的条件下，ConcurrentHashMap 存取速度是ConcurrentSkipListMap 的4倍左右。
    但ConcurrentSkipListMap有几个ConcurrentHashMap 不能比拟的优点：
    1、ConcurrentSkipListMap 的key是有序的。
    2、ConcurrentSkipListMap 支持更高的并发。ConcurrentSkipListMap 的存取时间是log（N），和线程数几乎无关。也就是说在数据量一定的情况下，并发的线程越多，ConcurrentSkipListMap越能体现出他的优势。
     
    3、的使用Vector或Collections.synchronizedList(List<T>)的方式来解决该问题。但是这并没有效果!虽然在列表上add(),remove()和get()方法现在对线程是安全的，但遍历时仍然会抛出ConcurrentModificationException！在你遍历在列表时，你需要在该列表上使用同步，同时，在使用Quartz修改它时，也需要使用同步机制。
     
    重点提下，所有的线程安全都是对于内部而言

因此我们两个缓存池如下：

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
    
也许你会问为什么做了这些分析，强引用线程池还使用Collections.synchronizedMap，我想coderanch.com论坛的Steve已经给我做出了答复：

    You will have to come up with the requirements for your storage structure to figure out which one is best. There are two big differences: 

    1) The LinkedHashMap is ordered but not thread safe 

    2) The ConcurrentHashMap is thread safe but not ordered 

    If you need an ordered thread safe map, then maybe ConcurrentSkipListMap might be a better choice (but maybe not...). 

    If you wanted the ordering of LinkedHashMap in a thread safe structure, your concerns should be: 
    - How much work would it take to make LinkedHashMap thread safe? 
    - Do you trust yourself to be able to make it thread safe? 
    - Can you make it thread safe and still efficient? 

    versus 
    - How much work would it take to make ConcurrentSkipListMap (or ConcurrentHashMap) sort like the LinkedHashMap?At first blush, this might seem easy (CSKLM uses a comparator, so just make a comparator for access time) but it won't be (you would be sorting on something other than the Key (insertion/access order), your structure would have to change with access, not just insertion, iteration would be affected...). 
    - Is the Map you come up with efficient enough to use?

因此目前强引用线程池，这么做是折中的选择，当然还有很大的优化空间。我承认这里完全可以我们自己写一个线程安全的LinkedHashMap或支持LRU的ConcurrenthashMap，因为我们有他们的源码与详细的分析文稿。暂时如此，以后抽空优化。

当然，虽然测试结果ConcurrentSkipListMap在高并发下有优势，但是就4线程1.6万数据而言ConcurrentHashMap的优势，让我毫不犹豫的使用了ConcurrentHashMap。

### 对于文件缓存而言，我希望这里我们通过代理类一并说。看看代理类吧：

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

对于缓存而言。我们分别为弱引用缓存池与强引用缓存池分别提供了三种类型的输入、输出：
    byte[]、Bitmap、Parcelable

这里选用这三种类型，做了些考究，byte[]与Bitmap是考虑到使用频率而言，就Parcelable做了以下分析:

    *1. Object必须为可序列化对象才可存储为byte[]
     2.在使用内存的时候，Parcelable 类比Serializable性能高，所以推荐使用Parcelable类。
     3.Serializable在序列化的时候会产生大量的临时变量，从而引起频繁的GC。
     4.Parcelable不能使用在要将数据存储在磁盘上的情况，因为Parcelable不能很好的保证数据的持续性在外界有变化的情况下。尽管Serializable效率低点， 也不提倡用，但在这种情况下，还是建议用Serializable 。
归根结底是需要存储对象的需求的折中处理。

文件缓存除了一些常用的方法，有一个工具方法是提供按修改日期，保证某目录下需要保留的的文件个数。在文件缓存这块还有很多需要拓展与智能化。抽空进行优化，并希望得到各类建议。

### 最后，作为缓存入口，它的功能绝非止于此。
抽空我会进行数据分块处理等拓展（对外提供接口），并且优化弱引用池类型与文件缓存处理，并做一些考究。
