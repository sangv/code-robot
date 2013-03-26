package reader;

import domain.Sentence;

import java.io.IOException;
import java.util.List;

/**
 * This is used to read sentences for processing.
 *
 * @author Sang
 */

public interface SentenceReader {

    /**
     *
     * @param location
     * @return
     * @throws IOException
     */
    List<Sentence> read(String location) throws IOException;

    List<String> getContents(String fileLocation) throws IOException;

    List<List<String>> readSentences(String location) throws IOException;
}
