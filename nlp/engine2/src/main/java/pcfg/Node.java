package pcfg;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class Node{
    private String emission;
    private String word;
    private Node leftNode;
    private Node rightNode;


    public Node() {
        super();
    }

    public Node(String word, String emission) {
        this.word = word;
        this.emission = emission;
    }

    public String getEmission() {
        return emission;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setEmission(String emission) {
        this.emission = emission;
    }

    public Node getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(Node leftNode) {
        this.leftNode = leftNode;
    }

    public Node getRightNode() {
        return rightNode;
    }

    public void setRightNode(Node rightNode) {
        this.rightNode = rightNode;
    }
}

