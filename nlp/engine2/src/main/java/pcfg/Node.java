package pcfg;

import domain.Sentence.WordTag;

import java.util.ArrayList;
import java.util.List;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class Node{
    private String emission;
    private Node parent;
    List<Node> children = new ArrayList<Node>();


    public Node() {
        super();
    }

    public Node(String emission) {
        this.emission = emission;
    }

    public String getEmission() {
        return emission;
    }

    public void setEmission(String emission) {
        this.emission = emission;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }
}

