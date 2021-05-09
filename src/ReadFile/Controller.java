package ReadFile;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * the class create the GUI for the Program.
 */
public class  Controller {

    @FXML
    //Part A
    public TextField tf_corpus_src;
    public TextField tf_corpus_dest;
    public CheckBox cb_stemm;
    public CheckBox cb_save_single_words;
    public Button btn_parse;
    public Button btn_load_dictionary;
    public Button btn_show_dictionary;
    public Button btn_reset;
    public Button btn_load_corpus;
    public Button btn_save_corpus;
    public Button btn_zipf;

    private ReadFile RF = null;
    private ConcurrentHashMap<String, TermDictionaryData> termDictionary;
    private List<DocData> docDictionary;


    //Part B
    public TextField tf_query_text;
    public TextField tf_query_file;
    public TextField tf_queries_dest;
    public CheckBox cb_identities;
    public CheckBox cb_semantic_treatment;
    public Button btn_RUN;
    public Button btn_browse;
    public Button btn_save_queries_results;
    public Button btn_run_queries;

    private ISearcher searcher;


    /**
     * Represent the action of the parse button
     */
    public void pressParse() {

        if(validPaths()) {

            long startTime = System.nanoTime();

            startParse();

            disableButtons();

            long endTime = System.nanoTime();

            long timer = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);

            List<String> summery = this.RF.getSummery();

            try {
                Stage stage = new Stage();
                stage.setTitle("About");
                Pane pane = new Pane();

                Label l1 = new Label("run time: " + timer);
                Label l2 = new Label("\nnumber of docs: " + summery.get(0));
                Label l3 = new Label("\n\nnumber of unique words: " + summery.get(1));
                pane.getChildren().add(l1);
                pane.getChildren().add(l2);
                pane.getChildren().add(l3);

                Scene scene = new Scene(pane, 200, 200);

                stage.setScene(scene);
                stage.setWidth(500);
                stage.setHeight(100);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

            enableButtons();
        }
    }

    /**
     * The method checks if the given paths is valid.
     *
     * @return boolean - true - if the paths are valid, else - return false.
     */
    private boolean validPaths() {

        return validCorpusPath() && validDestinationPath();
    }

    /**
     * The method checks if the valid destination is valid.
     *
     * @return boolean - true - if the path is valid. else - return false.
     */
    private boolean validDestinationPath() {

        String dest = tf_corpus_dest.getText();

        if(!new File(dest).exists()) {
            showMessage("destination folder doesnt exists - please check destination path");

            return false;
        }

        return true;
    }

    /**
     * The method checks if the valid corpus path is valid.
     *
     * @return boolean - true - if the path is valid. else - return false.
     */
    private boolean validCorpusPath() {

        String corpusSrcPath = tf_corpus_src.getText();

        if(!new File(corpusSrcPath).exists()) {
            showMessage("source path not valid, please check source path");

            return false;
        }

        String corpusPath = corpusSrcPath.concat("/corpus/");

        if(!new File(corpusPath).exists()) {
            showMessage("cannot find corpus folder - please check source path");

            return false;
        }

        String stopWordPath = corpusSrcPath.concat("/05 stop_words.txt");

        if(!new File(stopWordPath).exists()) {
            showMessage("cannot find stop word file - please check source path");

            return false;
        }

        return true;
    }

    /**
     * Private method activating the Parser.
     */
    private boolean startParse() {

        String corpusSrcPath = tf_corpus_src.getText();
        String corpusPath = corpusSrcPath.concat("/corpus/");

        initialReadFile();
        this.RF.engineController(corpusPath);

        this.termDictionary = this.RF.getTermDictionary();
        this.docDictionary = this.RF.getDocDictionary();
        btn_parse.setDisable(false);

        return true;
    }

