package ReadFile;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import org.tartarus.snowball.ext.PorterStemmer;


/**
 * Parser class used to parse and index one document or more.
 */
public class Parser implements IParser{

    //Initial variables
    private IndexerInterface indexer;
    private HashSet<String> stopWords;
    private Logger logger;
    private boolean stemmer;
    private boolean saveSingleWords;
    private Object stemmerMutex;

    private Map<String, String> calender;
    private PorterStemmer PS;

    /**
     * Constructor.
     *
     * @param indexer Indexer. will index the documents after parsing.
     * @param stopWordsPath String. Path to the corpus directory.
     * @param stemmerOn Boolean. true if stemming is needed, false otherwise.
     * @param identityAsSingle Boolean. true if needed to save single word in complex phrases.
     * @param logger Logger. log file to write progress through the running.
     */
    public Parser(IndexerInterface indexer, String stopWordsPath, boolean stemmerOn, boolean identityAsSingle, Logger logger) {
        logger.info("Parser - constructor");

        if (stopWordsPath == null) {
            throw new NullPointerException("The stop word cannot be NULL.");
        }

        this.indexer = indexer;

        try {
            this.stopWords = LoadFile(stopWordsPath);

            this.stopWords.add("--");
            this.stopWords.add(",");
            this.stopWords.add("");
        } catch (FileNotFoundException e) {
            logger.warning("coulnd't find specified stop word file.");
            this.stopWords = new HashSet<>();
        }

        this.stemmer = stemmerOn;
        this.stemmerMutex = new Object();
        this.saveSingleWords = identityAsSingle;
        this.logger = logger;
        this.PS = new PorterStemmer();

        //Initial calender dictionary
        this.calender = new HashMap<>();

        this.calender.put("January", "01");
        this.calender.put("JANUARY", "01");
        this.calender.put("JAN", "01");
        this.calender.put("Jan", "01");
        this.calender.put("February", "02");
        this.calender.put("FEBRUARY", "02");
        this.calender.put("FEB", "02");
        this.calender.put("Feb", "02");
        this.calender.put("March", "03");
        this.calender.put("MARCH", "03");
        this.calender.put("MAR", "03");
        this.calender.put("Mar", "03");
        this.calender.put("April", "04");
        this.calender.put("APRIL", "04");
        this.calender.put("APR", "04");
        this.calender.put("Apr", "04");
        this.calender.put("MAY", "05");
        this.calender.put("May", "05");
        this.calender.put("may", "05");
        this.calender.put("June", "06");
        this.calender.put("JUNE", "06");
        this.calender.put("JUN", "06");
        this.calender.put("Jun", "06");
        this.calender.put("July", "07");
        this.calender.put("JULY", "07");
        this.calender.put("JUL", "07");
        this.calender.put("Jul", "07");
        this.calender.put("August", "08");
        this.calender.put("AUGUST", "08");
        this.calender.put("AUG", "08");
        this.calender.put("Aug", "08");
        this.calender.put("SEPTEMBER", "09");
        this.calender.put("September", "09");
        this.calender.put("SEP", "09");
        this.calender.put("Sep", "09");
        this.calender.put("October", "10");
        this.calender.put("OCTOBER", "10");
        this.calender.put("OCT", "10");
        this.calender.put("Oct", "10");
        this.calender.put("November", "11");
        this.calender.put("NOVEMBER", "11");
        this.calender.put("NOV", "11");
        this.calender.put("Nov", "11");
        this.calender.put("December", "12");
        this.calender.put("DECEMBER", "12");
        this.calender.put("DEC", "12");
        this.calender.put("Dec", "12");
    }

