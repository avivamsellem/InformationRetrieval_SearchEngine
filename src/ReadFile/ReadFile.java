package ReadFile;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

/**
 * Class represent object that able to Load, parse and index a given corpus.
 */
public class ReadFile {

    static Handler fileHandler = null;
    static final Logger logger = Logger.getLogger("Logger");
    private IParser parser;
    private ThreadPoolManager threadPoolManager;
    private IndexerInterface indexer;
    private boolean stemmer;
    private String destPath;

    /**
     * Constructor.
     *
     * @param stop_word_path String. path to stop word file.
     * @param toStem Boolean. indicate if stem is needed.
     * @param saveSingle Boolean. indicate if save header is needed.
     * @param numOfThreads Int. number of threads.
     */
    public ReadFile(String destinationPath, String stop_word_path, Boolean toStem, boolean saveSingle , int numOfThreads) {
        if(toStem) {
            this.destPath = destinationPath.concat("stem/");
        }
        else {
            this.destPath = destinationPath.concat("no stem/");
        }

        File directory = new File(this.destPath);

        if (!directory.exists()) {
            directory.mkdir();
        } else {
            deleteFolder(directory);
            directory.mkdir();
        }

        //LOGGER
        initialLogger(this.destPath);

        //Load Stop Words
        this.stemmer = toStem;
        MemoryManager cacheMemoryManager = new Cache();
        this.threadPoolManager = new ThreadPoolManager(numOfThreads);
        this.indexer = new Indexer(cacheMemoryManager, this.threadPoolManager, this.destPath);
        this.parser = new Parser(this.indexer, stop_word_path, toStem, saveSingle, this.logger);
    }

