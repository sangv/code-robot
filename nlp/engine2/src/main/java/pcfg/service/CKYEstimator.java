package pcfg.service;

import domain.DynamicProgrammingResults;
import domain.NGramTag;
import domain.Sentence;
import domain.TagResults;
import pcfg.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
                    qFunctionMap.get(binaryRuleTag.getTag()).put(binaryRuleTag.getOthers()[0] + "_AND_" + binaryRuleTag.getOthers()[1],qFunction);
                } else {
                    Map<String,Double> newMap = new LinkedHashMap<String,Double>();
                    newMap.put(binaryRuleTag.getOthers()[0] + "_AND_" + binaryRuleTag.getOthers()[1],qFunction);
                    qFunctionMap.put(binaryRuleTag.getTag(),newMap);
                }
            }
        }
    }

    public void calculateWordGivenX(Map<String,Integer> nonTerminalTagCounts, Map<Sentence.WordTag,Integer> unaryRuleCounts, Map<String,Map<String,Double>> qFunctionMap, TagResults tagResults){

        for (Map.Entry<Sentence.WordTag,Integer> entry: unaryRuleCounts.entrySet()){
            Sentence.WordTag wordTag = entry.getKey();
            int numerator = entry.getValue();
            String WORD = wordTag.getWord();

            int denominator = nonTerminalTagCounts.containsKey(wordTag.getTag()) ? nonTerminalTagCounts.get(wordTag.getTag()) : 0;
            double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;

            if(qFunction > 0) {
                if(qFunctionMap.containsKey(wordTag.getTag())) {
                    qFunctionMap.get(wordTag.getTag()).put(WORD,qFunction);
                } else {
                    Map<String,Double> newMap = new LinkedHashMap<String,Double>();
                    newMap.put(WORD,qFunction);
                    qFunctionMap.put(wordTag.getTag(),newMap);
                }
            }
        }
    }

    public Node calculateFinalString(DynamicProgrammingResults dynamicProgrammingResults, String[] words, Set<String> tags){
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();

        Node parentNode  = new Node();

        buildFinalTree(parentNode, words, piMap, maxBackPointerMap,"SBARQ",1, words.length);

        return parentNode;
    }

    protected void buildFinalTree(Node node, String[] words, Map<String, Double> piMap, Map<String, String> maxBackPointerMap,String tag,int start, int end){

            node.setEmission(tag);
            String key = start+","+end+","+tag;

            if(maxBackPointerMap.containsKey(key)){
                String[] s_Y_Z = maxBackPointerMap.get(key).split("_");
                int s = new Integer(s_Y_Z[0]);
                String Y = s_Y_Z[1];
                String Z = s_Y_Z[2];
                Node leftNode = new Node();
                attachNode(leftNode,node);
                buildFinalTree(leftNode, words, piMap, maxBackPointerMap, Y, start, s);

                Node rightNode = new Node();
                attachNode(rightNode,node);
                buildFinalTree(rightNode, words, piMap, maxBackPointerMap,Z,s+1,end);
            }  else if(start == end) {
                String terminalTag = maxBackPointerMap.get(Integer.toString(start));
                node.setEmission(terminalTag);
                node.setWord(words[start-1]);
            }

    }

    private void attachNode(Node node, Node parentNode) {
        if(parentNode.getLeftNode() == null){
            parentNode.setLeftNode(node);
        } else {
            parentNode.setRightNode(node);
        }
    }

    public DynamicProgrammingResults calculatePiMap(String[] words, Set<String> tags, Map<String,Map<String,Double>> qFunctionY1Y2GivenX, Map<String,Map<String,Double>> qFunctionWordGivenX, TagResults tagResults){

        DynamicProgrammingResults dynamicProgrammingResults = new DynamicProgrammingResults();
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();


        for(int i=1; i <= words.length; i++){
            double currentMax = 0;
            for(String tag: tags){
                String X = tag;
                String WORD = words[i-1];
                if(tagResults != null && (!tagResults.getWordCountMap().containsKey(WORD) || tagResults.getWordCountMap().get(WORD) < 5)){
                    WORD = "_RARE_";
                }
                double qValue = 0;
                Map<String,Double> emissionMap = qFunctionWordGivenX.containsKey(X) ? qFunctionWordGivenX.get(X) : null;
                if(emissionMap != null){
                    qValue = emissionMap.containsKey(WORD) ? emissionMap.get(WORD) : 0.0;
                }
                if(qValue > currentMax){
                    currentMax = qValue;
                    piMap.put(i+","+i+","+X,qValue);
                    maxBackPointerMap.put(Integer.toString(i),X);
                }
            }
        }
        for(int l = 0; l <words.length; l++){
            calculatePiMapAtEachLevel(words, tags, qFunctionY1Y2GivenX, qFunctionWordGivenX, piMap, maxBackPointerMap,l);
        }
        return dynamicProgrammingResults;
    }

    protected void calculatePiMapAtEachLevel(String[] words, Set<String> tags, Map<String,Map<String,Double>> qFunctionY1Y2GivenX, Map<String,Map<String,Double>> qFunctionWordGivenX,Map<String, Double> piMap,Map<String, String> maxBackPointerMap, int l){
        for(int i=1; i <= words.length-l; i++){
                int j=i+l;


                    for(String tag: tags){
                        String X = tag;
                        if(qFunctionY1Y2GivenX.containsKey(X)){
                            Map<String,Double> xEntries = qFunctionY1Y2GivenX.get(X);
                            double currentMax = 0.0F;
                            for(Map.Entry<String,Double> xEntry:xEntries.entrySet()){
                                String split[] = xEntry.getKey().split("_AND_");
                                String Y = split[0];
                                String Z = split[1];
                                String key = i+","+j+","+X;

                                for(int s=i; s<=j-1; s++){

                                    Double pivalue1 = piMap.containsKey(i +","+s+"," + Y) ? piMap.get(i +","+s+"," + Y) : 0.0F;
                                    Double pivalue2 = piMap.containsKey(new Integer(s+1) +","+j+"," + Z) ? piMap.get(new Integer(s+1) +","+j+"," + Z) : 0.0F;
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
