package ReadFile;

/**
 * Pair Class represent record of term word and metadata
 */
public class Pair
{
    //initial variables
    private String termName;
    private String data;

    /**
     * Constructor.
     *
     * @param termName String. term.
     * @param data String. meta-data.
     */
    public Pair(String termName, String data)
    {
        this.termName = termName;
        this.data = data;
    }

    /**
     * Getter.
     *
     * @return String. term name.
     */
    public String getTermName() {
        return termName;
    }

    /**
     * Getter.
     *
     * @return String. term's Meta-data.
     */
    public String getData() {
        return data;
    }

}
