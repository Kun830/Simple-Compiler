package CMM;

import CMM.LA.LAnalyzer;
import CMM.SA.SATreeNode;
import CMM.SA.SAnalyzer;
import CMM.SEA.SEAnalyzer;
import CMM.model.Word;

import java.util.ArrayList;

public class SEATest {
    public static void main(String[] args) {
        LAnalyzer lAnalyzer = new LAnalyzer();
        ArrayList<Word> words = lAnalyzer.execute("SEA.txt");
        SAnalyzer sAnalyzer = new SAnalyzer(words);
        SATreeNode node = sAnalyzer.Execute();
        SATreeNode.show(node,0);
        SEAnalyzer seAnalyzer = new SEAnalyzer(node);
        seAnalyzer.Executor();
    }
}
