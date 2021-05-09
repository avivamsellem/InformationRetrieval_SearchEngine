package ReadFile;

/**
 * Object representing document with document fields(HEADER, TEXT, DATA, AUTHOR)
 */
public class Document
{
    private String docName;
    private String title;
    private String date;
    private String text;
    private String docID;

    public Document() {
        this.docName = null;
        this.title = null;
        this.date = null;
        this.text = null;
    }

    public Document(Document doc) {
        this.docName = doc.docName;
        this.title = doc.title;
        this.date = doc.date;
        this.text = doc.text;
    }

    /**
     * Set tag and tag-data.
     * @param TAG String. name of the tag.
     * @param data String. data to enter under the given tag.
     */
    public void setTag(String TAG, String data) {
        String category = TAG.substring(1,TAG.length()-1);

        if(category.equals("DOCNO"))
        {
            this.docName = data;
            ReadFile.logger.info("Document - File: " + data + " started");
        }
        else if(category.equals("DOCID")) {
            this.docID = data;
        }
        else if(category.equals("DATE1") || category.equals("DATE"))
        {
            this.date = data;
        }
        else if(category.equals("TI"))
        {
            this.title = data;
        }
        else if(category.equals("TEXT"))
        {
            this.text = data;
        }
    }

    /**
     * Getter.
     *
     * @return String. return document name.
     */
    public String getDocName() {
        return this.docName;
    }

    /**
     * Getter.
     *
     * @return String. return document date.
     */
    public String getDate() {
        return this.date;
    }

    /**
     * Getter.
     *
     * @return String. return document title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter.
     *
     * @return String. return document text.
     */
    public String getText() {
        return text;
    }

    /**
     * the method return all Document data.
     * @return String - data.
     */
    @Override
    public String toString()
    {
        String docString = "DOCNO: " + this.docName;
        docString += "DATE: " + this.date;
        docString += "TITLE: " + this.title;
        docString += "----------------------------------------------------------------------------------------------------------------------";
        docString += this.text;
        docString += "----------------------------------------------------------------------------------------------------------------------";
        docString += "";

        return docString;
    }

}
