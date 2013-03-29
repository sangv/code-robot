package domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class BackPointer {


    private Map<String, Double> piMap = new LinkedHashMap<String,Double>();

    private Map<String,String>  maxBackPointerMap = new LinkedHashMap<String,String>();

    public Map<String, Double> getPiMap() {
        return piMap;
    }

    public void setPiMap(Map<String, Double> piMap) {
        this.piMap = piMap;
    }

    public Map<String, String> getMaxBackPointerMap() {
        return maxBackPointerMap;
    }

    public void setMaxBackPointerMap(Map<String, String> maxBackPointerMap) {
        this.maxBackPointerMap = maxBackPointerMap;
    }


}
