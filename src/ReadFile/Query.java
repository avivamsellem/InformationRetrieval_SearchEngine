package ReadFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * the class represent a single query from the the query files.
 */
public class Query
{

    private int queryNumber;
    private String title;
    private String description;
    private String narattive;


    /**
     * constructor.
     * @param queryNumber int - represent the query number.
     * @param title - String - represent the title of the query.
     * @param description String - represent the description of the query.
     * @param narattive String - represent the narrative of the query.
     */
    public Query(int queryNumber,String title, String description, String narattive)
    {
        this.queryNumber = queryNumber;
        this.title = title;
        this.description = description;
        this.narattive = narattive;
    }

    /**
     * getter method
     * @return int - the query number.
     */
    public int getNumber() {
        return queryNumber;
    }

    /**
     * getter method
     * @return String - title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * getter method
     * @return String - description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * getter method
     * @return String - narrative.
     */
    public String getNarattive() {
        return narattive;
    }


    /**
     * Static method - the method receive a path to the query and return list of Query of all
     * the queries in the file.
     * @param queriesPath - String - path of the queries files.
     * @return List<Query> - a list of the all the queries in the given file queries.
     */
    public static List<Query> parseQueries(String queriesPath)

    {
        List<Query> allQueries = new ArrayList<>();

        File file = new File(queriesPath);

        if(!file.exists())
        {
            System.out.println("couldn't find quries path: " + queriesPath);
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";

            while (line != null)
            {
                int queryNumber;
                String title = "";
                String description = "";
                String narrative = "";

                if(line.equals("<top>")) //start of new Query
                {

                    while(!line.contains("<num>")) //look for query number
                    {
                        line = br.readLine();
                    }

                    queryNumber = getQueryNumber(line);

                    while(!line.contains("<title>"))
                    {
                        line = br.readLine();
                    }

                    title = getQueryTitle(line);

                    description = getQueryDecsription(line, br);
                    narrative =  getQueryNarrative(line, br);

                    //title = title.replace('-',' ');
                    //                    System.out.println("num:\n" + queryNumber +"\n");
//                    System.out.println("title:\n" + title +"\n");
//                    System.out.println("description:\n" + description +"\n");
//                    System.out.println("narrative:\n" + narrative +"\n");

                    allQueries.add(new Query(queryNumber, title.trim().toLowerCase(), description.trim().toLowerCase(), narrative.trim().toLowerCase()));
                }
                else
                {
                    line = br.readLine();
                }

            }
            br.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return allQueries;
    }

    /**
     * the method parse the query title.
     * @param line String - current line in the BufferedReader
     * @return String - the text of the title.
     */
    private static String getQueryTitle(String line)
    {
        //String title = line.substring(line.indexOf(' '), line.length());
        String title = line.substring(line.indexOf('>') + 1, line.length());
        title = title.trim();
        return title;
    }

    /**
     * the method parse the query number.
     * @param line String - current line in the BufferedReader
     * @return String - the number of the query.
     */
    private static int getQueryNumber(String line)
    {
        String queryNumberByString = line.substring(14,line.length());
        if(queryNumberByString.contains(" "))
        {
            queryNumberByString = queryNumberByString.substring(0, queryNumberByString.length() - 1);
        }
        return Integer.parseInt(queryNumberByString);
    }

    /**
     * the method parse the query description.
     * @param line String - current line in the BufferedReader
     * @return String - the description of the title.
     */
    private static String getQueryDecsription(String line, BufferedReader br)
    {
        String text = "";
        try {
            while (!line.contains("<desc>")) //look for query description
            {
                line = br.readLine();
            }

            line = br.readLine();
            text = line;
            line = br.readLine();


            while (!line.equals("") && !line.contains("<narr>") && line.length() != 1 && line.length() != 2 )
            {
                text += line;
                line = br.readLine();
            }
        }
        catch(IOException e)
        {

            e.printStackTrace();
        }

        return text;
    }

    /**
     * the method parse the query narrative.
     * @param line String - current line in the BufferedReader
     * @return String - the text of the narrative.
     */
    private static String getQueryNarrative(String line, BufferedReader br)
    {
        String narrative = "";
        try {
            while (!line.contains("<narr>")) //look for query narrative
            {
                line = br.readLine();
            }

            line = br.readLine();
            narrative = line;
            line = br.readLine();

            while (!line.equals("</top>"))
            {
                narrative += line;
                line = br.readLine();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        return narrative;
    }


}
