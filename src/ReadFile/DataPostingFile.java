package ReadFile;

/**
 * the class represent the connection between the Document data to the Term data from the dictionary.
 */
public class DataPostingFile
{
    private int tf; //tf - represent the number of times the word appear in the document.
    private int inHeader;
    private int docIndex;
    private int[] wordLocationByDelta;
    private String termName;

    /**
     * Constructor. get a PostFile toString from disc and build a DataPostingFile object.
     *
     * @param PostingFileData
     */
    /* capital:3,0, 4 */
    public DataPostingFile(String PostingFileData)
    {
        String[] splitStrem = PostingFileData.split(",");
        this.tf = Integer.parseInt(splitStrem[0]);
        this.inHeader = (Integer.parseInt(splitStrem[1]) == 1 ) ? 1 : 0;
        this.docIndex = Integer.parseInt(splitStrem[2]);

        if(splitStrem.length == 4) {
            String[] splitLocations = splitStrem[3].split(",");
            this.wordLocationByDelta = new int[splitLocations.length];

            for (int i = 0; i < splitLocations.length; i++)
            {
                this.wordLocationByDelta[i] = Integer.parseInt(splitLocations[i]);
            }
        }
    }

    /**
     * setter method.
     * @param termName - String.
     */
    public void setTermName(String termName)
    {
        this.termName = termName;
    }

    /**
     * setter method.
     * @return tf - int.
     */
    public int getTf() {
        return tf;
    }

    /**
     * getter method.
     * @return inHeader - int.
     */
    public int getInHeader() {
        return inHeader;
    }

    /**
     * getter method.
     * @return docIndex - int.
     */
    public int getDocIndex() {
        return docIndex;
    }

    /**
     * getter method
     * @return int Array - all the locations of the term in the current Document.
     */
    public int[] getWordLocationByDelta() {
        return wordLocationByDelta;
    }

    /**
     * getter method.
     * @return String - the term Name.
     */
    public String getTermName() {
        return termName;
    }
}
