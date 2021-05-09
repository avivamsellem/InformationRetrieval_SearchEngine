package ReadFile;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * the class is in charge of building the Inverted Index
 */
public class Indexer extends AIndexer{

    private final int minimalApearance = 1; //minimal threshold to enter the PostFile


    public MemoryManager memoryManager;
    private ConcurrentHashMap<String, Object> mutexHashTable;

    private List<DocData> docNameDictionary; //dictionary containing all docs names.
    private ConcurrentHashMap<String, TermDictionaryData> termsDictionary; //dictionary containing all terms names with data.
    private Object termsDictionaryLockNumbers = new Object();
    private Object termsDictionaryLockTerms = new Object();
    private Object termDictionaryLockUniques = new Object();
    private Object addToPostingFileLock = new Object();
    private Object docNameDictionaryLock = new Object();


    private static AtomicInteger counterForDocs = new AtomicInteger(0);

    private ThreadPoolManager threadPool;
    private String destPath;


    public Indexer(MemoryManager MM, ThreadPoolManager threadPool, String destinationPath) {

        ReadFile.logger.info("entered Indexer constructor.");
        this.memoryManager = MM;
        this.mutexHashTable = new ConcurrentHashMap<>();
        this.destPath = destinationPath;

        File file = new File(this.destPath.concat("PostingFiles/"));

        file.mkdir();

        this.docNameDictionary = new ArrayList<>();
        this.termsDictionary = new ConcurrentHashMap<>();
        this.threadPool = threadPool;
    }


    /**
     * public method executed from the the Parser Class.
     * the method using multi-threading for adding every word in the doc.
     *
     * @param allWordsHashMap - List off all words in the current document.
     * @param docName         - the document name.
     */
    @Override
    public void addDoc(HashMap<String, DocTerm> allWordsHashMap, String docName) {

        int numberOfMostFrequentWord = 0;
        Iterator iter = allWordsHashMap.entrySet().iterator();

        Map.Entry pair = null;
        String term;

        while (iter.hasNext()) {
            pair = (Map.Entry) iter.next();

            int currentAppearances = ((DocTerm) pair.getValue()).getCount();
            if (numberOfMostFrequentWord < currentAppearances) {
                numberOfMostFrequentWord = currentAppearances;
            }

        }

        DocData docData;

        synchronized (docNameDictionaryLock)
        {
            docData = new DocData(docName, numberOfMostFrequentWord, allWordsHashMap.size());
            this.docNameDictionary.add(docData);
        }

        int docIndex = this.docNameDictionary.indexOf(docData);
        ReadFile.logger.info("Indexer;'addDoc' method : with the docName: " + docName + "(received DocIndex: " + docIndex + ")");

        for (DocTerm currentDocTerm : allWordsHashMap.values())
        {
            // check if the currentTerm is an identity
            if(currentDocTerm.getTermName().equals(currentDocTerm.getTermName().toUpperCase()) && !containNumber(currentDocTerm.getTermName()))
            {
                docData.setTop5Identity(currentDocTerm);
            }

            addDocTerm(currentDocTerm, docIndex);
        }
    }

    private boolean containNumber(String termName) {

        char[] breakWord = termName.toCharArray();

        for(char ch : breakWord) {
            if(Character.isDigit(ch)) {
                return true;
            }
        }

        return false;
    }


    /**
     * the method receive a word and a docIndex and add it to all necessary data structures.
     *
     * @param docTerm  - word name.
     * @param docIndex - the index the document the word came from received.
     */
    private void addDocTerm(DocTerm docTerm, int docIndex) {
        ReadFile.logger.info("Indexer;'addDocTerm' method - with the term: '" + docTerm.getTermName() + "' from docIndex: '" + docIndex + ".");

        if(docTerm.getTermName().length() >= 2 || isNumber(docTerm.getTermName())) {
            addToTermsDictionary(docTerm);
            addToPostingFile(docTerm, docIndex);
        }
    }