    /**
     * Parse the given document text and header.
     * at the end of the parse it calling indexer object to save the document.
     *
     * @param doc Document. document object to parse.
     */
    public void parseDocuments(Document doc) {

        if (doc == null) {
            this.logger.warning("parseDocument throw NullPointerException because it got null as a document.");
            throw new NullPointerException();
        }

        this.logger.info("parseDocuments called to parse:" + doc.getDocName());

        HashMap<String, DocTerm> docTerms = new HashMap<>();
        parseText(docTerms, doc.getTitle(), true);
        parseDate(docTerms, doc.getDate());
        parseText(docTerms, doc.getText(), false);

        this.indexer.addDoc(docTerms, doc.getDocName());

        this.logger.info("parse " + doc.getDocName() + "done. HashMap was sent to indexer.");
    }

    /**
     * Parse a given string to date representation
     *
     * @param docTerms
     * @param date
     */
    private void parseDate(HashMap<String, DocTerm> docTerms, String date) {
        if(date == null) {
            return;
        }

        String[] splitDate = date.split(" ");

        try {
            String newDateFormatFullHyphen = splitDate[0].concat("-".concat(this.calender.get(splitDate[1]).concat("-".concat(splitDate[2]))));
            String newDateFormatMMMYYYY = splitDate[1].concat(" ".concat(splitDate[2]));
            String dateMMDD = this.calender.get(splitDate[1]).concat("-".concat(splitDate[0]));
            String dateYYYYMM = splitDate[2].concat("-".concat(this.calender.get(splitDate[1])));

            addTerm(docTerms, date, 0, false, true);
            addTerm(docTerms, newDateFormatFullHyphen, 0, false, true);
            addTerm(docTerms, newDateFormatMMMYYYY, 0, false, true);
            addTerm(docTerms, dateMMDD, 0, false, true);
            addTerm(docTerms, dateYYYYMM, 0, false, true);
        }
        catch (NullPointerException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
            addTerm(docTerms, date, 0, false, true);
        }
    }

    /**
     *  Parse given text to HashMap<String, DocTerm> object
     *
     * @param docTerms HashMap<String, DocTerm>. structure to add all the DocTerm.
     * @param text String. text to parse.
     * @param isHeader boolean, True if the text have more weight than regular text.
     */
    private void parseText(HashMap<String, DocTerm> docTerms, String text, boolean isHeader) {

        if (text == null) {
            return;
        }

        //break text to String Array
        String[] words = text.split("\\s+");
        String currentWord;

        for (int i = 0; i < words.length; i++) {

            currentWord = cleanWord(words[i]);

            //take care of special cases
            if (currentWord.equals("") || currentWord.charAt(0) == '<' || currentWord.contains("_")) {
                continue;
            }

            String newWord;

            //word
            if (!containsDigit(currentWord)) {
                currentWord = currentWord.replaceAll("[\\./\\\\]", "");
                i = handleWord(docTerms, words, currentWord, i, isHeader);
            }

            //number[,number]*[.number]?[m|bn]? | number% | $number | number / number
            else if (isNumberOrKnownSymbol(currentWord)) {

                if(currentWord.charAt(currentWord.length()-1) == '.') { //remove redundant dot

                    currentWord = currentWord.substring(0, currentWord.length()-1);
                }

                i = handleNumber(docTerms, words, currentWord.toUpperCase(), i, isHeader);

            } else if(currentWord.contains("-")){
                // .8-mm -> 0.8-mm    |  .75-mile-long  -> 0.75-mile-long
                if(currentWord.charAt(0) == '.' && currentWord.length() > 1) {
                    if (Character.isDigit(currentWord.charAt(1))) {
                        currentWord = "0".concat(currentWord);
                    } else if (currentWord.charAt(1) == '.') {
                        continue;
                    }
                }
                handleHyphen(docTerms, currentWord, i, isHeader);
            }

            else if(currentWord.contains("$")) {
                handleDollar(docTerms, currentWord, i, false, isHeader);
            }

            else if((newWord = checkNumberWithPostFix(currentWord)) != null) {
                addTerm(docTerms, newWord, i, false, isHeader);
            }
            else if(currentWord.charAt(currentWord.length() - 1) != '>') {
//                addTerm(docTerms, currentWord, i, false, isHeader);
            }
            else {

                this.logger.info("cannot map the current word:" + currentWord);
            }
        }
    }

