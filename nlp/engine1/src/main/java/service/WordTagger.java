package service;

import domain.NGramTag;
import domain.WordTagCounts;
import reader.SentenceReader;

import java.io.IOException;
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
}