    /**
     * the methos checks if the term is a number.
     * @param termName String - the Term.
     * @return boolean - true - if the term is a number, else - false.
     */
    private boolean isNumber(String termName) {

        char[] breakWord = termName.toCharArray();

        try {
            for(char ch : breakWord) {
                Character.isDigit(ch);
            }
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * the method checks if the dictionary contains the term. it it does - update the data. else - add to dictionary.
     *
     * @param docTerm
     */
    private void addToTermsDictionary(DocTerm docTerm) {
        TermDictionaryData currentWordInDictionary = null;

        String termName = docTerm.getTermName();
        char firstChar = termName.charAt(0);
        int termCountInDoc = docTerm.getCount();

        boolean updateTerm = false;

        if (representNumber(termName)) //the term is a number
        {
            synchronized (this.termsDictionaryLockNumbers) {
                if (this.termsDictionary.containsKey(termName)) //if the number exist in the dictionary - update df and total appearances
                {
                    currentWordInDictionary = this.termsDictionary.get(termName);
                    updateTerm = true;
                } else {
                    this.termsDictionary.put(termName, new TermDictionaryData(termCountInDoc));
                }
            }
        }
        else if (checkIfTermUnique(termName)) //checks if the term is unique
        {
            synchronized (this.termDictionaryLockUniques) {
                if (this.termsDictionary.containsKey(termName.toLowerCase())) //check is exist in dictionary UpperCase
                {
                    currentWordInDictionary = this.termsDictionary.get(termName.toLowerCase());
                    updateTerm = true;
                } else if (this.termsDictionary.containsKey(termName.toUpperCase())) //check if exist in dictionary in LowerCase
                {
                    currentWordInDictionary = this.termsDictionary.get(termName.toUpperCase());
                    updateTerm = true;
                } else if (this.termsDictionary.containsKey(termName))  //check if exist with both Upper and Lower letters
                {
                    currentWordInDictionary = this.termsDictionary.get(termName);
                    updateTerm = true;
                }
                else {//the unique term dowsnt exist in the dictionary
                    this.termsDictionary.put(termName, new TermDictionaryData(termCountInDoc));
                    updateTerm = false;
                }
            }
        }
        else if (Character.isUpperCase(firstChar)) //checks if the word is in UpperCase
        {
            String termInLowerCase = termName.toLowerCase();

            /*checks if the word exist in the dictionary in LowerCase
             * if it does, update data. else - checks if exists in UpperCase*/
            synchronized (this.termsDictionaryLockTerms)
            {
                if (this.termsDictionary.containsKey(termInLowerCase))
                {
                    currentWordInDictionary = this.termsDictionary.get(termInLowerCase);
                    updateTerm = true;
                }
                else if (this.termsDictionary.containsKey(termName)) //the word exists in UpperCase
                {
                    currentWordInDictionary = this.termsDictionary.get(termName);
                    updateTerm = true;

                }
                else //the word not exists - we add the word in UpperCase
                {
                    this.termsDictionary.put(termName, new TermDictionaryData(termCountInDoc));
                }
            }
        }
        else //the word is in LowerCase
        {
            String termInUpperCase = termName.toUpperCase();

            /*checks if the word already exist in the dictionary in UpperCase.
             * if it does - we will change the word to appear in the dictionary in LowerCase and update the data as usual*/
            synchronized (this.termsDictionaryLockTerms)
            {
                if (this.termsDictionary.containsKey(termInUpperCase))
                {
                    currentWordInDictionary = this.termsDictionary.get(termInUpperCase);
                    this.termsDictionary.remove(termInUpperCase);//remove the word in his UpperCase form
                    this.termsDictionary.put(termName, currentWordInDictionary);// add the word in LowerCase form

                    updateTerm = true;
                }
                else if (this.termsDictionary.containsKey(termName)) //checks of the word exist in LowerCase form
                {
                    currentWordInDictionary = this.termsDictionary.get(termName);
                    updateTerm = true;
                }
                else //the word doesn't exist in the dictionay - add the word in LowerCase form
                {
                    this.termsDictionary.put(termName, new TermDictionaryData(termCountInDoc));
                }
            }
        }

        if (updateTerm) {

            currentWordInDictionary.setDocFrequency();
            currentWordInDictionary.setTotalAppearancesInAllCorpus(termCountInDoc);
        }
    }

    /**
     * Check if the given string is a number acccording to the first 2 chars.
     *
     * @param termName String. word to check if represent a number
     * @return Boolea. true if the seting is number, false otherwise,
     */
    public static boolean representNumber(String termName) {
        if(termName == null) {
            throw new NullPointerException();
        }

        char firstChar = termName.charAt(0);

        if(termName.length() == 1) {
            return Character.isDigit(firstChar);

        } else if(termName.length() >= 2) {

            char secondChar = termName.charAt(1);

            if (Character.isDigit(firstChar)) {
                return true;

            } else if ((firstChar == '-' || firstChar == '+') && Character.isDigit(secondChar)) {
                return true;
            }
        }

        return false;
    }


    /**
     * the method checks if the df of the current term appear the corpus the minimal times we decided.
     *
     * @param DFofTerm - the df of the current term.
     * @return false - if the word appears less or equal to the number of appearances we decided. else - true.
     */
    private boolean checkIfPassMinimalAppearance(int DFofTerm)
    {
        if (DFofTerm <= this.minimalApearance)
        {
            return false;

        }
        return true;
    }

    /**
     * the method add the current docTerm to the correct posting File
     *
     * @param docTerm
     * @param docIndex
     */
    private void addToPostingFile(DocTerm docTerm, int docIndex) {
        String first2LettersInTerm = "";


        /*checks if the word is number. if it does - we will save the
        PostFile as under the name numbers for all numbers.*/
        if (representNumber(docTerm.getTermName()))
        {
            first2LettersInTerm = "numbers";
        }
        else if(checkIfTermUnique(docTerm.getTermName())) //checks if the words is unique.
        {
            first2LettersInTerm = "unique";
        }
        else if(docTerm.getTermName().length() > 1)
        {
            first2LettersInTerm = (docTerm.getTermName().toLowerCase()).substring(0, 2);
        }
        else {
            return;
        }

        PostFile postFile;

        synchronized (this.addToPostingFileLock) //mutex
        {

            if (!this.mutexHashTable.containsKey(first2LettersInTerm)) //
            {
                this.mutexHashTable.put(first2LettersInTerm, new Object());
            }
        }
        synchronized (this.mutexHashTable.get(first2LettersInTerm)) {
            postFile = getPostingFile(first2LettersInTerm);
            ReadFile.logger.info("Indexer;'addToPostingFile' method -  docTerm : " + docTerm.getTermName() + " with postFile name: " + first2LettersInTerm);
            postFile.addTermToPostingFile(docTerm.getTermName(), docTerm.toString() + "," + docIndex);

        }
    }


    /**
     * the method receive a term and checks if the term is unique -
     * which means if the term contains in his first 2 chars characters that are not a letter or a digit.
     * @param termName - String. the term the method will check.
     * @return true - the term is unique. else - regular term.
     */
    public static boolean checkIfTermUnique(String termName) {

        if(termName.length() == 1) {
            return false;
        }
        char firstLetter = termName.charAt(0);
        char secondLetter = termName.charAt(1);

        /*if the char is not a letter and not a digit */
        if( !(representNumber(termName) || (Character.isLetter(firstLetter) && Character.isLetter(secondLetter)) ) )
        {
            return true;
        }

        return false;
    }


    /**
     * the method clean all the Post Files in the MemoryCache.
     * * * * the method will be called after we indexed all the docs* * *.
     */
    @Override
    public void streamAllPostFilesInCacheMemory(MemoryManager oldMemory) {

        for (String postFileName : oldMemory.getAllFilesNamesInMemory()) {

            PostFile currentPostFileFromCache = oldMemory.getFileFromMemory(postFileName);
            String currentPostFileNameFromCache = postFileName;

            this.threadPool.execute(() -> streamPostFileToDisc(currentPostFileFromCache, currentPostFileNameFromCache));
        }
    }

    /**
     * the method stream the all thw words to the necessary posting file in the Disc.
     *
     * @param postFile
     * @param first2LettersInTerm
     */
    private void streamPostFileToDisc(PostFile postFile, String first2LettersInTerm) {
        String postFilePath = this.destPath.concat("PostingFiles/".concat(first2LettersInTerm.concat(".txt")));

        File file = new File(postFilePath);

        try {

            if (!file.exists()) {
                file.createNewFile(); // if file already exists will do nothing
            }

            FileWriter FW = new FileWriter(postFilePath, true); //Set true for append mode
            for (Pair pair : (List<Pair>) postFile.getAllWords()) {
                FW.write(pair.getTermName().concat(":".concat(pair.getData().concat("\n"))));
            }
            FW.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * the method return the instance of the the necessary posting File *from the Cache Memory*.
     *
     * @param first2LettersInTerm - String - the necessary PostFile name.
     * @return instace of the necessary posting File.
     */
    private PostFile getPostingFile(String first2LettersInTerm) {
        PostFile postFile = this.memoryManager.getFileFromMemory(first2LettersInTerm); // if the the CacheMemory doesn't contain the necessary PostFile return null;

        if (postFile != null) {
            return postFile;
        }

        /*if we arrive here it means the necessary postFile is not the in CacheMemory - we need to create new one and add it to the cache.*/

        return createNewPostFile(first2LettersInTerm);
    }


    /**
     * the method creates a new posting File and add it to the Cache Memory.
     *
     * @param first2LettersInTerm
     */
    private PostFile createNewPostFile(String first2LettersInTerm) {
        PostFile postFile = new PostFile();
        this.memoryManager.addFileToMemory(first2LettersInTerm, postFile);
        return postFile;
    }


    /**
     * the method gets the posting files *in the Disc* and get the data and send them to getDataFromPostingFile to extract the data.
     *
     * @return
     */
    @Override
    public PostFile getFilesToSort() {

        TermDictionaryData.setNumberOfDocsInCorpus(this.docNameDictionary.size());

        String[] allPostFilesNames;

        File file = new File(this.destPath.concat("PostingFiles"));

        allPostFilesNames = file.list();

        String currentPostingFileName = "";

        PostFile postFile = null;

        /*checks if the PostingFile already exsists on disc */
        for (String pathName : allPostFilesNames) {
            currentPostingFileName = pathName.substring(0, pathName.lastIndexOf('.'));

            String postFileName = currentPostingFileName;
            this.threadPool.execute(() -> getDataFromPostingFile(postFileName));
        }

        return null;
    }


    /**
     * the method read the data from the posting file and send it to sort by alphabetical order.
     *
     * @param first2LettersInTerm
     */
    private void getDataFromPostingFile(String first2LettersInTerm) {
        File file = new File(this.destPath.concat("PostingFiles/" + first2LettersInTerm + ".txt"));
        PostFile allWordsToSort = new PostFile();
        BufferedReader br;
        String line;

        try {
            br = new BufferedReader(new FileReader(file));

            while ((line = br.readLine()) != null) {
                if (line.indexOf(":") == -1) {
                    continue;
                }
                String termName = line.substring(0, line.indexOf(':'));
                String data = line.substring(line.indexOf(':') + 1);
                allWordsToSort.addTermToPostingFile(termName, data);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sortArrayList(allWordsToSort.getAllWords());
        reWriteToPostingFilesInDisc(allWordsToSort.getAllWords(), first2LettersInTerm);
    }


    /**
     * sort the posting file by alphabetical order.
     *
     * @param allWords
     */
    private void sortArrayList(List<Pair> allWords) {
        Collections.sort(allWords, new SortPair());
    }


    /**
     * the method receive the the postFile sorted and update all pointers in the termsDictionary and compute IDF
     * and reWrite to the Posting File in the disc.
     *
     * @param allWords
     * @param postFileName
     */
    private void reWriteToPostingFilesInDisc(List allWords, String postFileName) {
        String path = this.destPath.concat("PostingFiles/".concat(postFileName.concat(".txt")));

        File file = new File(path);
        file.delete();

        boolean isNumbersPostFile = false;

        if (postFileName.equals("numbers")) {
            isNumbersPostFile = true;
        }

        try {
            file.createNewFile(); // if file already exists will do nothing
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path, true)); //Set true for append mode

            int lineIndex = 0;

            Pair currentPair = (Pair) allWords.get(0);
            String currentTerm = currentPair.getTermName();
            String currentData = currentPair.getData();



            TermDictionaryData currentTermDictionaryData;

            if (isNumbersPostFile)
            {
                currentTermDictionaryData = this.termsDictionary.get(currentTerm);
            }
            else
            {
                currentTermDictionaryData = getCurrentTermDictionaryDataFromTermsDictionary(currentTerm);
            }

            /*check if the term pass minimal threshHold of tf we decided. if not delete from the word form dictionary*/
            if(currentTermDictionaryData != null && checkIfPassMinimalAppearance(currentTermDictionaryData.getDocFrequency()))
            {
                currentTermDictionaryData.setPtr(lineIndex);
                currentTermDictionaryData.setIdf();

                lineIndex++;

                bufferedWriter.write(currentData + "\n");

            }
            else if(currentTermDictionaryData != null)
            {
                this.termsDictionary.remove(getTermDictionaryName(currentTerm));
            }


            currentTerm = currentTerm.toLowerCase(); //change the word to LowerCase either way for comparing with the next word in the posting file

            for (int i = 1; i < allWords.size(); i++)
            {

                Pair nextPair = (Pair) allWords.get(i);
                String nextTerm = nextPair.getTermName();

                String nextData = nextPair.getData();

                if (!nextTerm.toLowerCase().equals(currentTerm)) //if the words are the same we dont need to do anything.
                {
                    if (isNumbersPostFile) //the term is numbers - we dont need to check Lower\Upper Case.
                    {
                        currentTermDictionaryData = this.termsDictionary.get(nextTerm);
                    }
                    else //term is a word.
                    {
                        currentTermDictionaryData = getCurrentTermDictionaryDataFromTermsDictionary(nextTerm);
                    }

                    if(currentTermDictionaryData != null) {
                        if (checkIfPassMinimalAppearance(currentTermDictionaryData.getDocFrequency())) {
                            currentTermDictionaryData.setPtr(lineIndex);
                            currentTermDictionaryData.setIdf();
                        } else //the term didnt pass the minimal df - delete the word from the dictionary
                        {
                            this.termsDictionary.remove(getTermDictionaryName(nextTerm));
                        }
                    }

                }

                if(currentTermDictionaryData != null && checkIfPassMinimalAppearance(currentTermDictionaryData.getDocFrequency()))
                {
                    bufferedWriter.write(nextData + "\n");
                    lineIndex++;
                }
                currentTerm = nextTerm.toLowerCase();


            }

            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }


    /**
     * the method gets the data of the next and different TermDictionaryData from the termsDictionary.
     * the method checks all the case sensitive (if the word is UpperCase or LowerCase)
     *
     * @param currentTerm
     * @return
     */
    private TermDictionaryData getCurrentTermDictionaryDataFromTermsDictionary(String currentTerm) {
        if (this.termsDictionary.containsKey(currentTerm)) {
            return this.termsDictionary.get(currentTerm);

        } else if (this.termsDictionary.containsKey(currentTerm.toLowerCase())) {
            return this.termsDictionary.get(currentTerm.toLowerCase());

        } else if (this.termsDictionary.containsKey(currentTerm.toUpperCase())) {//the word is in the dictionary in UpperCase
            return this.termsDictionary.get(currentTerm.toUpperCase());
        }
        return null;
    }

    /**
     * the method return how the current term is shown in the dictionary.
     * @param currentTerm
     * @return
     */
    private String getTermDictionaryName(String currentTerm){
        if (this.termsDictionary.containsKey(currentTerm)) {
            return currentTerm;

        } else if (this.termsDictionary.containsKey(currentTerm.toLowerCase())) {
            return currentTerm.toLowerCase();

        } else if (this.termsDictionary.containsKey(currentTerm.toUpperCase())) {//the word is in the dictionary in UpperCase
            return currentTerm.toUpperCase();
        }
        return null;
    }

    /**
     * the method stream all dictionaries to the disc
     */
    @Override
    public void writeDictionariesToDisc() {

        new File(this.destPath.concat("Dictionaries/")).mkdir();

        Object[] keys = this.termsDictionary.keySet().toArray();
        Arrays.sort(keys);

        writeTermsDictionaryToDisc(keys);

        writeDocNameDictionaryToDisc();
    }


    /**
     * the method loads the dictionaries (if exist) from the disc to the memory.
     *
     * @param destPath
     */
    public static ConcurrentHashMap loadTermsDictionaryFromDisc(String destPath) {

        ConcurrentHashMap<String, TermDictionaryData> termsDictionary = new ConcurrentHashMap<>();

        String path = destPath.concat("Dictionaries/".concat("termsDictionary.txt"));
        File file = new File(path);

        if(!file.exists()) {
        System.out.println("coulndt find term dictionary at: " + path);

        return termsDictionary;
    }
        try {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = null;

        while ((line = br.readLine()) != null)
        {
            if(line.indexOf(":") == -1) {
                continue;
            }

            String word = line.substring(0, line.indexOf(":"));
            String data = line.substring(line.indexOf(":") + 1);

            termsDictionary.put(word, new TermDictionaryData(data));
        }
        br.close();

        return termsDictionary;
    }
        catch (IOException e)
    {
        e.printStackTrace();
    }

        return termsDictionary;
}

    /**
     * the method load the termDictionary and the docDictionary from the disc.
     */
    public static List<DocData> loadDocDictionaryFromDisc(String destPath) {

        String path = destPath.concat("Dictionaries/" +"docNameDictionary.txt");
        List<DocData> docDictionary = new ArrayList<>();

        File file = new File(path);

        if(!file.exists()) {
//            throw new FileNotFoundException("coulndt find documents dictionary at: " + path);
            System.out.println("coulndt find documents dictionary at: " + path);

            return docDictionary;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;

            while ((line = br.readLine()) != null)
            {
                if(line.equals("")) {
                    continue;
                }

                docDictionary.add(new DocData(line));
            }

            br.close();

            return docDictionary;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return docDictionary;
    }

    /**
     * the method writes all the DocNameDictionary DocNames and their indexes to the disc.
     */
    private void writeDocNameDictionaryToDisc() {

        String path = this.destPath.concat("Dictionaries/" + "docNameDictionary.txt");

        File file = new File(path);

        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(path, true); //Set true for append mode
            file.createNewFile(); // if file already exists will do nothing

            for(int i = 0; i < this.docNameDictionary.size(); i++)
            {
                String docName = this.docNameDictionary.get(i).toString();

                fileWriter.write(this.docNameDictionary.get(i).toString() + "\n");
            }

            fileWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * the method writes all the TermDictionary words and data to the disc.
     */
    private void writeTermsDictionaryToDisc(Object[] sortedKeys) {

        String path = this.destPath.concat("Dictionaries/".concat("termsDictionary.txt"));
        File file = new File(path);

        try {

            file.createNewFile();// if file already exists will do nothing
            FileWriter fileWriter = new FileWriter(path, true); //Set true for append mode


            for(int i = 0 ; i < sortedKeys.length ; i++)
            {
                TermDictionaryData TDD = this.termsDictionary.get(sortedKeys[i]);

                fileWriter.write(sortedKeys[i] + ":" + TDD +"\n");
            }
            fileWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Getter for Catch memory.
     * @return Catch. represent all the posting files Objects.
     */
    @Override
    public MemoryManager getDataStructure() {
        return this.memoryManager;
    }

    /**
     * the method set a new cache.
     */
    @Override
    public void setNewDataStructure(MemoryManager newMM) {
        this.memoryManager = newMM;
    }

    /**
     * getter method.
     * @return int - the size of the docs Dictionary.
     */
    @Override
    public int getNumberOfDocs() {
        return this.docNameDictionary.size();
    }

    /**
     * getter method.
     * @return int - the size of the term dictionary.
     */
    @Override
    public int getNumberOfTerms() {
        return this.termsDictionary.size();
    }

    /**
     * getter method.
     * @return ConcurrentHashMap<String, TermDictionaryData> - Terms Dictionary.
     */
    @Override
    public ConcurrentHashMap<String, TermDictionaryData> getTermDictionary() {
        return this.termsDictionary;
    }

    /**
     * getter method.
     * @return List<DocData> - Document Dictioaney.
     */
    @Override
    public List<DocData> getDocDictionary() {
        return this.docNameDictionary;
    }

}
