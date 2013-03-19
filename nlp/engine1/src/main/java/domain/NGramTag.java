package domain;

import java.util.Arrays;

/**
 *
 * @author Sang Venkatraman
 */
public class NGramTag {

    private Integer n;

    private String tag;

    private String[] others;

    public NGramTag(Integer n, String tag, String... others) {
        this.n = n;
        this.tag = tag;
        this.others = others;
    }

    public Integer getN() {
        return n;
    }

    public String getTag() {
        return tag;
    }

    public String[] getOthers() {
        return others;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NGramTag nGramTag = (NGramTag) o;

        if (!n.equals(nGramTag.n)) return false;
        if (!Arrays.equals(others, nGramTag.others)) return false;
        if (!tag.equals(nGramTag.tag)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = n.hashCode();
        result = 31 * result + tag.hashCode();
        result = 31 * result + (others != null ? Arrays.hashCode(others) : 0);
        return result;
    }
}
