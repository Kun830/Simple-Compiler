package CMM.model;

import CMM.util.EWordKind;

/**
 * 单词信息，包括单词，类别，所在行，该行所在位置
 */
public class Word {
    public String word;
    public EWordKind type;
    public int line;
    public int position;

    public Word(String word, EWordKind type, int line, int position) {
        this.word = word;
        this.type = type;
        this.line = line;
        this.position = position;
    }

    @Override
    public String toString() {
        return "{" +
                "word='" + word + '\'' +
                ", type=" + type +
                ", line=" + line +
                ", position=" + position +
                '}';
    }
}
