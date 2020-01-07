package CMM;

import CMM.LA.LAnalyzer;
import CMM.SA.SATreeNode;
import CMM.SA.SAnalyzer;
import CMM.SEA.SEAnalyzer;
import CMM.model.Word;

import java.io.IOException;
import java.util.ArrayList;

public class SATest {
    public static void main(String argv[]) throws IOException {
        LAnalyzer lAnalyzer = new LAnalyzer();
        ArrayList<Word> words = lAnalyzer.execute("SA.txt");
        SAnalyzer sAnalyzer = new SAnalyzer(words);
        SATreeNode node = sAnalyzer.Execute();
        SATreeNode.show(node,0);
//        SEAnalyzer seAnalyzer = new SEAnalyzer(node);
//        seAnalyzer.Executor();
    }
}
