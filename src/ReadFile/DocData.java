package ReadFile;

import java.util.LinkedList;
import java.util.List;


/**
 * ermDictionaryData class representing a document in the Document Dictionary and its data.
 */
public class DocData
{
    private int docIndex;
    private String docName;
    private int numberOfMostFrequentWord;
    private int totalUniqueWords;
    private LinkedList<DocTerm> top5Identities = new LinkedList<>();

    /**
     * constructor.
     * @param docName - Document name as it appears in the corpus.
     * @param numberOfMostFrequentWord - the number of times that the most common word appears.
     * @param totalUniqueWords - the number of the unique words in the document.
     */
    public DocData(String docName, int numberOfMostFrequentWord, int totalUniqueWords)
    {
        this.docName = docName;
        this.numberOfMostFrequentWord = numberOfMostFrequentWord;
        this.totalUniqueWords = totalUniqueWords;
    }


    /**
     * constructor.
     * @param data String - all the data about the document as it saved in the disc.
     */
    public DocData(String data)
    {
        String [] splitData = data.split(",");

        this.docName = splitData[0];
        this.totalUniqueWords = Integer.parseInt(splitData[1]);
        this.numberOfMostFrequentWord = Integer.parseInt(splitData[2]);

        for (int i = 3; i < splitData.length; i++)
        {
            this.top5Identities.add(i-3, new DocTerm(splitData[i]));
        }
    }

    /**
     * setter method.
     * @param index - the number of index the document received.
     */
    public void setDocIndex(int index) {
        this.docIndex = index;
    }

    /**
     * getter method.
     * @return String - the document name as it appears on the corpus.
     */
    public String getDocName()
    {
        return this.docName;
    }

    /**
     * getter method.
     * @return List - the top 5 most common entities.
     */
    public List<DocTerm> getIdentities() {
        return this.top5Identities;
    }


//    public void addIdentity(DocTerm docTerm) {
//        this.top5Identities.add(docTerm);
//    }

    /**
     * setter method.
     * @param docTerm - a term represent an entity.
     */
    public void setTop5Identity(DocTerm docTerm)
    {
        int num = Math.min(5,this.top5Identities.size());
        boolean wasAdded = false;

        if(docTerm.isInHeader()) {
            for(int i = 0 ; i < num ; i++) {
                if(this.top5Identities.get(i).isInHeader()) {
                    if(docTerm.getCount() > this.top5Identities.get(i).getCount()) {
                        this.top5Identities.add(i, docTerm);
                        wasAdded = true;

                        break;
                    }
                }
                else {
                    this.top5Identities.add(i, docTerm);
                    wasAdded = true;

                    break;
                }
            }
        }
        else {
            for(int i = 0 ; i < num ; i++) {
                if(!this.top5Identities.get(i).isInHeader()) {
                    if(docTerm.getCount() > this.top5Identities.get(i).getCount()) {
                        this.top5Identities.add(i, docTerm);
                        wasAdded = true;

                        break;
                    }
                }
            }
        }

        if(!wasAdded && this.top5Identities.size() < 5) {
            this.top5Identities.add(docTerm);
        }

        if(this.top5Identities.size() > 5) {
            this.top5Identities.remove(5);
        }
    }


    /**
     * the method represent all the object data as a String.
     * @return String.
     */
    @Override
    public String toString()
    {
        String data =  this.docName + "," + this.totalUniqueWords + "," + this.numberOfMostFrequentWord;

        for(DocTerm currentIdentity : this.top5Identities)
        {
            data = data.concat( "," + currentIdentity.getTermName());
        }

         return data;
    }

    /**
     * getter method.
     * @return int - the number of appearances of the most frequent word int he document.
     */
    public int getNumberOfMostFrequentWord() {
        return numberOfMostFrequentWord;
    }

    /**
     * getter method.
     * @return int - the number of unique words in the document.
     */
    public int getTotalUniqueWords() {
        return totalUniqueWords;
    }

    /**
     * the method initialize a new entities list.
     */
    public void newIdentitiesList() {
        this.top5Identities = new LinkedList<>();
    }

    /**
     * the method return the data of the document without the entities.
     * @return String.
     */
    public String toPresentWithoutIdentities()
    {
        return this.docIndex + ". " + this.docName;
    }

    /**
     * the method return the data of the document with the entities.
     * @return
     */
    public String toPresentWithIdentities() {

        String toString = this.docIndex + ". " + this.docName + "   [";

        for (int i = 0; i < this.top5Identities.size(); i++) {

            if(i + 1 == this.top5Identities.size()) {
                toString += this.top5Identities.get(i).getTermName();
            }
            else {
                toString += this.top5Identities.get(i).getTermName().concat(", ");
            }
        }

        return toString.concat("]");
    }
}
