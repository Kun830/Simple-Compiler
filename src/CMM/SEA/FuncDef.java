package CMM.SEA;

import CMM.SA.SATreeNode;

import java.util.List;

public class FuncDef {
    List<Val> valList;
    String name;
    SATreeNode funcNode;
    FuncDef(String s,List<Val> vals,SATreeNode node){
        name=s;
        valList=vals;
        funcNode=node;
    }
}
