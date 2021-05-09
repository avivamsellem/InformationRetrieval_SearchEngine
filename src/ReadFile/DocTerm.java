package ReadFile;

import java.util.ArrayList;
import java.util.List;

/**
 * the class represent at term from a file and his data.
 */
public class DocTerm {

    private final String termName;
    private int count;
    private int delta;
    private boolean inHeader;
    private List<Integer> locationsInDocs;


    /**
     * Constructor.
     *
     * @param termName String. term name.
     * @param firstIndex Int. first index the word occur.
     * @param isHeader Boolean. indicate if the term have more weight.
     */
    public DocTerm(String termName, int firstIndex ,boolean isHeader) {
        this.delta = firstIndex;
        this.termName = termName;
        this.count = 1;
        this.locationsInDocs = new ArrayList<>();
        this.locationsInDocs.add(firstIndex);
        this.inHeader = this.inHeader || isHeader;
    }

    /**
     * Constructor.
     *
     * @param termName String. term name.
     */
    public DocTerm(String termName) {
        this.termName = termName;
    }


    /**
     * Add to current DocTerm indexs and update occurrences number.
     *
     * @param index Int. index.
     */
    public void add(int index)
    {
        this.locationsInDocs.add(index-delta);
        this.count++;
        this.delta = index;
    }

    /**
     * Getter.
     *
     * @return String. term name.
     */
    public String getTermName() {
        return this.termName;
    }

    /**
     * Getter.
     *
     * @return Int. number of occurrence of the tern at current document.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Getter.
     *
     * @return Boolean. true if the term is in document header ir have higher weight.
     */
    public boolean isInHeader() {
        return this.inHeader;
    }

    /**
     * Update header.
     *
     * @param flag Boolean. indicate if header should be updated.
     */
    public void updateHeader(boolean flag) {
        this.inHeader =  this.inHeader || flag;
    }

    /**
     * Getter.
     *
     * @return List<Integer>. all location of the current term.
     */
    public List<Integer> getLocationsInDocs() {
        return this.locationsInDocs;
    }

    /**
     * the method checks if 2 DocTerms are equals by there names.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {

        if(o instanceof DocTerm) {
            return this.termName.equals(((DocTerm) o).termName);
        }
        return false;
    }

    /**
     * the method return the DocTerm data.
     * @return String - data.
     */
    @Override
    public String toString()
    {
        return this.count + "," + (this.inHeader ? 1 : 0);
    }
}
