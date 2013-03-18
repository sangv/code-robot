package reader;

import domain.WordTag;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sang
 * Date: 3/18/13
 * Time: 4:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SentenceReader {
    List<List<WordTag>> read(String location) throws IOException;
}
