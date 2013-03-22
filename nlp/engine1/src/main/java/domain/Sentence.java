package domain;

import java.util.List;

/**
 * The Sentence is an abstraction made up of wordTags.
 *
 * @author Sang Venkatraman
 */
public class Sentence {

    List<WordTag> wordTags;

    public Sentence(List<WordTag> wordTags) {
        this.wordTags = wordTags;
    }

    /**
     * WordTag represents the combination of the word and its tag
     *
     */
    public static class WordTag {

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

    public List<WordTag> getWordTags() {
        return wordTags;
    }
}
