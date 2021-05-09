package ReadFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PostFile Class represent the structure of virtual File in the computer.
 */
public class PostFile {

    private List<Pair> allWords;

    public PostFile() {
        this.allWords = new ArrayList<>();
    }

    /**
     * Adding a given term represent by string to the post file.
     *
     * @param termName String. Term.
     * @param data String. all the term metadata represented by string.
     *             DF IDF Numner of Document appear in etc.
     */
    public synchronized void addTermToPostingFile(String termName, String data) {
        Pair newPair = new Pair(termName, data);
        this.allWords.add(newPair);
    }

    /**
     *
     * Getter.
     *
     * @return List<Pair> of all postFile words
     */
    public List getAllWords() {
        return this.allWords;
    }


    /**
     * the method returns all the termName and the data of the term
     * as String chained together.
     * @return String - termName + data together.
     */
    @Override
    public String toString() {
        String content = "";

        for (Pair pair : allWords) {
            content = content.concat(pair.getTermName().concat(":".concat(pair.getData().concat("\n"))));
        }
        return content;
    }

}


/**
 * this class is used for comparator for sorting the Posting files.
 *
 */
class SortPair implements Comparator<Pair>
{
    // Used for sorting in ascending order of

    /**
     * the method is a comparator for sorting the PostingFile.
     * the sort is by the terms when they are in LowerCase.
     * @param firstPair
     * @param secondPair
     * @return
     */
    public int compare(Pair firstPair, Pair secondPair)
    {
        String firstName = firstPair.getTermName().toLowerCase();
        String secondName = secondPair.getTermName().toLowerCase();

        return firstName.compareTo(secondName);
    }
}

/**
 * this class is used for comparator for sorting the Posting files.
 *
 */
class SortPairByAppearance implements Comparator<Pair>
{
    // Used for sorting in ascending order of

    /**
     * the method is a comparator for sorting the pairs of data.
     * the sort is by the numbers.
     * @param firstPair
     * @param secondPair
     * @return
     */
    public int compare(Pair firstPair, Pair secondPair)
    {
        String totalAppearancesFirst = firstPair.getData();
        String totalAppearancesSecond = secondPair.getData();

        Float toalAppearances1;
        Float toalAppearances2;

        try {
            toalAppearances1 = Float.parseFloat(totalAppearancesFirst);
            toalAppearances2 = Float.parseFloat(totalAppearancesSecond);
        }
        catch (NumberFormatException e) {
            return 0;
        }
        catch (NullPointerException e) {
            return 0;
        }

        return toalAppearances2.compareTo(toalAppearances1);
    }
}