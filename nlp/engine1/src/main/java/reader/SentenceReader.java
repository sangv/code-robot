package reader;

import domain.WordTag;

import java.io.IOException;
import java.util.List;

/**
 * @author Sang
 */

public interface SentenceReader {

    /**
     *
     * @param location
     * @return
     * @throws IOException
     */
    List<List<WordTag>> read(String location) throws IOException;
}