    /**
     * Check special occasion of unique word combine with number
     *
     * @param currentWord String. word to check if is valid term.
     * @return True of the given string is valid, false otherwise.
     */
    private String checkNumberWithPostFix(String currentWord) {

        if(currentWord.charAt(currentWord.length()-1) == '.') { //remove redundant dot

            currentWord = currentWord.substring(0, currentWord.length() - 1);
        }

        String str = currentWord.replaceAll("\\d+[.\\d*]", "");

        ArrayList<String> postix = new ArrayList<String>() {{
            add("st");
            add("nd");
            add("rd");
            add("th");
            add("s");
            add("am");
            add("pm");
            add("mm");
        }};

        return postix.contains(str.toLowerCase()) ? currentWord.toLowerCase() : null;
    }

    /**
     * Check special occasion of number with dollar (sign or word).
     *
     * @param docTerms HashMap<String, DocTerm>.
     * @param currentWord
     * @param i
     * @param b
     * @param isHeader
     */
    private void handleDollar(HashMap<String, DocTerm> docTerms, String currentWord, int i, boolean b, boolean isHeader) {

        String num = currentWord.replaceAll("\\D+","");

        String numFixedRepresentation = changeToKMBRepresentation(num);

        addTerm(docTerms, numFixedRepresentation + " $", i, false,isHeader);
    }

    /**
     * This function get a WORD term represented by String, and handle
     * this string according the parse word rules.
     *
     * @param docTerms HashMap<String,DocTerm>. represent HashMap to append the final term to.
     * @param words String[]. represent all the text split into words.
     * @param currentWord String. current word to parse.
     * @param i Int. index of the word in the text.
     * @param isHeader Boolean. indicate if this word should be marked as important.
     * @return Int. the next word should be parse at word array.
     */
    private int handleNumber(HashMap<String, DocTerm> docTerms, String[] words, String currentWord, int i, boolean isHeader) {

        //Check if date
        if (representDay(currentWord) && (i + 1 < words.length - 1) && this.calender.containsKey(words[i + 1])) {

            addTerm(docTerms, transformDate(words[i + 1], currentWord), i, false, isHeader);
            i++;

            return i;
        }

        if (currentWord.length() == 1 && Character.isDigit(currentWord.charAt(0))) {
            addTerm(docTerms, currentWord, i, false,isHeader);

            return i;
        //length is 1 or containing two dollars.
        } else if(currentWord.length() == 1 || (1 < (currentWord.replaceAll("$", "").length() - currentWord.length()))) {

            return i;
        }

        int originalI = i;
        char symbol = '\u0000';
        char extention = '\u0000';

        int wordLength = currentWord.length();
        char lastChar = currentWord.charAt(wordLength - 1);

        //check if the number contain 'm' or 'bn'
        if (lastChar == '%') {

            symbol = '%';
            currentWord = currentWord.substring(0, wordLength - 1);


        } else if (lastChar == 'm') {

            extention = 'M';
            currentWord = currentWord.substring(0, currentWord.length() - 1);

        } else if ((currentWord.length() >= 3) && (currentWord.substring(wordLength - 2).toLowerCase().equals("bn"))) {

            extention = 'M';
            currentWord = currentWord.substring(0, wordLength - 2) + "000"; //Just found that is bn so translate bm to m 3 zeros
        }

        if (currentWord.charAt(0) == '$') {

            symbol = '$';
            currentWord = currentWord.substring(1);
        }

        if ((i + 1 < words.length - 1) && containFraction(words[i + 1])) {

            //The next ord is fraction
            currentWord = currentWord.concat(" " + words[i+1]); // attach the fraction to th current number ith space
            i++;
        }

        //Fix number representation
        if (i + 1 < words.length - 1) {

            String nextWord = words[i + 1].toLowerCase();

            if (nextWord.equals("million") || nextWord.equals("m")) {

                extention = 'M';
                i++;

            } else if (nextWord.equals("billion") || words[i + 1].toLowerCase().equals("bn")) {

                extention = 'M';
                currentWord = currentWord.concat("000");
                i++;

            } else if (nextWord.equals("trillion")) {

                extention = 'M';
                currentWord = currentWord.concat("000000");
                i++;

            } else if (nextWord.equals("percent") || nextWord.equals("percentage")) {

                symbol = '%';
                i++;
            }

            //Dollar
            if (words[i + 1].toLowerCase().equals("u.s") && words[i + 2].toLowerCase().equals("dollars")) {
                symbol = '$';
                i += 2;
            }
        }

        //Extensions
        currentWord = changeToMilionRepresentation(currentWord);

        if (extention == '\u0000' && symbol == '\u0000') {
            //there is nothing to add to the number
        }
        else if(symbol == '$') {
            if(extention != '\u0000') {
                currentWord = currentWord.concat(" " + extention);
            }
            currentWord = currentWord.concat(" Dollars");

        }
        else if(symbol == '%'){

            if(extention != '\u0000') {
                currentWord = currentWord.concat(" " + extention);
            }
            currentWord = currentWord.concat(" %");
        }

        //Add term
        addTerm(docTerms, currentWord, originalI, false, isHeader);

        return i;

    }

