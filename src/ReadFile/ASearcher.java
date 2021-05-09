package ReadFile;

import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;


public abstract class ASearcher implements ISearcher{

    static HashSet<String> stopWords = null;

    public static final int semanticRangeSize = 2;

    static Searcher semanticSearcher = LoadSemanticModel();

    private static Searcher LoadSemanticModel() {

        try {
            Word2VecModel word2VecModel = Word2VecModel.fromTextFile(new File("word2vec.c.output.model.txt"));

            return word2VecModel.forSearch();
        }
        catch(IOException e) {
            System.out.println("[ASearcher]: Failed to load semantic model");
        }

        return null;
    }

    static void loadStopWords(String stopWordsPath) {

        if(ASearcher.stopWords == null) {
            try {
                ASearcher.stopWords = loadFile(stopWordsPath);

                ASearcher.stopWords.add("--");
                ASearcher.stopWords.add(",");
                ASearcher.stopWords.add("");
                ASearcher.stopWords.add("?");

            } catch (FileNotFoundException e) {
                System.out.println("[Searcher] coulnd't find specified stop word file.");
                ASearcher.stopWords = new HashSet<>();
            }
        }
    }

    /**
     * Load the file at the given path to a HashSet.
     * @param path
     * @throws FileNotFoundException Throw exception if the given path doesnt contain file.
     */
    private static HashSet<String> loadFile(String path) throws FileNotFoundException {

        HashSet<String> hashSet = new HashSet<>();

        File file = new File(path);

        if(file.exists())
        {
            Scanner iterator = new Scanner(file);
            String line;

            while (iterator.hasNext()) {

                line = iterator.nextLine();
                hashSet.add(line);
            }

            return hashSet;
        }

        throw new FileNotFoundException();
    }
}
