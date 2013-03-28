package domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class BackPointer {


    private Map<String, Float> piMap = new LinkedHashMap<String,Float>();

    private Map<Integer,String>  maxBackPointerMap = new LinkedHashMap<Integer,String>();

    public Map<String, Float> getPiMap() {
        return piMap;
    }

    public void setPiMap(Map<String, Float> piMap) {
        this.piMap = piMap;
    }

    public Map<Integer, String> getMaxBackPointerMap() {
        return maxBackPointerMap;
    }

    public void setMaxBackPointerMap(Map<Integer, String> maxBackPointerMap) {
        this.maxBackPointerMap = maxBackPointerMap;
    }


}
