package domain;

/**
 * Created with IntelliJ IDEA.
 * User: sang
 * Date: 3/16/13
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class WordTag {

    enum TAG {IGENE};

    private String tag;

    private String word;

    public WordTag(String word, String tag) {
        this.word = word;
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public String getWord() {
        return word;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WordTag wordTag = (WordTag) o;

        if (!tag.equals(wordTag.tag)) return false;
        if (!word.equals(wordTag.word)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tag.hashCode();
        result = 31 * result + word.hashCode();
        return result;
    }
}