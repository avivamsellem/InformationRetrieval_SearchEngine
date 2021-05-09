package ReadFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IRanker
{

    public Map<Integer, Double> rank(List<DocData> docNameDictionary, List<Object> relevantDocument, HashMap<Object, Object> termData);
}
