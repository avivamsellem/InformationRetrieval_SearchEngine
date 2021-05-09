package ReadFile;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * TermDictionaryData class representing a term in the dictionary and its metadata.
 */
public class TermDictionaryData
{
    //initial static variables
    private static int NumberOfDocsInCorpus;

    //initial variables
    private AtomicInteger docFrequency;

    public AtomicInteger getTotalAppearancesInAllCorpus() {
        return totalAppearancesInAllCorpus;
    }

    private AtomicInteger totalAppearancesInAllCorpus;

    private double idf;
    private int ptr;

    /**
     * Constructor.
     *
     * @param numberOfAppearances Int. number of word appearances in the hole corpus.
     */
    public TermDictionaryData(int numberOfAppearances)
    {
        this.docFrequency = new AtomicInteger(1);
        this.totalAppearancesInAllCorpus = new AtomicInteger(numberOfAppearances);

        this.idf = 0;
        this.ptr = 0;
    }

    /**
     * Constructor. (Mostly use to rebuild the dictionary).
     *
     * @param str
     */
    public TermDictionaryData(String str)
    {
        String[] data = str.split(",");

        this.docFrequency = new AtomicInteger(Integer.parseInt(data[0]));

        this.totalAppearancesInAllCorpus = new AtomicInteger(Integer.parseInt(data[3]));

        this.idf = Double.parseDouble(data[2]);
        this.ptr = Integer.parseInt(data[1]);
    }

    /**
     * Add 1 to docFrequency.
     */
    public void setDocFrequency()
    {
        this.docFrequency.incrementAndGet();
    }

    /**
     * Adding the total appearance.
     *
     * @param addToTotalApearances
     */
    public void setTotalAppearancesInAllCorpus(int addToTotalApearances)
    {
        this.totalAppearancesInAllCorpus.addAndGet(addToTotalApearances);
    }

    /**
     * Setter.
     */
    public void setIdf()
    {
        this.idf = Math.log(NumberOfDocsInCorpus/this.docFrequency.get()) / Math.log(2);
    }

    /**
     * Setter.
     *
     * @param ptr
     */
    public void setPtr(int ptr)
    {
        this.ptr = ptr;
    }

    /**
     * Getter.
     *
     * @return
     */
    public int getDocFrequency()
    {
        return docFrequency.get();
    }

    /**
     * Getter.
     *
     * @return Int. IDF of the current word.
     */
    public double getIdf()
    {
        return this.idf;
    }

    /**
     * Getter.
     *
     * @return Int. Pointer represent the word location.
     */
    public int getPtr()
    {
        return this.ptr;
    }


    /**
     * the method return all the data of the Object as a String.
     * @return String - data.
     */
    @Override
    public String toString()
    {
        return this.docFrequency + "," + this.ptr + "," + this.idf + "," + this.totalAppearancesInAllCorpus.get();
    }

    /**
     * STATIC FUNCTION. set the whole number of the Dictionary Term IDF.
     */
    public static void setNumberOfDocsInCorpus(int N)
    {
        NumberOfDocsInCorpus = N;
    }

}
