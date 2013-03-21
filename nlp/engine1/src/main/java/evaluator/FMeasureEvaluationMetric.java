package evaluator;

import domain.TaggedSentence;
import domain.TaggedSentence.WordTag;
import org.slf4j.Logger;
import reader.FileBasedSentenceReader;
import reader.SentenceReader;
import writer.FileOutputWriter;
import writer.OutputWriter;

import java.io.IOException;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class FMeasureEvaluationMetric  {

        private static final Logger LOG = getLogger(FMeasureEvaluationMetric.class);

        private OutputWriter outputWriter = new FileOutputWriter();

        private SentenceReader sentenceReader = new FileBasedSentenceReader();

        public Double calculateMetric(String actualFileLocation, String expectedFileLocation, String positiveOutcome) throws IOException {

            List<TaggedSentence> actualSentences = sentenceReader.read(actualFileLocation);
            List<TaggedSentence> expectedSentences = sentenceReader.read(expectedFileLocation);

            if(actualSentences.size() != actualSentences.size()){
                throw new RuntimeException("The content length of expected and actual word tags need to match.");
            }

            double TP=0.0;
            double FP=0.0;
            double FN=0.0;
            double TN=0.0;

            for(int i=0; i< actualSentences.size(); i++){

                WordTag[] actualContents = actualSentences.get(i).getWordTags().toArray(new WordTag[]{});
                WordTag[] expectedContents = expectedSentences.get(i).getWordTags().toArray(new WordTag[]{});

                for(int j=0; j<actualContents.length; j++) {
                    if(positiveOutcome.equals(expectedContents[j].getTag()) && positiveOutcome.equals(actualContents[j].getTag())){
                        TP++;
                    }else if(!positiveOutcome.equals(expectedContents[j].getTag()) && !positiveOutcome.equals(actualContents[j].getTag())){
                        TN++;
                    }else if(positiveOutcome.equals(expectedContents[j].getTag()) && !positiveOutcome.equals(actualContents[j].getTag())){
                        FN++;
                    }else if(!positiveOutcome.equals(expectedContents[j].getTag()) && positiveOutcome.equals(actualContents[j].getTag())){
                        FP++;
                    }
                }
            }

            double precision = (double)(TP/(TP+FP));
            double recall = (double)(TP/(TP+FN));

            double fmeasure = (2*precision*recall)/(precision+recall);

            StringBuilder fMeasureString = new StringBuilder();
            fMeasureString.append("Matrix:[TP  FN; FP   TN]").append("\n");
            fMeasureString.append("Matrix:["+TP+" "+FN+";"+FP+" "+TN+"]").append("\n");
            fMeasureString.append("precision:"+precision).append("\n");
            fMeasureString.append("recall:"+recall).append("\n");
            fMeasureString.append("fmeasure: "+fmeasure);

            LOG.info(fMeasureString.toString());

            return fmeasure;
        }



}
