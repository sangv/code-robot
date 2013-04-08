package service;

import domain.NGramTag;
import domain.NGramTagResults;
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

    NGramTagResults getTagResults();

    void setSentenceReader(SentenceReader sentenceReader);

    Map<Sentence.WordTag,Double> calculateExpectations(Map<String, Integer> tagMap, Map<Sentence.WordTag, Integer> taggedWords) throws IOException;

    void setOutputWriter(OutputWriter outputWriter);

    List<String> estimate(String testFileLocation, String outputFileLocation, Map<Sentence.WordTag, Double> expectationMap, NGramTagResults tagResults, boolean useRareSubclasses) throws IOException;

    List<String> replaceLessFrequentWordTags(String outputFileLocation, NGramTagResults tagResults, boolean rareSubClasses) throws Exception;

    List<String> getLowOccurenceWords(NGramTagResults tagResults);
}
