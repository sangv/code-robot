package service;

import domain.NGramTag;
import domain.TaggedSentence;
import domain.WordTagCounts;
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

    WordTagCounts getWordTagCounts();

    void setSentenceReader(SentenceReader sentenceReader);

    Map<TaggedSentence.WordTag,Float> calculateExpectations(Map<String, Integer> tagMap, Map<TaggedSentence.WordTag, Integer> taggedWords) throws IOException;

    void setOutputWriter(OutputWriter outputWriter);

    List<String> estimate(String testFileLocation, String outputFileLocation, Map<TaggedSentence.WordTag, Integer> taggedWords, Map<TaggedSentence.WordTag, Float> expectationMap) throws IOException;

    List<String> replaceLessFrequentWordTags(String outputFileLocation, Map<TaggedSentence.WordTag,Integer> taggedWords) throws Exception;
}
