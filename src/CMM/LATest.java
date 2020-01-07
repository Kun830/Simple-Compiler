package CMM;

import CMM.LA.LAnalyzer;
import CMM.model.Word;

import java.io.IOException;
import java.util.ArrayList;

public class LATest {
    public static void main(String argv[]) throws IOException {
        LAnalyzer lAnalyzer = new LAnalyzer();
        ArrayList<Word> words = lAnalyzer.execute("LA.txt");
        lAnalyzer.show();
    }
}
