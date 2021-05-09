package ReadFile;

import java.io.*;
import java.util.*;
import javafx.util.Pair;
import com.medallia.word2vec.Searcher.Match;
import java.util.concurrent.ConcurrentHashMap;
import org.tartarus.snowball.ext.PorterStemmer;
import com.medallia.word2vec.Searcher.UnknownWordException;

/**
 * the class responsible on analyzing a query and retrieve the 50 most relevant documents.
 */
public class Searcher extends ASearcher {
    static public int TOP_SIZE = 83;

    private boolean toStem;
    private IRanker ranker;
    private ConcurrentHashMap<String, TermDictionaryData> termsDictionary;
    private List<DocData> docNameDictionary;;
    private String postingFilePath;
    private PorterStemmer stemmer;


    /**
     * constructor.
     * @param toStem - boolean - weather to use stemming or not.
     * @param ranker - Iranker - a ranker for ranking the documents.
     * @param termsDictionary - ConcurrentHashMap - the Terms Dictionary.
     * @param docNameDictionary - List<DocData> - the Documents Dictionary.
     * @param postingFilePath - String - path to the Inverted Index Posting Files.
     */
    public Searcher(boolean toStem,
                    IRanker ranker,
                    ConcurrentHashMap termsDictionary,
                    List<DocData> docNameDictionary,
                    String postingFilePath)
    {

        this.toStem = toStem;
        this.ranker = ranker;
        this.termsDictionary = termsDictionary;
        this.docNameDictionary = docNameDictionary;
        this.postingFilePath = postingFilePath.concat("/");
        this.stemmer = new PorterStemmer();
    }

