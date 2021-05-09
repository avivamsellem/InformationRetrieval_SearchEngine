package ReadFile;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface for the indexer.
 */
public interface IndexerInterface {

    void addDoc(HashMap<String, DocTerm> allWordsHashMap, String docName);

    void streamAllPostFilesInCacheMemory(MemoryManager oldMemory);

    PostFile getFilesToSort();

    void writeDictionariesToDisc();

    MemoryManager getDataStructure();

    void setNewDataStructure(MemoryManager newMM);

    int getNumberOfDocs();

    int getNumberOfTerms();

    ConcurrentHashMap<String, TermDictionaryData> getTermDictionary();

    List<DocData> getDocDictionary();
}
