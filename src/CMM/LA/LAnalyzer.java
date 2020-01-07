package CMM.LA;

import CMM.util.EWordKind;
import CMM.util.ErrorExecutor;
import CMM.model.Word;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LAnalyzer {
    //记录单词信息
    ArrayList<Word> wordsInfo = new ArrayList<>();

    /**
     *
     * 进行词法分析
     * @param filename
     */
    public ArrayList<Word> execute(String filename){
        try {
            List<String> strList= FileExecutor.readTestFile(filename);
            int comment=0;
            for(int i=0;i<strList.size();i++){
                String line = FileExecutor.replaceBlank(strList.get(i));
                List<String> words = FileExecutor.readWord(line,i+1);
                for(int j=0;j<words.size();j++){
                    String word = words.get(j);
                    //判断是否为关键字
                    EWordKind judge = JudgeTools.isKeyWord(word);

                    //遇到右半部分，停止多行注释
                    if(judge==EWordKind.RCOMMENT){
                        comment--;
                        if(comment<0){
                            ErrorExecutor.showError("LA","缺少左多行注释",i+1,j+1);
                            comment=0;
                        }
                        continue;
                    }
                    //若标记开启，进行多行注释
                    if(comment>0) continue;
                    //按照 关键词--数字--变量名--字符串顺序判断 都不满足，则进行标记为错
                    if(judge!=EWordKind.WRONG){
                        //遇到左半部分，开启多行注释
                        if(judge==EWordKind.LCOMMENT){
                            comment++;
                            continue;
                        }
                        //单行注释
                        if(judge==EWordKind.COMMENT) break;
                        wordsInfo.add(new Word(word,judge,i+1,j+1));
                        //未开启多行注释时，遇到单行注释，开启单行注释
                    }else if((judge=JudgeTools.isCharacter(word))!=EWordKind.WRONG){
                        //是否为字符
                        wordsInfo.add(new Word(word,judge,i+1,j+1));
                    }else if((judge=JudgeTools.isFNumeric(word))!=EWordKind.WRONG){
                        //是否为浮点数
                        wordsInfo.add(new Word(word,judge,i+1,j+1));
                    }else if((judge=JudgeTools.isINumeric(word))!=EWordKind.WRONG){
                        //是否为整型数字
                        wordsInfo.add(new Word(word,judge,i+1,j+1));
                    }else if((judge=JudgeTools.isIdentifier(word))!=EWordKind.WRONG){
                        //是否为变量名
                        if(word.length()>64){
                            //变量名过长
                            wordsInfo.add(new Word(word,EWordKind.WRONG,i+1,j+1));
                        }else {
                            wordsInfo.add(new Word(word, judge, i + 1, j + 1));
                        }
                    }else if((judge=JudgeTools.isString(word))!=EWordKind.WRONG){
                        //是否为字符串
                        word = word.replaceAll("\\\\t","\t");
                        word = word.replaceAll("\\\\r","\r");
                        word = word.replaceAll("\\\\n","\n");
                        wordsInfo.add(new Word(word,judge,i+1,j+1));
                    } else{
                        //出现错误，忽略该行
                        wordsInfo.add(new Word(word,judge,i+1,j+1));
                        break;
                    }
                }
            }
            return wordsInfo;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 打印分析获得的单词信息
     */
    public void show(){
        for(int i=0;i<wordsInfo.size();i++){
            System.out.println(wordsInfo.get(i));
        }
    }
}
