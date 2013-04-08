package pcfg.domain;

import domain.NGramTag;
import domain.Sentence;
import domain.Sentence.WordTag;
import domain.TagResults;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class CKYTagResults extends TagResults {

    Map<String,Integer> nonTerminalCountMap =  new LinkedHashMap<String, Integer>();

    Map<WordTag,Integer> unaryRuleCountMap =  new LinkedHashMap<WordTag, Integer>();

    Map<NGramTag,Integer> binaryRuleCountMap =  new LinkedHashMap<NGramTag, Integer>();

    public Map<String, Integer> getNonTerminalCountMap() {
        return nonTerminalCountMap;
    }

    public void setNonTerminalCountMap(Map<String, Integer> nonTerminalCountMap) {
        this.nonTerminalCountMap = nonTerminalCountMap;
    }

    public Map<WordTag, Integer> getUnaryRuleCountMap() {
        return unaryRuleCountMap;
    }

    public void setUnaryRuleCountMap(Map<Sentence.WordTag, Integer> unaryRuleCountMap) {
        this.unaryRuleCountMap = unaryRuleCountMap;
    }

    public Map<NGramTag, Integer> getBinaryRuleCountMap() {
        return binaryRuleCountMap;
    }

    public void setBinaryRuleCountMap(Map<NGramTag, Integer> binaryRuleCountMap) {
        this.binaryRuleCountMap = binaryRuleCountMap;
    }
}
