package pcfg.service;

import domain.DynamicProgrammingResults;
import domain.NGramTag;
import domain.Sentence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class CKYEstimator {

    public void calculateY1Y2GivenX(Map<String,Integer> nonTerminalTagCounts, Map<NGramTag,Integer> binaryRuleCounts, Map<String,Map<String,Double>> qFunctionMap){

        for (Map.Entry<NGramTag,Integer> entry: binaryRuleCounts.entrySet()){
            NGramTag binaryRuleTag = entry.getKey();
            int numerator = entry.getValue();
            int denominator = nonTerminalTagCounts.containsKey(binaryRuleTag.getTag()) ? nonTerminalTagCounts.get(binaryRuleTag.getTag()) : 0;
            double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
            if(qFunction > 0) {
                if(qFunctionMap.containsKey(binaryRuleTag.getTag())) {
                    qFunctionMap.get(binaryRuleTag.getTag()).put(binaryRuleTag.getOthers()[0] + "And" + binaryRuleTag.getOthers()[1],qFunction);
                } else {
                    Map<String,Double> newMap = new LinkedHashMap<String,Double>();
                    newMap.put(binaryRuleTag.getOthers()[0] + "And" + binaryRuleTag.getOthers()[1],qFunction);
                    qFunctionMap.put(binaryRuleTag.getTag(),newMap);
                }
            }
        }
    }

    public void calculateWordGivenX(Map<String,Integer> nonTerminalTagCounts, Map<Sentence.WordTag,Integer> unaryRuleCounts, Map<String,Map<String,Double>> qFunctionMap){

        for (Map.Entry<Sentence.WordTag,Integer> entry: unaryRuleCounts.entrySet()){
            Sentence.WordTag wordTag = entry.getKey();
            int numerator = entry.getValue();
            int denominator = nonTerminalTagCounts.containsKey(wordTag.getTag()) ? nonTerminalTagCounts.get(wordTag.getTag()) : 0;
            double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
            if(qFunction > 0) {
                if(qFunctionMap.containsKey(wordTag.getTag())) {
                    qFunctionMap.get(wordTag.getTag()).put(wordTag.getWord(),qFunction);
                } else {
                    Map<String,Double> newMap = new LinkedHashMap<String,Double>();
                    newMap.put(wordTag.getWord(),qFunction);
                    qFunctionMap.put(wordTag.getTag(),newMap);
                }
            }
        }
    }

    public String calculateFinalString(DynamicProgrammingResults dynamicProgrammingResults, String[] words, List<String> tags){
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();

        piMap.get("pi(1,8,S)");
        double currentMax = 0;
        for(int i = 1; i <= words.length; i++){
           for(String tag:tags){
               //FIXME
           }
        }
        return null;
    }

    public DynamicProgrammingResults calculatePiMap(String[] words, List<String> tags, Map<String,Map<String,Double>> qFunctionY1Y2GivenX, Map<String,Map<String,Double>> qFunctionWordGivenX){

        DynamicProgrammingResults dynamicProgrammingResults = new DynamicProgrammingResults();
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();


        for(int i=1; i <= words.length; i++){
            for(String tag: tags){
                String X = tag;
                String WORD = words[i-1];
                double qValue = 0;
                Map<String,Double> emissionMap = qFunctionWordGivenX.containsKey(X) ? qFunctionWordGivenX.get(X) : null;
                if(emissionMap != null){
                    qValue = emissionMap.containsKey(WORD) ? emissionMap.get(WORD) : 0.0;
                }
                if(qValue > 0){
                    piMap.put("pi("+i+","+i+","+X+")",qValue);
                }
            }
        }
        for(int l = 0; l <words.length; l++){
            calculatePiMapAtEachLevel(words, tags, qFunctionY1Y2GivenX, qFunctionWordGivenX, piMap, maxBackPointerMap,l);
        }
        return dynamicProgrammingResults;
    }

    protected void calculatePiMapAtEachLevel(String[] words, List<String> tags, Map<String,Map<String,Double>> qFunctionY1Y2GivenX, Map<String,Map<String,Double>> qFunctionWordGivenX,Map<String, Double> piMap,Map<String, String> maxBackPointerMap, int l){
        for(int i=1; i <= words.length-l; i++){
                int j=i+l;
                double currentMax = 0.0F;
                for(int s=1; s<=words.length; s++){
                    for(String tag: tags){
                        String X = tag;
                        if(qFunctionY1Y2GivenX.containsKey(X)){
                            Map<String,Double> xEntries = qFunctionY1Y2GivenX.get(X);
                            for(Map.Entry<String,Double> xEntry:xEntries.entrySet()){
                                String split[] = xEntry.getKey().split("_AND_");
                                String Y = split[0];
                                String Z = split[1];
                                String key = "pi("+i+","+j+","+X+")";

                                Double pivalue1 = piMap.containsKey("pi("+ i +","+s+"," + Y+ ")") ? piMap.get("pi("+ i +","+s+"," + Y+ ")") : 0.0F;
                                Double pivalue2 = piMap.containsKey("pi("+ new Integer(s+1) +","+j+"," + Z+ ")") ? piMap.get("pi("+ new Integer(s+1) +","+j+"," + Z+ ")") : 0.0F;
                                Double pivalue =  xEntry.getValue()*pivalue1*pivalue2;

                                if(pivalue > currentMax){
                                    currentMax = pivalue;
                                    piMap.put(key,pivalue);
                                    maxBackPointerMap.put(i+","+j+","+X,s+"_"+Y+"_"+Z);
                                }
                            }
                        }

                    }
                }
            }
        }

}
