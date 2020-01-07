package CMM.SA;

import CMM.util.EAWordKind;
import CMM.model.Word;

import java.util.ArrayList;

public class SATreeNode {
    public Word word;
    public EAWordKind type;
    public ArrayList<SATreeNode> children;
    SATreeNode(Word w,EAWordKind t){
        word = w;
        type = t;
        children=new ArrayList<>();
    }
    SATreeNode(EAWordKind t){
        word = null;
        type = t;
        children=new ArrayList<>();
    }

    public SATreeNode(EAWordKind rs, Word word) {
        this.word = word;
        type = rs;
        children=new ArrayList<>();
    }

    public static void show(SATreeNode node,int dis){
        for(int i=0;i<dis;i++)
            System.out.print('-');
        if(node.type==EAWordKind.TOKEN){
            System.out.println("<"+node.word.type.toString()+">: "+node.word.word);
        }else{
            System.out.println("<"+node.type.toString()+">");
        }
        for(int i=0;i<node.children.size();i++){
            SATreeNode child = node.children.get(i);
            show(child,dis+1);
        }
    }
}
