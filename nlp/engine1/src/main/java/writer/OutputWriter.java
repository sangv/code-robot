package writer;

import java.io.IOException;
import java.util.List;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public interface OutputWriter {

  void write(String location, boolean append, List<String> strings) throws IOException;
}
