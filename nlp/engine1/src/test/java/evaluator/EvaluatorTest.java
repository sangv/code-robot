package evaluator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reader.FileBasedSentenceReader;
import writer.FileOutputWriter;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class EvaluatorTest {

    private FMeasureEvaluationMetric evaluator;

    @Before
    public void setUp(){
        evaluator = new FMeasureEvaluationMetric();
        evaluator.setOutputWriter(new FileOutputWriter());
        evaluator.setSentenceReader(new FileBasedSentenceReader());
    }

    @Test
    @Ignore
    public void evaluate() throws IOException {
        assertEquals(1.0D, evaluator.calculateMetric("src/test/resources/gene.key.copy", "src/test/resources/gene.key", "I-GENE"));
        assertEquals(0.5581936685288642,evaluator.calculateMetric("src/test/resources/gene_dev.p1.fullcount.out","src/test/resources/gene.key","I-GENE"));
    }
}
