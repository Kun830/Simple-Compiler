package CMM.LA;

import CMM.util.EWordKind;
import CMM.util.ErrorExecutor;
import CMM.model.Word;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileExecutor {
    /**
     * 逐行读取文件
     * @param filename
     * @return
     * @throws IOException
     */
    public static List<String> readTestFile(String filename) throws IOException {
        List<String >list=new ArrayList<String>();
        String fileContent="";
        FileReader fread = new FileReader(filename);
        BufferedReader bf=new BufferedReader(fread);
        String strLine=bf.readLine();
        while(strLine!=null){
            list.add(strLine);
            strLine=bf.readLine();
        }
        bf.close();
        fread.close();
        return list;
    }

    /**
     * 处理换行、制表符
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll(" ");
        }
        return dest;
    }

    public static void writeWord(ArrayList<Word> words, String path){
        File writename = new File(path); // 相对路径，如果没有则要建立一个新的output。txt文件
        try {
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            for(Word s:words){
                out.write(s+"\r\n");
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拆分单词
     * @param s
     * @param line
     * @return
     */
    public static ArrayList<String> readWord(String s, int line) {
        ArrayList<String> words = new ArrayList<>();
        StringBuffer all = new StringBuffer(s);
        StringBuffer word = new StringBuffer("");
        boolean isStr=false;
        for(int i=0;i<all.length();i++){
            char cur=all.charAt(i);
            //字符串开关
            if(cur=='"'){
                isStr=!isStr;
                if(!isStr)
                    word.append(cur);
                //切割前部收入词组
                if(!"".equals(word.toString())){
                    words.add(word.toString());
                    word = new StringBuffer("");
                }
                if(isStr)
                    word.append(cur);
                continue;
            }

            if(isStr) {
                //字符串开启
                word.append(cur);
            }else if(cur == ' '|| isOptsOrEnd(cur)){
                //若当前字符为空格或者分割字符，切割单词
                //切割前部收入词组
                if(!"".equals(word.toString())){
                    words.add(word.toString());
                    word = new StringBuffer("");
                }

                //判断切割字符是否为关键字
                if(isOptsOrEnd(cur)){
                    word.append(cur);
                    //将后一位字符加入，进行关键字判断
                    if(i+1<all.length()){
                         word.append(all.charAt(i+1));
                         //关键字判断失败，回退一位，若成功，则将这两个看作一个单词收入词库
                        if(JudgeTools.isKeyWord(word.toString())== EWordKind.WRONG) {
                            word.deleteCharAt(1);
                            i--;
                        }
                        //成功，不回退
                        i++;
                    }
                    if(cur=='\''){
                        if(i+2<all.length()&&all.charAt(i+2)=='\''){
                            word.append(all.charAt(i+1));
                            word.append(all.charAt(i+2));
                            i+=2;
                        }else if(i+3<all.length()&&all.charAt(i+3)=='\''&&all.charAt(i+1)=='\\'&&
                                (all.charAt(i+2)=='r'||all.charAt(i+2)=='t'||all.charAt(i+2)=='n')){
                            word.append(all.charAt(i+1));
                            word.append(all.charAt(i+2));
                            word.append(all.charAt(i+3));
                            i+=3;
                        }
                    }
                    words.add(word.toString());
                    word=new StringBuffer("");
                }
            }else {
                word.append(cur);
            }
        }
        //尾部单词
        if(!"".equals(word.toString())){
            words.add(word.toString());
        }
        if(isStr) {
            ErrorExecutor.showError("LA","识别到未闭合的字符串结构",line+1,0);
            words.clear();
        }
        return words;
    }

    /**
     * 切割字符
     * @param cur
     * @return
     */
    private static boolean isOptsOrEnd(char cur) {
        switch(cur){
            case '+':
                return true;
            case '-':
                return true;
            case '*':
                return true;
            case '/':
                return true;
            case '>':
                return true;
            case '<':
                return true;
            case '=':
                return true;
            case ';':
                return true;
            case '(':
                return true;
            case ')':
                return true;
            case '{':
                return true;
            case '}':
                return true;
            case '\'':
                return true;
            case '[':
                return true;
            case ']':
                return true;
            case '&':
                return true;
            case '|':
                return true;
            case ',':
                return true;
            case '\\':
                return true;
        }
        return false;
    }


}