    /**
     * Clean a Sting represent a word from 'noise' (redundant symbols).
     *
     * @param str Sting. word representation.
     * @return String. clean word.
     */
    private String cleanWord(String str) {
        return str.replaceAll("[?:;,.!#'`*\"(|){}\\[\\]]", "");
    }

    /**
     * Handle Word, parse it according to the parse word laws.
     *
     * @param docTerms    HashMap. to add the the term to.
     * @param words       String[]. containing all the split text.
     * @param currentWord String. current word to check.
     * @param i           int. index of the current word.
     * @param isHeader    boolean. indicade if the current text is in header.
     * @return next word index.
     */
    private int handleWord(HashMap<String, DocTerm> docTerms, String[] words, String currentWord, int i, boolean isHeader) { //This is a word

        if (currentWord.contains("-")) {
            if(currentWord.contains("--")) {
                return i;
            }

            return handleHyphen(docTerms, currentWord, i, isHeader); // step-by-step

        } else if (this.calender.containsKey(currentWord) && (i + 1 < words.length - 1) && isPositiveNumber(words[i + 1])) {
            addTerm(docTerms, transformDate(currentWord, words[i + 1]), i, false, isHeader);
            i++;

            return i;

        } else if (currentWord.length() > 1) {

            //Between NUMBER-NUMBER
            if (currentWord.toLowerCase().equals("between") && (i + 3 < words.length - 1)) {
                if(handleBetween(docTerms, words, currentWord, i, isHeader)) {
                    //increase i because 'between num - num' was found
                    i +=3;

                    return i;
                }
            }

            //Check if the first character is Upper
            if (Character.isUpperCase(currentWord.charAt(0))) {

                return handleUpperCase(docTerms, words, i, currentWord.toUpperCase(), isHeader);

            }
            else if(currentWord.charAt(0) == '&') {
                return i;
            }
            else {
                addTerm(docTerms, currentWord.toLowerCase(), i, this.stemmer, isHeader);

                return i;
            }
        }

        return i;
    }