    /**
     * Private method for initializing the ReadFile class.
     */
    private void initialReadFile() {

        int numberOfThread = Runtime.getRuntime().availableProcessors();

        String corpusSrcPath = tf_corpus_src.getText().replaceAll("\\\\", "/").concat("/");
        String corpusDestPath = tf_corpus_dest.getText().replaceAll("\\\\", "/").concat("/");

        boolean stem = cb_stemm.isSelected();
        boolean saveSingleWords = cb_save_single_words.isSelected();
        String stopWordPath = corpusSrcPath.concat("/05 stop_words.txt");

        this.RF = new ReadFile(corpusDestPath, stopWordPath, stem, saveSingleWords, numberOfThread);

    }

    /**
     * represent the action of opening the browser of the location of the
     * corpus we want to build Inverted index for.
     */
    public void openFolderChooseDialogSource() {

        DirectoryChooser directoryChooser = new DirectoryChooser();

        File file = directoryChooser.showDialog(null);

        if (file != null) {
            tf_corpus_src.setText(file.toString());
        }
    }

    /**
     * Represent the action of choosing the location of
     * where we want to save out Inverted Index.
     */
    public void openFolderChooseDialogDestination() {

        DirectoryChooser directoryChooser = new DirectoryChooser();

        File file = directoryChooser.showDialog(null);

        if (file != null) {
            tf_corpus_dest.setText(file.toString());
        }
    }

    /**
     * the method opens a scene and show a table of all the term dictionary.
     */
        public void showDictionary() {

            if (this.termDictionary == null || this.docDictionary == null) {

                showMessage("please make sure there is a dictionary first");

                return;
            }


            List<Pair> reducedDict = AIndexer.reduceTermDictionary(this.termDictionary);
            Collections.sort(reducedDict, new SortPair());

            TableColumn<Pair, String> nameColumn = new TableColumn<>("Terms");
            nameColumn.setMinWidth(500);
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("termName"));

            //Quantity column
            TableColumn<Pair, String> quantityColumn = new TableColumn<>("Total Appearances");
            quantityColumn.setMinWidth(100);
            quantityColumn.setCellValueFactory(new PropertyValueFactory<>("data"));

            TableView<Pair> table = new TableView<>();
            table.setMinHeight(600);
            table.setMinWidth(400);
            table.setItems(getProduct(reducedDict));
            table.getColumns().addAll(nameColumn, quantityColumn);

            VBox vBox = new VBox();
            vBox.getChildren().addAll(table);