    /**
     * Initial logger for the first time. config name,
     * format and create file.
     *
     * https://stackoverflow.com/questions/33598097/whats-the-best-way-to-initialise-a-logger-in-a-class
     */
    private void initialLogger(String destPath) {
        File directory = new File(this.destPath.concat("Logger"));

        if (!directory.exists()) {
            directory.mkdir();
        }

        try {

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd HH-mm-ss");
            fileHandler = new FileHandler(destPath.concat("Logger/log " + dateFormat.format(date)+ ".log"));
            Logger.getLogger(fileHandler.toString()).setLevel(Level.WARNING);
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.setLevel(Level.WARNING);
            logger.info("ReadFile - constructor - logger created.");

        } catch (SecurityException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse and index all the corpus given at the path directory.
     *
     * @param corpusPath - String. represent the path to the Corpus.
     */
    public void engineController(String corpusPath) {

        logger.info("ReadFile - readDirectory - Started.");

        if (corpusPath == null || Files.notExists(Paths.get(this.destPath)) || Files.notExists(Paths.get(corpusPath))) {
            throw new IllegalArgumentException("illegal argument is given");
        }

        File corpusFile = new File(corpusPath);
        List<File> fileList = new ArrayList<>();

        getAllFiles(corpusFile, fileList);
        String regex = "<[/]?[DOCNO|DATE[1]?|TI|TEXT]*>";

        int batchSize = 100;
        int fileCounter = 0;
        int batch_number = 1;

        //start iterate all files in corpus
        for (File file : fileList) {
            //add task to split the current file
            this.threadPoolManager.execute(() -> SplitAndParseDoc(file, regex));
            fileCounter++;

            //when files counter get batch, wait for threadpool to finish and dump memory to files
            if(fileCounter >= batchSize) { //means need to write all batch to the disk

                logger.warning("[ReadFile]: stating to write batch number " + batch_number);
                waitForThreadPool();

                fileCounter = 0;
                batch_number++;

                logger.warning("[ReadFile]: streamBatchFiles " + batch_number);
                streamBatchFiles();
            }
        }

        //main thread waiting to all tasks
        logger.warning("[ReadFile]: waitForThreadPool");
        waitForThreadPool();

        //write the remaining docs.
        logger.warning("[ReadFile]: streamBatchFiles");
        streamBatchFiles();

        logger.warning("[ReadFile]: waitForThreadPool");
        waitForThreadPool();

        logger.warning("[ReadFile]: sortAndSaveDataStructure");
        sortAndSaveDataStructure();

        logger.warning("[ReadFile]: waitForThreadPool");
        waitForThreadPool();

        logger.warning("[ReadFile]: shutting down");
        shutdown();
    }

    /**
     * Sorting posting files and save structures.
     */
    private void sortAndSaveDataStructure() {
        //sort

        this.indexer.getFilesToSort();

        waitForThreadPool();

        //Dictionaries
        this.indexer.writeDictionariesToDisc();
    }

    /**
     * Replace indexer Data Structure.
     */
    private void streamBatchFiles() {
        //replace data structure
        MemoryManager dataStructure = this.indexer.getDataStructure();
        this.indexer.setNewDataStructure(new Cache());

        //dump
        this.threadPoolManager.execute(() -> this.indexer.streamAllPostFilesInCacheMemory(dataStructure));
    }

    /**
     * Wait until all task at the current ThreadPoolExecutor are done
     */
    private void waitForThreadPool() {

        this.threadPoolManager.sleepUntilAllThreadAreDone();
    }

    /**
     * Shutdown ReadFile object.
     */
    private void shutdown() {
        this.threadPoolManager.shutdown();
    }

    /**
     * this method get the file path and return a List of all the subFiles in the given directory.
     * works recursively.
     *
     * @param dir   - File. represent the path.
     * @param files - represent the List which will conatin all the subFiles.
     */
    private void getAllFiles(File dir, List<File> files) {
        logger.info("ReadFile - getAllFiles - Started.");

        if (dir.isDirectory()) {
            for (File subFile : dir.listFiles()) {
                if (subFile.isFile()) {
                    files.add(subFile);

                } else {
                    for (File subSubFile : subFile.listFiles()) {

                        if (subSubFile.isFile()) {
                            files.add(subSubFile);

                        } else {
                            getAllFiles(subSubFile, files);
                        }
                    }
                }
            }

        } else {
            files.add(dir);
        }
    }

    /**
     * Split and parse a file containig one or more documents.
     *
     * @param file File. file containing all the documents.
     * @param regex String. regex containing all the relevant catagories from the document.
     */
    private void SplitAndParseDoc(File file, String regex) {
        logger.info("ReadFile - getSplitedText - Started.");

        BufferedReader reader;
        Pattern pat = Pattern.compile(regex);

        Stack<String> stackCategories = new Stack<>(); //conatins all the single lined tags

        Document doc = new Document();  //contain a single doc after parsing tags (will be added to docs)

        List<String> tags;
        List<Integer> tagStart;
        int docCouter = 1;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();

            String cache = "";

            while (line != null) {

                Matcher matcher = pat.matcher(line);

                if (line.equals("<DOC>")) {
                    doc = new Document();

                } else if (line.equals("</DOC>")) {

                    Document threadDoc = new Document(doc);
                    this.threadPoolManager.execute(() -> this.parser.parseDocuments(threadDoc));

                    this.logger.info(String.format("File Reader sent to parser Doc:%d.", docCouter++));

                } else {
                    tags = new ArrayList<>();
                    tagStart = new ArrayList<>();
                    while (matcher.find()) {
                        tags.add(matcher.group());
                        tagStart.add(matcher.start());
                    }

                    if (tags.size() > 1) {

                        int start = tagStart.get(tagStart.size() / 2 - 1) + tags.get(tags.size() / 2 - 1).length();
                        int end = tagStart.get(tagStart.size() / 2);

                        String innerText = line.substring(start, end).trim();
                        String tagName = tags.get(tags.size() / 2 - 1);

                        if(line.contains("FB") && !innerText.contains("FB")) {
                            System.out.println("line: " + line + ", innerText: " + innerText);
                        }

                        doc.setTag(tagName, innerText);

                    } else if (tags.size() == 1) {

                        if (tags.get(0).contains("/")) {
                            String category = stackCategories.pop();

                            doc.setTag(category, cache); //save data inner
                            cache = "";

                        }
                        else {
                            stackCategories.push(tags.get(0));
                            reader.readLine();
                            reader.readLine();
                            reader.readLine();

                            line = reader.readLine();
                            if(line.length() > 5 && !line.substring(0 ,6).equals("<F P=")) {
                                line = reader.readLine();
                                continue;
                            }
                        }

                    } else if (!line.equals("") && !stackCategories.empty()) { //add the catch only if there is relevant category in the stack
                        if (cache.length() != 0) {
                            cache += "\n" + line;

                        } else {
                            cache = line;
                        }
                    }
                }

                line = reader.readLine();

                //skip empty lines
                while(line != null && line.equals("")) {

                    line = reader.readLine();

                    if(line == null) {
                        return;
                    }
                }
            }
            reader.close();

        } catch (IOException e)
        {
            logger.warning("couldn't read the file: " + file.getName() + ".");
        }
    }

    /**
     * the method collects the the sizes of the dictionaries and send them as a list.
     * @return List<String> - a list of of the sizes of the dictionaries.
     */
    public List<String> getSummery() {

        List<String> summery = new ArrayList<>();

        int docNumber = this.indexer.getNumberOfDocs();
        int termSize = this.indexer.getNumberOfTerms();

        summery.add("" + docNumber);
        summery.add("" + termSize);

        return summery;
    }

    /**
     * Delete the given folder and it content.
     *
     * @param folder String. Path to directory to delete.
     */
    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * getter method.
     * @return ConcurrentHashMap<String, TermDictionaryData> - the Term Dictionary.
     */
    public ConcurrentHashMap<String, TermDictionaryData> getTermDictionary() {
        return this.indexer.getTermDictionary();
    }

    /**
     * getter method.
     * @return List<DocData> - the Documents Dictionary.
     */
    public List<DocData> getDocDictionary() {
        return this.indexer.getDocDictionary();
    }
}
