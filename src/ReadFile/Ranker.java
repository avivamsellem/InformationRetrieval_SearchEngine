package ReadFile;

import java.util.*;


public class Ranker implements IRanker
{
    //Static fields
    /*best so far:*/
//    static final double headerWeight = 1.45;
//    static final double k = 1.6;
//    static final double b = 0.18;

    /*tests*/
    static final double headerWeight = 1.45;
    static final double k = 1.6;
    static final double b = 0.1755;

    /**
     * Ranking function by BM25
     *
     * @param TermDocConnection DataPostingFile as Object represent term-doc connection.
     * @param termData String represent the term and the TermDictionaryData as value.
     * @return
     */
    public Map<Integer, Double> rank(List<DocData> docNameDictionary, List<Object> TermDocConnection, HashMap<Object, Object> termData) {
        Map<Integer, Double> results = new HashMap<>();

        HashSet<Integer> uniquesDocTermConnection = getUniqueDocs(TermDocConnection);//getting all the docs (without duplicates) that contains words from the query.
        double averageDocLength = computeAverageDocLength(docNameDictionary, uniquesDocTermConnection); //getting the average DocLength of the unique docs.

        for (Object obj : TermDocConnection) {
            DataPostingFile dpf = (DataPostingFile) obj;

            int docIndex = dpf.getDocIndex();

            if (results.containsKey(docIndex)) {
                double score = results.get(docIndex);

                score += BM25(docNameDictionary, dpf, termData, averageDocLength);

                results.put(docIndex, score);
            } else {
                double score = BM25(docNameDictionary, dpf, termData, averageDocLength);

                results.put(docIndex, score);
            }
        }

        return results;
    }


    /**
     * the method compute the average documents length that ranker retrieved.
     * @param docNameDictionary - the Documents Dictionary.
     * @param uniqueDocs - a list of all the unique documents (without duplicates) the ranker retrieved.
     * @return - double - the average of the documents length.
     */
    private double computeAverageDocLength(List<DocData> docNameDictionary, HashSet<Integer> uniqueDocs) {
        int total = 0;
        int docNumber = uniqueDocs.size();

        Iterator iter = uniqueDocs.iterator();

        while (iter.hasNext()) {
            int docNameInt = (Integer) iter.next();

            DocData docData = docNameDictionary.get(docNameInt);
            total += docData.getTotalUniqueWords();
        }

        if (docNumber == 0) {
            return 0;
        }

        return total / docNumber;
    }

    /**
     * getter method - the method return all the unique (without duplicates) documents the ranker retrieved.
     * @param relevantDocument - all the relevant documents the ranker retrieved.
     * @return list of all the unique documents without duplicates.
     */
    private HashSet<Integer> getUniqueDocs(List<Object> relevantDocument) {

        HashSet<Integer> uniqueDocs = new HashSet<>();

        for (Object obj : relevantDocument) {
            DataPostingFile dataPostingFile = (DataPostingFile) obj;
            int docIndex = dataPostingFile.getDocIndex();

            if (!uniqueDocs.contains(docIndex)) {
                uniqueDocs.add(docIndex);
            }
        }

        return uniqueDocs;
    }

    /**
     * the method computes and rank a document that retrieved using the BM25 formula.
     * @param docNameDictionary- the Documents Dictionary.
     * @param dataPostingFile - the data that connect between the term from the posting file to the the term document.
     * @param termData HashMap<Object, Object>.
     * @param avgdl - the average lengths of all the retrieved documents.
     * @return
     */
    private double BM25(List<DocData> docNameDictionary, DataPostingFile dataPostingFile, HashMap<Object, Object> termData, double avgdl)
    {

        String term = dataPostingFile.getTermName();
        TermDictionaryData termDictionaryData = (TermDictionaryData) termData.get(term);
        double idf = termDictionaryData.getIdf();
        double tf = dataPostingFile.getTf();
        DocData doc = docNameDictionary.get(dataPostingFile.getDocIndex());
        int D = doc.getTotalUniqueWords();

        tf = (dataPostingFile.getInHeader() == 1) ? tf * Ranker.headerWeight : tf;

        double mone = tf * (k + 1);
        double mechane = tf + k * (1 - b + (b * (D / avgdl)));

        return (idf * (mone / mechane));
    }

}
