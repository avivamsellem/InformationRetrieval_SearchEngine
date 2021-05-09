package ReadFile;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache class manage memory.
 */
public class Cache implements MemoryManager {

    private static AtomicInteger numberOfWordsEntered = new AtomicInteger(0);

    private ConcurrentHashMap<String, PostFile> memoryCache;

    /**
     * Constructor.
     *
     */
    public Cache()
    {
        this.memoryCache = new ConcurrentHashMap<>();
    }

    /**
     * add new PostFile to the CacheMemory
     * @param postFile
     */
    public void addFileToMemory(String first2Letters, PostFile postFile) {

        this.memoryCache.put(first2Letters, postFile);
    }


    /**
     * the method return the necessary PostFile from the Cache Memory. if not exist - return null.
     * @param  - first2Letters - String - the name of the posting file.
     * @return PostFile if the necessary exist. else - null.
     */
    public PostFile getFileFromMemory(String first2Letters) {

        this.numberOfWordsEntered.incrementAndGet();
        return this.memoryCache.get(first2Letters);
    }

    /**
     * Getter.
     *
     * @return Set<String>. all current files name in the memory.
     */
    public Set<String> getAllFilesNamesInMemory() {

        Set<String> allPostFilesNameInCache = this.memoryCache.keySet();

        return allPostFilesNameInCache;
    }

}
