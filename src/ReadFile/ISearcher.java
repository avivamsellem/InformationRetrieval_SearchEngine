package ReadFile;

import javafx.util.Pair;
import java.util.List;

public interface ISearcher
{

    List<Pair> search(String query, boolean semantic);


}
