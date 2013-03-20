package reader;

import domain.TaggedSentence;

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
    List<TaggedSentence> read(String location) throws IOException;
}