            Scene scene = new Scene(vBox);
            Stage stage = new Stage();
            stage.setHeight(600);
            stage.setWidth(600);
            stage.setScene(scene);
            stage.show();
        }


    /**
     * the method shows a message by a given String to the user.
     *
     * @param str - String - a given message to show the user.
     */
    private void showMessage(String str) {

        Label label = new Label(str);
        VBox vBox = new VBox();
        vBox.getChildren().addAll(label);

        Scene scene = new Scene(vBox);
        Stage stage = new Stage();
        stage.setHeight(40);
        stage.setWidth(400);
        stage.setScene(scene);
        stage.show();

        return;
    }

    //Get all of the products

    /**
     * getter.
     *
     * @param list - a list of pair of term and how many times they appear in the Corpus.
     *
     * @return - Observable List for displaying in th GUI.
     */
    public ObservableList<Pair> getProduct(List<Pair> list) {

        ObservableList<Pair> products = FXCollections.observableArrayList();

        for (int i = 0; i < list.size(); i++) {
            products.add(list.get(i));
        }

        return products;
    }


    /**
     * getter.
     *
     * @param list - a list of pair of term and how many times they appear in the Corpus.
     *
     * @return - Observable List for displaying in th GUI.
     */
    public ObservableList<javafx.util.Pair> castPairToObservableList(List<javafx.util.Pair> list) {

        ObservableList<javafx.util.Pair> products = FXCollections.observableArrayList();

        for (int i = 0; i < list.size(); i++) {
            javafx.util.Pair pair = list.get(i);
            DocData docData = (DocData) pair.getKey();
            docData.setDocIndex(i + 1);
            String score = pair.getValue().toString();
            String docString;

            if (this.cb_identities.isSelected()) {
                docString = docData.toPresentWithIdentities();
            } else {
                docString = docData.toPresentWithoutIdentities();
            }

            products.add(new javafx.util.Pair(docString, score));
        }

        return products;
    }

    /**
     * Represent the action of loading the Dictionaries from the disc to the memory.
     */
    public boolean loadDictionary() {

        boolean stem = cb_stemm.isSelected();

        String dictionariesPath = tf_corpus_dest.getText();

        if (stem) {
            dictionariesPath += "/stem/";
        } else {
            dictionariesPath += "/no stem/";
        }
        if(!new File(dictionariesPath).exists()) {
            showMessage("Couldn't find inverted index at the given path");

            return false;
        }

        this.termDictionary = Indexer.loadTermsDictionaryFromDisc(dictionariesPath);
        this.docDictionary = Indexer.loadDocDictionaryFromDisc(dictionariesPath);

        return true;

    }

    /**
     * represent the action of showing the zipf's law chart.
     */
     /*
    public void showZipfLawHist() {

        boolean stem = cb_stemm.isSelected();
        String path;

        if(stem) {
            path = tf_corpus_dest.getText().concat("stem/");
        }
        else {
            path = tf_corpus_dest.getText().concat("no stem/");
        }
        initialReadFile();

        List<Pair> reducedDict = this.RF.distributionArray(path);

        Collections.sort(reducedDict, new SortPairByAppearance());

        double[] totalTermFreqency = new double[reducedDict.size()];

        String maxTotalAppearances = reducedDict.get(0).getData();;
        int maxAppearances = 1;
        try {
            maxAppearances = Integer.parseInt(maxTotalAppearances);
        } catch (NullPointerException e) {}
        catch (NumberFormatException e) {}

        for(int i = 0 ; i < reducedDict.size() ; i++) {

            Float totalAppearances;

            try {
                totalAppearances = Float.parseFloat(reducedDict.get(i).getData());

                totalTermFreqency[i] = totalAppearances/maxAppearances;
            }
            catch (NullPointerException e) {}
            catch (NumberFormatException e) {}
        }


        System.out.println("starting to build zipf graph");
        final ZipfLawGraph demo = new ZipfLawGraph(totalTermFreqency);
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }

*/


    /**
     * Reset all the configuration to default.
     */
    public void reset() {

        String pathToIndexFiles = this.tf_corpus_dest.getText();

        if (!deletePreviousInvertedIndexs(pathToIndexFiles)) {

            Label label = new Label("There is nothing to delete");

            VBox vBox = new VBox();
            vBox.getChildren().addAll(label);

            Scene scene = new Scene(vBox);
            Stage stage = new Stage();
            stage.setHeight(100);
            stage.setWidth(200);
            stage.setScene(scene);
            stage.show();

            return;
        }
    }

    /**
     * the method checks of the previous Inverted Index as been deletd.
     *
     * @param pathToIndexFiles - String - a path to the previous Inverted Index.
     *
     * @return boolean - true - if succeeded deleting the previous Inverted Index. else - return false.
     */
    private boolean deletePreviousInvertedIndexs(String pathToIndexFiles) {

        File stemFolder = new File(pathToIndexFiles + "/stem");
        File noStemFolder = new File(pathToIndexFiles + "/no stem");

        return delete(stemFolder) | delete(noStemFolder);
    }

    /**
     * the method deleting a file in the disc.
     *
     * @param file - File.
     *
     * @return true - if succeeded. else - return false.
     */
    private boolean delete(File file) {
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                delete(c);
            }
        }

        if (!file.delete()) {
            return false;
        }

        return true;
    }

    /**
     * Disable all program buttons
     */
    private void disableButtons() {
        //Part A
        btn_parse.setDisable(true);
        btn_reset.setDisable(true);
        btn_load_dictionary.setDisable(true);
        btn_show_dictionary.setDisable(true);
        btn_load_corpus.setDisable(true);
        btn_save_corpus.setDisable(true);
        btn_zipf.setDisable(true);
        cb_stemm.setDisable(true);
        cb_save_single_words.setDisable(true);

        //Part B
        cb_identities.setDisable(true);
        cb_semantic_treatment.setDisable(true);
        btn_RUN.setDisable(true);
        btn_browse.setDisable(true);
        btn_save_queries_results.setDisable(true);
        btn_run_queries.setDisable(true);
    }

    /**
     * Enable all program buttons
     */
    private void enableButtons() {
        //Part A
        btn_parse.setDisable(false);
        btn_reset.setDisable(false);
        btn_load_dictionary.setDisable(false);
        btn_show_dictionary.setDisable(false);
        btn_load_corpus.setDisable(false);
        btn_save_corpus.setDisable(false);
        btn_zipf.setDisable(false);
        cb_stemm.setDisable(false);
        cb_save_single_words.setDisable(false);

        //Part B
        cb_identities.setDisable(false);
        cb_semantic_treatment.setDisable(false);
        btn_RUN.setDisable(false);
        btn_browse.setDisable(false);
        btn_save_queries_results.setDisable(false);
        btn_run_queries.setDisable(false);
    }


    /**
     * the method receive a query from the user and start operating the program to retrieve the documents.
     */
    public void runQuery() {

        if (this.termDictionary == null || this.docDictionary == null) {

            showMessage("please make sure there is a dictionary first");

            return;
        }

        if(!new File(this.tf_queries_dest.getText()).exists()) {
            showMessage("couldn't find destination query file. please validate path");

            return;
        }

        File queriesDestFile = new File(this.tf_queries_dest.getText().concat("/queries_result_b-" + Ranker.b + "_k-" + Ranker.k + "_HW-" + Ranker.headerWeight + "_ST-" + this.cb_semantic_treatment.isSelected() + ".txt"));

        if(!queriesDestFile.exists()) {
            try {
                queriesDestFile.createNewFile();
            }
            catch (IOException e) {
                showMessage("Couldn't create 'queries_result.txt' at " + queriesDestFile.getAbsolutePath());

                return;
            }
        }

        ASearcher.loadStopWords(this.tf_corpus_src.getText().replaceAll("\\\\", "/").concat("/05 stop_words.txt"));

        createAndUpdateSearcher();

        String queryString = this.tf_query_text.getText();

        List<javafx.util.Pair> results = this.searcher.search(queryString, cb_semantic_treatment.isSelected());

        int randNumber = generateNumberInRange(100, 1000);

        Query query = new Query(randNumber, queryString, "", "");

        writeQueryResultToTrecEvalFile(queriesDestFile, query, results);

        TableColumn<javafx.util.Pair, String> nameColumn = new TableColumn<>("Document name");
        nameColumn.setMinWidth(650);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("Key"));

        //Quantity column
        TableColumn<javafx.util.Pair, String> quantityColumn = new TableColumn<>("Score");
        quantityColumn.setMinWidth(200);
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("Value"));

        TableView<javafx.util.Pair> table = new TableView<>();
        table.setMinHeight(700);
        table.setMinWidth(850);
        table.setItems(castPairToObservableList(results));
        table.getColumns().addAll(nameColumn, quantityColumn);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(table);

        Scene scene = new Scene(vBox);
        Stage stage = new Stage();
        stage.setHeight(700);
        stage.setWidth(850);
        stage.setScene(scene);
        stage.show();

    }

    /**
     * return Integer between the nim value that given and the upper bound given.
     *
     * @param min Inter. lower bound.
     * @param max Inter. upper bound.
     *
     * @return Integer, between the given range.
     */
    private int generateNumberInRange(int min, int max) {

        int number = (int)(Math.random() * max);

        while(number < min) {
            number = (int)(Math.random() * max);
        }

        return number;
    }

    /**
     * the message re-initialize the searcher for a new query.
     */
    private void createAndUpdateSearcher() {

        loadDictionary();

        this.searcher = new Searcher(this.cb_stemm.isSelected(),
                new Ranker(),
                this.termDictionary,
                this.docDictionary,
                this.tf_corpus_dest.getText().replaceAll("\\\\", "/")

        );
    }

    /**
     * the method is browsing for a path where the query file is located.
     */
    public void browseQuery() {

        FileChooser fileChooser = new FileChooser();

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            tf_query_file.setText(file.toString());
        }
    }

    /**
     * the method open file dialog to the user for where to put
     * the qureies results.
     */
    public void openFileDialogForQueriesResults() {


        DirectoryChooser directoryChooser = new DirectoryChooser();

        File file = directoryChooser.showDialog(null);

        if (file != null) {
            tf_queries_dest.setText(file.toString());
        }
    }

    /**
     * the method responsible to run all the queries in the inserted query file.
     */
    public void runQueriesFile() {

        disableButtons();
        long start_time = System.currentTimeMillis();


        if(this.termDictionary == null || this.docDictionary == null) {
            showMessage("Couldn't find Dictionary, please make sure dictionary are loaded");
            enableButtons();

            return;
        }

        if(new File(this.tf_query_file.getText()).exists()) {
            showMessage("Couldn't find queries file, please make sure query file path is valid");
            enableButtons();

            return;
        }

        File queriesDestFile = new File(this.tf_queries_dest.getText().concat("/queries_result_b-" + Ranker.b + "_k-" + Ranker.k + "_HW-" + Ranker.headerWeight + "_ST-" + this.cb_semantic_treatment.isSelected() + ".txt"));

        if(!queriesDestFile.exists()) {
            try {
                queriesDestFile.createNewFile();
            }
            catch (IOException e) {
                showMessage("Couldn't create 'queries_result.txt' at " + queriesDestFile.getAbsolutePath());

                return;
            }
        }

        List<Query> queries = Query.parseQueries(this.tf_query_file.getText());

        //Check if StopWord Dictionary is loaded, if not, load
        ASearcher.loadStopWords(this.tf_corpus_src.getText().replaceAll("\\\\", "/").concat("/05 stop_words.txt"));

        createAndUpdateSearcher();

        for(Query query : queries) {

            List<javafx.util.Pair> results = this.searcher.search(query.getTitle()  /*+ query.getDescription() /* + " " + query.getNarattive()*/, cb_semantic_treatment.isSelected());

            writeQueryResultToTrecEvalFile(queriesDestFile, query, results);
        }

        long estimatedTime = System.currentTimeMillis() - start_time;

        System.out.println("timer: " + TimeUnit.SECONDS.convert(estimatedTime, TimeUnit.MILLISECONDS));

        enableButtons();
    }

    /**
     * the method writtes the queries result to a txt file by
     * the required structure.
     *
     * @param queriesDestFile - a path where to save the query results.
     * @param query - the inserted query.
     * @param results - the result retrieved to the query.
     */
    private void writeQueryResultToTrecEvalFile(File queriesDestFile, Query query, List<javafx.util.Pair> results) {

        try {
            FileWriter FW = new FileWriter(queriesDestFile, true); //Set true for append mode

            String preLine = ("" + query.getNumber()).concat("   0  ");
            String postLine = "  1   42.38   mt\n";

            for (javafx.util.Pair pair : results) {
                DocData docData = (DocData)pair.getKey();

                FW.write(preLine + docData.getDocName() + postLine);
            }

            FW.close();
        }
        catch (IOException e) {
            System.out.println("[Controller]: couldn't write queries result to file. Error:" + e.getStackTrace());
        }
    }
}