    /**
     * Handle Upper Case words, parse it according to the parse word laws.
     *
     * @param docTerms    HashMap. to add the the term to.
     * @param words       String[]. containing all the split text.
     * @param i           int. index of the current word.
     * @param currentWord String. current word to check.
     * @param isHeader    boolean. indicade if the current text is in header.
     * @return
     */
    private int handleUpperCase(HashMap<String,DocTerm> docTerms, String[] words, int i, String currentWord, boolean isHeader) {

        //Make Upper Case
        currentWord = currentWord.toUpperCase();
        String cache = currentWord;
        int firstIndex = i;

        // check if there is a next word
        if (i + 1 < words.length) {

            boolean dot = false;
            String nextWord = cleanWord(words[i + 1]);
            while (!dot && nextWord.length() >= 1 && Character.isUpperCase(nextWord.charAt(0)) && !this.calender.containsKey(nextWord)) {

                dot = words[i + 1].contains(".");
                cache = cache.concat(" " + nextWord.toUpperCase());
                i++;

                if (i + 1 < words.length) {

                    nextWord = cleanWord(words[i + 1]);

                } else {
                    break;
                }
            }
        }

        //If NOT one word and it stop-word save it
        if ((i - firstIndex) != 0) {

            addTerm(docTerms, cache, firstIndex, false);

            //The identity is bigger then one Word. if it is'nt stop-word add it as well
            if (this.saveSingleWords) {
                for (int j = firstIndex; j <= i; j++) {
                    String subWord = cleanWord(words[j]);
                    if (!this.stopWords.contains(subWord.toLowerCase())) {

                        addTerm(docTerms, words[j].toUpperCase(), j, this.stemmer, isHeader); //Adding all the sub-word of the phrase without stemming
                    }
                }
            }

            return i;
        } else {
            addTerm(docTerms, currentWord, i, this.stemmer, isHeader);

            return i;
        }
    }

