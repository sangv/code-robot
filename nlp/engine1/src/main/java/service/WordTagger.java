package service;

import domain.NGramTag;
import domain.Sentence;
import domain.TagResults;
import reader.SentenceReader;
import writer.OutputWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Sang Venkatraman
 */
public interface WordTagger {

    void init(String location) throws IOException;

    Map<NGramTag,Integer> getNGramCounts(int length);

    void invalidate();

    TagResults getTagResults();

    void setSentenceReader(SentenceReader sentenceReader);

    Map<Sentence.WordTag,Double> calculateExpectations(Map<String, Integer> tagMap, Map<Sentence.WordTag, Integer> taggedWords) throws IOException;

    void setOutputWriter(OutputWriter outputWriter);

    List<String> estimate(String testFileLocation, String outputFileLocation, Map<Sentence.WordTag, Integer> taggedWords, Map<Sentence.WordTag, Double> expectationMap) throws IOException;

    List<String> replaceLessFrequentWordTags(String outputFileLocation, TagResults tagResults) throws Exception;

    Map<String,Double> calculateQFunction(TagResults tagResults);

    List<String> getLowOccurenceWords(TagResults tagResults);

    List<String> estimateWithViterbi(String testFileLocation, String outputFileLocation, Map<String, Double> qFunction, Map<Sentence.WordTag, Double> expectationMap, TagResults tagResults) throws IOException;
}
