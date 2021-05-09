package ReadFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class for the Indexer Interface.
 */
public abstract class AIndexer implements  IndexerInterface{

    static List<Pair> reduceTermDictionary(ConcurrentHashMap<String, TermDictionaryData> termsDictionary) {

        List<Pair> reducedDict = new ArrayList<Pair>();

        for(String termName : termsDictionary.keySet()) {

            reducedDict.add(new Pair(termName, termsDictionary.get(termName).getTotalAppearancesInAllCorpus().toString()));
        }
        return reducedDict;
    }
}
