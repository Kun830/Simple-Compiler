package CMM.SA;

import CMM.util.EAWordKind;

public class SAException extends Exception {
    SAException(EAWordKind type, String msg, int line, int pos){
        super(String.format("%s错误：%s，错误发生在第%d行 第%d处",type.toString(),msg,line,pos));
    }
}
