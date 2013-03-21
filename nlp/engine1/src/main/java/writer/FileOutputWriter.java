package writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class FileOutputWriter implements OutputWriter {

    @Override
    public void write(String location, boolean append, List<String> strings) throws IOException {
        File file = new File(location);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new java.io.FileWriter(file, append));
            for (String string : strings) {
                bufferedWriter.write(string);
                bufferedWriter.newLine();
            }
        } finally {
            bufferedWriter.flush();
            bufferedWriter.close();
        }
    }
}
