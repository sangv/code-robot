package writer;

import domain.WordTagCounts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class FileOutputWriter implements OutputWriter {

    @Override
    public void write(String fileLocation, WordTagCounts wordTagCounts) throws IOException {
        writeToFile(new File(fileLocation),false,null);
    }


    protected void writeToFile(File file, boolean append,String... strings) throws IOException {
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
