package service;

import domain.DynamicProgrammingResults;
import domain.NGramTag;
import domain.NGramTagResults;
import domain.Sentence.WordTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Sang Venkatraman
 */
public class ViterbiAlgorithmWordTagger extends AbstractWordTagger{

    private static final Logger LOG = LoggerFactory.getLogger(ViterbiAlgorithmWordTagger.class);


    @Override
    public List<String> estimate(String testFileLocation, String outputFileLocation, Map<WordTag, Double> expectationMap, NGramTagResults tagResults, boolean useRareSubclasses) throws IOException {

        Map<String,Double> qFunction = calculateQFunction(tagResults);

        List<String> estimatedWords = new ArrayList<String>();

        List<List<String>> sentences = getSentenceReader().readSentences(testFileLocation);
        for (List<String> sentence : sentences){
              String[] words = sentence.toArray(new String[]{});
              estimatedWords.addAll(calculateViterbiEstimates(words,qFunction,expectationMap,tagResults,useRareSubclasses));
              estimatedWords.add("");
        }


        getOutputWriter().write(outputFileLocation, false, estimatedWords);
        return estimatedWords;
    }

    protected Map<String,Double> calculateQFunction(NGramTagResults tagResults){
        Map<NGramTag,Integer> trigramCounts = tagResults.getTrigramTagCountMap();
        Map<NGramTag,Integer> bigramCounts = tagResults.getBigramTagCountMap();
        Map<String,Double> qFunctionResults = new LinkedHashMap<String,Double>();
        calculateQFunction(new String[]{"I-GENE","O","*"},bigramCounts,trigramCounts,qFunctionResults);
        calculateQFunction(new String[]{"O","I-GENE","*"},bigramCounts,trigramCounts,qFunctionResults);
        calculateQFunction(new String[]{"STOP","I-GENE","O","*"},bigramCounts,trigramCounts,qFunctionResults);

        return qFunctionResults;
    }

    protected void calculateQFunction(String[] keyTags,Map<NGramTag,Integer> bigramCounts, Map<NGramTag,Integer> trigramCounts, Map<String,Double> qFunctionMap){

            String tag =  keyTags[0];
            for(int i=0; i< keyTags.length; i++){
                for(int j=0; j< keyTags.length; j++){
                    NGramTag bigramTag = new NGramTag(2,keyTags[i],keyTags[j]);
                    NGramTag trigramTag = new NGramTag(3,keyTags[i],new String[]{keyTags[j],tag});
                    int numerator = trigramCounts.containsKey(trigramTag) ? trigramCounts.get(trigramTag) : 0;
                    int denominator = bigramCounts.containsKey(bigramTag) ? bigramCounts.get(bigramTag) : 0;
                    double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
                    if(qFunction > 0) {
                        qFunctionMap.put(tag + "Given" + keyTags[i] + "And" + keyTags[j],qFunction);
                    }

                }
            }
    }

    List<String> calculateViterbiEstimates(String[] words, Map<String, Double> qFunction, Map<WordTag, Double> expectationMap, NGramTagResults tagResults, boolean useRareSubclasses){

        DynamicProgrammingResults dynamicProgrammingResults = calculatePiMap(words,qFunction,expectationMap,tagResults, useRareSubclasses);
        Map<String, Double> piMap =  dynamicProgrammingResults.getPiMap();

        Map<String, String> maxBackPointerMap =  dynamicProgrammingResults.getMaxBackPointerMap();


        Map<String,Double> endMap = new LinkedHashMap<String,Double>();
        String[] tags = {"O","I-GENE"};
        double currentMax = 0.0;
        for(int u=0; u< tags.length; u++){
            for(int v=0; v<tags.length; v++){
                String lookupKey = "pi("+Integer.toString(words.length)+","+tags[u]+","+tags[v]+")";
                Double piValue = piMap.containsKey(lookupKey)?piMap.get(lookupKey):0.0F;
                String qKey = "STOP" + "Given" + tags[u] + "And" + tags[v];
                Double qValue = qFunction.containsKey(qKey)? qFunction.get(qKey) : 0.0F;
                if(piValue*qValue > currentMax){
                    currentMax = piValue*qValue;
                    endMap.put(new String(tags[u] + "," + tags[v]),currentMax);
                }

            }
        }

        String[] calculatedTags = new String[words.length+1];

        calculatedTags[0] = "*";
        double maxValue = 0.0F;
        for(String key:endMap.keySet()){
            if(endMap.get(key) > maxValue){
               maxValue = endMap.get(key);
               String[] split = key.split(",");
               calculatedTags[words.length]=split[1];
               calculatedTags[words.length-1]=split[0];
            }
        }


        for(int k = words.length-2; k > 0;k--){
            calculatedTags[k] = maxBackPointerMap.get(Integer.toString(k+2)+"_"+calculatedTags[k+1]+"_"+calculatedTags[k+2]);
        }


        List<String> estimatedWords = new ArrayList<String>();
        for(int blah=1; blah<=words.length; blah++){
            estimatedWords.add(words[blah-1] + " " + calculatedTags[blah]);
        }
        return estimatedWords;
    }

    protected DynamicProgrammingResults calculatePiMap(String[] words, Map<String, Double> qFunction, Map<WordTag, Double> expectationMap, NGramTagResults tagResults, boolean useRareSubclasses){

        DynamicProgrammingResults dynamicProgrammingResults = new DynamicProgrammingResults();
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();
        piMap.put("pi(0,*,*)",1.0);
        String[] wTags;
        String[] uTags;
        String[] tags = {"O","I-GENE"};
        for(int k=1; k<=words.length; k++){

            if(k == 1){
                wTags = new String[]{"*"};
                uTags = new String[]{"*"};
            } else if (k ==2){
                wTags = new String[]{"*"};
                uTags = new String[]{"O","I-GENE"};
            } else {
                wTags = new String[]{"O","I-GENE"};
                uTags = new String[]{"O","I-GENE"};
            }

            for(int u=0; u < uTags.length; u++){
                    for(int v=0; v<tags.length; v++){
                        double currentMax = 0.0F;
                        for(int w=0; w<wTags.length; w++){
                            String key = "pi("+k+","+uTags[u]+","+tags[v]+")";
                            WordTag wordTag = new WordTag(words[k-1],tags[v]);
                            double expectation = 0.0F;
                            if(tagResults.getWordCountMap().containsKey(words[k-1]) && tagResults.getWordCountMap().get(words[k-1]) >= 5){
                                expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : 0.0F;
                            } else {
                                String rareClass = "_RARE_";
                                if(useRareSubclasses){
                                    rareClass = deduceRareSubclass(words[k-1]);
                                }
                                expectation = expectationMap.get(new WordTag(rareClass,tags[v]));
                            }

                            Double pivalue =  piMap.containsKey("pi("+ new Integer(k-1) +","+wTags[w]+"," + uTags[u]+ ")") ? piMap.get("pi("+ new Integer(k-1) +","+wTags[w]+"," + uTags[u]+ ")") : 0.0F;
                            Double result = pivalue*qFunction.get(tags[v] + "Given" + wTags[w] + "And" + uTags[u])*expectation;
                            if(result > currentMax){ //TODO revisit
                                currentMax = result;
                                piMap.put(key,result);
                                maxBackPointerMap.put(k+"_"+uTags[u]+"_"+tags[v],wTags[w]);
                            }
                        }
                    }
                }
        }
        return dynamicProgrammingResults;
    }

}