    /**
     * Handle between Case words, parse it according to the parse word laws.
     *
     * @param docTerms    HashMap. to add the the term to.
     * @param words       String[]. containing all the split text.
     * @param currentWord String. current word to check.
     * @param i           int. index of the current word.
     * @param isHeader    boolean. indicade if the current text is in header.
     * @return
     */
    private boolean handleBetween(HashMap<String, DocTerm> docTerms, String[] words, String currentWord, int i, boolean isHeader) {

        String rangeFrom = changeToKMBRepresentation(words[i + 1]);

        if (rangeFrom != null) {

            if (words[i + 2].toLowerCase().equals("and")) {

                String rangeUntil = changeToKMBRepresentation(words[i + 3]);

                if (rangeUntil != null) {
                    //TEMPLATE 'between NUM - NUM'
                    addTerm(docTerms, "between " + rangeFrom + " and " + rangeUntil, i, false);

                    if (this.saveSingleWords) {
                        addTerm(docTerms, rangeFrom, i + 1, false);
                        addTerm(docTerms, rangeUntil, i + 3, false);

                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param docTerms
     * @param currentWord
     * @param i
     * @param isHeader
     * @return
     */
    private int handleHyphen(HashMap<String,DocTerm> docTerms, String currentWord, int i, boolean isHeader) {

        String newHyphenTerm;

        String[] breakIdentity = currentWord.split("-");

        //2-term case
        if(breakIdentity.length >= 2) {

            String str = "";
            boolean capital = false;
            char firstChar = currentWord.charAt(0);

            if(Character.isDigit(firstChar) || Character.isUpperCase(firstChar)) {
                capital = true;
            }

            for(int j = 0 ; j < breakIdentity.length ; j++) {

                try {
                    Float partOne = Float.parseFloat(breakIdentity[j]);

                    newHyphenTerm = changeToKMBRepresentation(partOne.toString()).concat("-");

                    if(Character.isUpperCase(newHyphenTerm.charAt(newHyphenTerm.length()-1))) {
                        capital = true;
                    }

                } catch (NumberFormatException e) {
                    newHyphenTerm = breakIdentity[j].concat("-");

                } catch (NullPointerException e) {
                    newHyphenTerm = breakIdentity[j].concat("-");
                }

                str = str.concat(newHyphenTerm);
            }

            if(capital) {
                str = str.toUpperCase();
            }
            else {
                str = str.toLowerCase();
            }

            addTerm(docTerms, str.substring(0, str.length()-1), i, false, isHeader); //Add phrase
        }
        else

        //if config save single word
        if (this.saveSingleWords) { //Check if need to save sub-phrase terms

            for (int j = 0; j < breakIdentity.length; j++) {

                String subTerm = cleanWord(breakIdentity[j]);
                String subTermAsNum = changeToKMBRepresentation(subTerm);

                if(subTermAsNum != null) {
                    addTerm(docTerms, subTermAsNum, i, false, isHeader);
                }
                else {
                    addTerm(docTerms, subTerm, i, this.stemmer, isHeader);
                }
            }
        }

        return i;
    }

    /**
     * Check if the given string is positive number.
     * @param str String. may represent a number.
     * @return true if the given string is a number false otherwise.
     */
    private boolean isPositiveNumber(String str) {
        try {
            int num = Integer.parseInt(str);

            return num > 0;

        } catch(NullPointerException e){
            return false;

        } catch (NumberFormatException e){
            return false;
        }
    }

    /**
     * Convert, if needed, the given string into a K, M, B representation.
     * 100 -> 100, 1234 -> 1.234 K, 1100000 -> 1.1 M, 1003000000 -> 1.003 B.
     * @param str String. representation the number to convert.
     * @return String. if needed, convert String accorfing to the format needed.
     */
    private String changeToKMBRepresentation(String str) {
        try {
            Float num = Float.parseFloat(str.replace("," ,""));

            if(num < 0) {
                return "-" + changeToKMBRepresentation(str.substring(1));
            }
//            str = str.contains(".") ? str.replaceAll("0*$","").replaceAll("\\.$","") : str;
            if(num < 1000) {
                return str;

            } else if(num < 1000000 && num >= 1000) {
                str = String.format("%.3f", num / 1000);
                String sa = str.contains(".") ? str.replaceAll("0*$","").replaceAll("\\.$","") : str;
                return sa + "K";

            } else if(num >= 1000000 && num < 1000000000) {
                return String.format("%.3fM", num / 1000000);

            } else if(num >= 1000000000) {
                return String.format("%.3fB", num / 1000000000);

            } else {
                logger.warning("changeToKMBRepresentation function couldn't parse the given number to one of the categories: " + str);
            }
        } catch (NullPointerException e) {
            return null;

        } catch (NumberFormatException e) {
            return null;
        }

        return str;
    }

    /**
     * Convert the given string to "number.XXX M" representation.
     * @param str String. check if need to convert the number to million.
     * @return string million representation if biger then million if not return thr same string it got.
     */
    private String changeToMilionRepresentation(String str) {
        try {
            Float num = Float.parseFloat(str);

            if(num >= 1000000) {
                return String.format("%.3f M", num / 1000000);
            }
        }
        catch (NullPointerException e) {}
        catch (NumberFormatException e) {}

        return str;
    }

    /**
     * This function get a string and check if it representing a fraction.
     *
     * https://stackoverflow.com/questions/51627511/checking-if-a-string-has-a-fraction-part-in-java-locale-involved
     *
     * @param str String. or to check if it is a fraction.
     * @return true if the given string represent fraction, false otherwise.
     */
    public boolean containFraction(String str) {

        return Pattern.compile("\\d+(,|\\.)\\d*/\\d+(,|\\.)\\d").matcher(str).matches();
    }

    /**
     * This function get String and check if it contain only numbers and known symbols ('%', '$', '.', ',')
     * number[,number]*[.number]?[m|bn]? | number% | $number | number / number
     *
     * @param str String. word to check if it contain some chars that are not numbers or known symbols.
     * @return
     */
    private boolean isNumberOrKnownSymbol(String str) {
        if(str == null) {
            return false;
        }
        ArrayList<Character> midSymbols = new ArrayList<Character>() {{
           add(',');
           add('.');
           add('/');
        }};

        ArrayList<Character> postSymbols = new ArrayList<Character>() {{
            add('%');
            add('m');
            add('b');
            add('M');
            add('B');
            add('.');
        }};

        char[] chars = str.toCharArray();

        if(str.length() == 1 && !Character.isDigit(chars[0])) {
            return false;
        }

        for (int i = 0 ; i < str.length() ; i++) {
            if (!Character.isDigit(chars[i])){
                if(i == (str.length()-1)){
                    if(!postSymbols.contains(chars[i])){
                        return false;
                    }
                }
                //only dollar allowed at the end
                else if(i == 0 && !(chars[i] =='$' || chars[i] =='-')) {
                    return false;
                }
                //unusual place for special symbol only /,.
                else if(!midSymbols.contains(chars[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Wrap function get a HashMap<DocTerm>, string and location.
     * if the string is already in the HashMap it raise his amount and
     * append his location, if not. it creating a new DocTerm and add
     * it the the HashMap.
     *
     * Use this function when header is off.
     *
     * @param docTerms HashMap<DocTerm>. collection to check if given string contain in it.
     * @param string word to insert to the collection.
     * @param location loccation of the word in the text.
     * @param toStem Boolean. true if stem is needed, false otherwise.
     */
    private void addTerm(HashMap<String, DocTerm> docTerms, String string, int location, boolean toStem) {
        addTerm(docTerms, string, location,false, toStem);
    }

    /**
     * This function get a HashMap<DocTerm>, string and location.
     * if the string is already in the HashMap it raise his amount and
     * append his location, if not. it creating a new DocTerm and add
     * it the the HashMap.
     * @param docTerms HashMap<DocTerm>. collection to check if given string contain in it.
     * @param string word to insert to the collection.
     * @param location loccation of the word in the text.
     * @param isHeader Boolean. true if the word is in header, false otherwise.
     * @param toStem Boolean. true if stem is needed, false otherwise.
     */
    private void addTerm(HashMap<String, DocTerm> docTerms, String string, int location, boolean toStem, boolean isHeader) {

        string = cleanWord(string);
        if(this.stopWords.contains(string.toLowerCase()))
        {
            return;
        }

        if(toStem) {

            synchronized (stemmerMutex) {
                this.PS.setCurrent(string);
                this.PS.stem();
                string = this.PS.getCurrent();
            }

            if(string.length() == 1) {
                return;
            }

        }

        if (docTerms.containsKey(string)) {
            docTerms.get(string).add(location);
            docTerms.get(string).updateHeader(isHeader);

        } else {
            docTerms.put(string, new DocTerm(string, location, isHeader));
        }
    }

    /**
     * This function get a date as a string and change the representation
     * of this date according to the template.
     * @param month String. representing a month.
     * @param number String. number representing the year or the day.
     * @return String. date according to template.
     */
    private String transformDate(String month, String number) {

        int num = Integer.parseInt(number);

        if(num <= 31) {

            //This is Day
            if(num <= 9) {
                return this.calender.get(month).concat("-0" + num);
            } else {
                return this.calender.get(month).concat("-" + num);
            }
        }

        //this num represent year
        return number.concat(this.calender.get(month));
    }

    /**
     * check if the given string represent a positive number(integer).
     *
     * https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
     *
     * @param str string to check if it parsable to positive integer.
     * @return true if str represent number, false otherwise.
     */
    private boolean representDay(String str) {
        try {
            int day = Integer.parseInt(str);

            return (day >= 1 && day <= 31) ? true : false;

        } catch(NumberFormatException e){
            return false;
        }
    }

    /**
     * This function check id the given string contain any number.
     *
     * https://stackoverflow.com/questions/18590901/check-if-a-string-contains-numbers-java
     *
     * @param str String.
     * @return Boolean. True if there is a number and false otherwise
     */
    private final boolean containsDigit(String str) {

        return str.replaceAll("\\D+", "").length() > 0;
    }

    /**
     * Load the file at the given path to a HashSet.
     * @param path
     * @throws FileNotFoundException Throw exception if the given path doesnt contain file.
     */
    private HashSet<String> LoadFile(String path) throws FileNotFoundException {

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
