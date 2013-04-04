package pcfg;

import domain.Sentence;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class WordTagWithPath extends Sentence.WordTag {

    private String path;

    public WordTagWithPath(String word, String tag, String path) {
        super(word, tag);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WordTagWithPath)) return false;
        if (!super.equals(o)) return false;

        WordTagWithPath that = (WordTagWithPath) o;

        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (!tag.equals(that.tag)) return false;
        if (!word.equals(that.word)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + tag.hashCode() + word.hashCode() + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WordTagWithPath{" +
                "word='" + word + '\'' +
                ", tag='" + tag + '\'' +
                "path='" + path + '\'' +
                '}';
    }
}
