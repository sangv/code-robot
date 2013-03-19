package writer;

import domain.WordTagCounts;

import java.io.IOException;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public interface OutputWriter {
    void write(String fileLocation, WordTagCounts wordTagCounts) throws IOException;
}
