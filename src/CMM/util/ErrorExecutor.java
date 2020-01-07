package CMM.util;

public class ErrorExecutor {
    public static void showError(String type,String msg,int line,int position){
        System.out.println(String.format("%s错误：%s，错误发生在第%d行 第%d处",
                type,msg,line,position));
    }
}
