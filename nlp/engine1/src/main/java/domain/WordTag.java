package domain;

/**
 *
 * @author Sang Venkatraman
 */
public class WordTag {

    private String word;

    private String tag;

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
        if (!(o instanceof WordTag)) return false;

        WordTag wordTag = (WordTag) o;

        if (!tag.equals(wordTag.tag)) return false;
        if (!word.equals(wordTag.word)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = word.hashCode();
        result = 31 * result + tag.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "WordTag{" +
                "word='" + word + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }
}
