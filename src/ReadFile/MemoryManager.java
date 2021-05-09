package ReadFile;

import java.util.Set;

public interface MemoryManager {

    void addFileToMemory(String first2Letters, PostFile postFile);

    PostFile getFileFromMemory(String first2Letters);

    Set<String> getAllFilesNamesInMemory();

}