    /**
     * the method is responsible on analyzing the received query and retrieved the 50 most relevant documents.
     * @param query String - represent the inserted query.
     * @return List - a list of the 50 most relevant documents.
     */
    @Override
    public List<Pair> search(String query, boolean semantic)
    {
        HashMap<Object, Object> termData = new HashMap(); //<String, TermDictionaryData>
        String [] splittedQuery = parseQuery(query);

        splittedQuery = removeStopWordsFromQuery(splittedQuery);

        if(semantic) {
            splittedQuery = semantic(splittedQuery);
        }

        if(this.toStem)
        {
            splittedQuery = stem(splittedQuery);
        }


        for(String currentWord: splittedQuery)
        {
            /*checks if the word appears in the dictionary in LowerCase */
            if(this.termsDictionary.containsKey(currentWord.toLowerCase()))
            {
                termData.put(currentWord.toLowerCase(), this.termsDictionary.get(currentWord.toLowerCase()));
            }
            else if(this.termsDictionary.containsKey(currentWord.toUpperCase())) /*checks if the word appears in UpperCase*/
            {
                termData.put(currentWord.toUpperCase(), this.termsDictionary.get(currentWord.toUpperCase()));
            }
        }

        List<Object> relevantDocument = new ArrayList<>(); //List<DataPostingFile>
        Iterator iter = termData.entrySet().iterator();

        while(iter.hasNext())
        {
            Map.Entry currentEntry = (Map.Entry)iter.next();
            TermDictionaryData currentTDD = (TermDictionaryData) currentEntry.getValue();
            String currentWord = (String) currentEntry.getKey();



            String postFileName =  getPostingFileName(currentWord);
            String path = this.postingFilePath.concat("");
            if(this.toStem)
            {
                path = this.postingFilePath.concat("stem/PostingFiles/" + postFileName);
            }
            else
            {
                path = this.postingFilePath.concat("no stem/PostingFiles/" + postFileName);
            }
            int df = currentTDD.getDocFrequency();
            File file = new File(path);

            if(!file.exists())
            {
                System.out.println("couldn't find the relevant postingfile " + file.getAbsolutePath());
            }
            try
            {
                BufferedReader br = new BufferedReader(new FileReader(file));

                for (int i = 0; i < currentTDD.getPtr(); i++) {
                    br.readLine();
                }

                for (int i = 0; i < df; i++)
                {
                    DataPostingFile currentDataPostingFile = new DataPostingFile(br.readLine());
                    currentDataPostingFile.setTermName(currentWord);

                    relevantDocument.add(currentDataPostingFile);
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<Integer, Double> results = this.ranker.rank(this.docNameDictionary, relevantDocument, termData);
        results = sortByValue(results);


        List<Pair> resultSort = new ArrayList<>();

        iter = results.entrySet().iterator();
        int resultSize = Math.min(TOP_SIZE, results.size());
        String docName = "";
        double rank = 0;
        Map.Entry runner = null;
        DocData docData = null;

        for (int i = 0; i < resultSize; i++)
        {
            runner = (Map.Entry)iter.next();

            int docIndex = (Integer)runner.getKey();

            docData = this.docNameDictionary.get(docIndex);

            rank = (Double)runner.getValue();

            resultSort.add(new Pair(docData, rank));
        }

        return resultSort;
    }

    private String[] parseQuery(String query) {

        if(query != null) {

            String[] splitQuery = query.split(" ");

            List<String> termQueryList = new ArrayList<>();

            for(String str : splitQuery) {
                termQueryList.add(str);

                if(str.contains("-")) {
                    String[] hyphenTerms = str.split("-");

                    for(String subTerm : hyphenTerms) {
                        termQueryList.add(subTerm);
                    }
                }
            }

            return termQueryList.toArray(new String[0]);
        }

        return new String[0];
    }

    /**
     * the method apply semantic treatment on the query and add additional words
     * to the query.
     * @param splitQuery
     * @return
     */
    private String[] semantic(String[] splitQuery) {

        List<String> semanticQuery = new ArrayList<>();

        for (int i = 0; i < splitQuery.length; i++) {

            //try to add semantic matches
            try {
                List<Match> semanticMatches = ASearcher.semanticSearcher.getMatches(splitQuery[i], ASearcher.semanticRangeSize);

                for(Match match : semanticMatches) {
                    semanticQuery.add(match.match());
                }

            } catch (UnknownWordException e) {
                //Couldn't find all the semantic words
                continue;
            }
        }

        return semanticQuery.toArray(new String[0]);
    }


    /**
     * the method remove stop words from the split query.
     * @param splitQuery
     * @return
     */
    private String[] removeStopWordsFromQuery(String[] splitQuery)
    {
        List<String> splitQueryAfterParse = new ArrayList<>();
        for(String queryWord: splitQuery)
        {
            if(!this.stopWords.contains(queryWord.toLowerCase()))
            {
                splitQueryAfterParse.add(queryWord);
            }
        }

        String[] newSplitQuery = new String[splitQueryAfterParse.size()];

        for (int i = 0; i < splitQueryAfterParse.size(); i++)
        {
         newSplitQuery[i]  = splitQueryAfterParse.get(i);
        }

        return newSplitQuery;

    }

    /**

     * the method gets a word and return the correct PostingFile *include .txt PostFix*.
     * @param currentWord - the word we look for in the PostingFile.
     * @return String - PostingFile Name.
     */
    private String getPostingFileName(String currentWord)
    {

        if(Indexer.checkIfTermUnique(currentWord)) //check if the word in unique
        {
            return "unique.txt";
        }
        else if(Indexer.representNumber(currentWord)) //check if the words is a number
        {
            return "numbers.txt";
        }
        else //the word is a regular term
        {
            return currentWord.substring(0,2).concat(".txt");
        }

    }

    /**
     * the method apply stemming on the query words.
     * @param splittedQuery - String Array - represent the inserted query.
     * @return String Array - the query after stemming.
     */
    private String[] stem(String[] splittedQuery)
    {
        /*use PorterStemmer*/
        for (int i = 0; i < splittedQuery.length ; i++)
        {
            this.stemmer.setCurrent(splittedQuery[i]);
            this.stemmer.stem();
            splittedQuery[i] = this.stemmer.getCurrent();
        }

        return splittedQuery;
    }

//    public void updateStem(boolean toStem)
//    {
//        this.toStem = toStem;
//    }
//
//    @Override
//    public void updatePath(String newPath) {
//        this.postingFilePath = newPath.concat("/");
//    }


    /**
     * the method taken from : https://mkyong.com/java/how-to-sort-a-map-in-java/
     * the method sort Map by the value of the map.
     * @param unsortMap
     * @return sorted map.
     */
    private Map<Integer, Double> sortByValue(Map<Integer, Double> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<Integer, Double>> list =
                new LinkedList<Map.Entry<Integer, Double>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
