package CMM.SA;

import CMM.util.EAWordKind;
import CMM.util.EWordKind;
import CMM.util.ErrorExecutor;
import CMM.model.Word;

import java.util.ArrayList;


public class SAnalyzer {
    ArrayList<Word> wordsinfo = null;
    Word tail = null;
    public SAnalyzer(ArrayList<Word> w){
        wordsinfo=w;
        tail = w.get(w.size()-1);
    }

    /**
     * 开始语法分析
     */
    public SATreeNode Execute(){
        if(wordsinfo!=null||wordsinfo.size()!=0) {
            try {
                SATreeNode treeRoot = new SATreeNode(EAWordKind.PROGRAM);
                isUnit(0,treeRoot);
                return treeRoot;
            } catch (SAException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * 判断是否为Unit
     * @param begin
     * @param root
     * @return
     */
    private int isUnit(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "处理单元缺失";
        Word word;
        EAWordKind rs = EAWordKind.UNIT;

        //文法识别结束
        if(end==wordsinfo.size()) {
            return begin;
        }
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别Statement或者Def-Func
        tmp=end;
        if((end=isStatement(tmp,node))==tmp&&(end=isDefFunc(tmp,node))==tmp){
            throw new SAException(rs,msg,word.line,word.position);
        }

        root.children.add(node);
        end=isUnit(end,root);
        return end;
    }

    /**
     * 识别Def-Func
     * @param begin
     * @param root
     * @return
     */
    private int isDefFunc(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "函数定义缺失";
        Word word;
        EAWordKind rs = EAWordKind.DEFFUNC;

        //文法识别结束
        if(end==wordsinfo.size()) {
            return begin;
        }
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别def
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.DEF){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }

        //识别identifier
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.IDENTIFIER,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别(
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别def-val-list
        tmp=end;
        word= wordsinfo.get(tmp);
        end = isDefValList(tmp,node);
        checkEnd(end,rs,msg);

        //识别)
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别block
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = isBlock(tmp,node))==tmp){
            throw new SAException(rs,msg,word.line,word.position);
        }

        root.children.add(node);
        return end;
    }

    /**
     * 识别Def-val-list
     * @param begin
     * @param root
     * @return
     */
    private int isDefValList(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "函数列表缺失";
        Word word;
        EAWordKind rs = EAWordKind.DEFVALLIST;

        //文法识别结束
        if(end==wordsinfo.size()) {
            return begin;
        }
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //循环识别变量定义
        tmp=end;
        while(true){
            end = isDeclaration(tmp,node);
            if(end==wordsinfo.size()||end==tmp) break;

            //识别;
            tmp=end;
            word= wordsinfo.get(tmp);
            if((end = tryCheckToken(tmp,EWordKind.BRANCH,rs))!=tmp){
                node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            }
            tmp=end;
        }

        root.children.add(node);
        return end;
    }


    /**
     * 语句识别
     * @param begin
     * @return
     */
    private int isStatement(int begin,SATreeNode root) throws SAException {
        int end = begin;
        String msg = "语句缺失";
        Word word;
        EAWordKind rs = EAWordKind.STATEMENT;

        //文法识别结束
        if(end==wordsinfo.size()) {
            return begin;
        }
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);


        //结构子节点，识别成功
        if ((end = isWhile(begin,node)) > begin ||
                (end = isIfElse(begin,node)) > begin) {
        } else if ((end = isAssignment(begin,node)) > begin ||
                (end = isDeclaration(begin,node)) > begin ||
                (end = isIn(begin,node)) > begin ||
                (end = isOut(begin,node)) > begin ||
                (end = isJump(begin,node)) > begin ||
                (end = isReturn(begin,node)) > begin ||
                (end = isCallFunc(begin,node)) > begin) {
            //分句子节点识别成功
            //判断分号情况
            checkEnd(end,rs,msg);
            word = wordsinfo.get(end);
            end = checkToken(end,EWordKind.BRANCH,rs);
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }else{
            return begin;
        }

        //识别成功，尝试下一个兄弟节点statement
        root.children.add(node);
        return end;
    }

    /**
     * 识别call-func
     * @param begin
     * @param root
     * @return
     */
    private int isCallFunc(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        Word word;
        String msg = "函数调用结构错误";
        EAWordKind rs = EAWordKind.CALLFUNC;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别identifier
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.IDENTIFIER){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }
        //若下一个为[，则退出函数调用识别
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = tryCheckToken(tmp,EWordKind.LARRAY,rs))!=tmp) {
            return begin;
        }

        //识别(
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别valList
        end=isValList(end,node);
        checkEnd(end,rs,msg);

        //识别)
        tmp=end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }
        root.children.add(node);
        return end;
    }

    /**
     * 识别ValList
     * @param begin
     * @param root
     * @return
     * @throws SAException
     */
    private int isValList(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        Word word;
        String msg = "参数传递列表错误";
        EAWordKind rs = EAWordKind.VALLIST;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //尝试识别Variable和RestValuable
        tmp=end;
        if((end=isVariable(tmp,node))!=tmp){
            end = isRestVariable(end,node);
        }

        root.children.add(node);
        return end;
    }

    /**
     * 识别返回语句
     * @param begin
     * @param root
     * @return
     */
    private int isReturn(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        Word word;
        String msg = "返回语句结构错误";
        EAWordKind rs = EAWordKind.RETURN;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别return
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.RETURN){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }

        //尝试识别expression
        tmp=end;
        end = isExpression(tmp,node);

        root.children.add(node);
        return end;
    }

    /**
     * 跳转关键字识别
     * @param begin
     * @return
     */
    private int isJump(int begin,SATreeNode root) throws SAException {
        int end = begin;
        Word word;
        String msg = "语句缺失";
        EAWordKind rs = EAWordKind.JUMP;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别跳转关键字
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.BREAK&&
                word.type!=EWordKind.CONTINUE){
            return begin;
        }else{
            end = addIndex(end,rs,msg);
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            root.children.add(node);
            return end;
        }
    }

    /**
     * 输入语句识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isIn(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        EAWordKind rs = EAWordKind.IN;
        Word word;
        String msg = "输入语句缺失";

        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别SCAN
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.SCAN){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }
        //识别(
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别variable
        end=isVariable(end,node);
        checkEnd(end,rs,msg);

        //识别)
        tmp=end;
        if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }
        root.children.add(node);
        return end;
    }

    /**
     * 输出语句识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isOut(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "输出缺失";
        EAWordKind rs = EAWordKind.OUT;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);
        //识别PRINT
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.PRINT){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }
        //识别(
        tmp=end;
        word= wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        tmp=end;
        //识别expression
        if((end=isExpression(end,node))==tmp&&(end=isString(end,node))==tmp){
            throw new SAException(rs,msg,word.line,word.position);
        }
        checkEnd(end,rs,msg);

        //识别)
        tmp=end;
        if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        root.children.add(node);
        return end;
    }

    /**
     * 判断是否为字符串
     * @param begin
     * @param root
     * @return
     */
    private int isString(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "字符串格式错误";
        EAWordKind rs = EAWordKind.STRING;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);
        if(word.type!= EWordKind.STRING){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }
        root.children.add(node);
        return end;
    }

    /**
     * 变量识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isVariable(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "变量缺失";
        Word word;
        EAWordKind rs = EAWordKind.VARIABLE;


        tmp = end;
        word = wordsinfo.get(tmp);
        end = tryCheckToken(tmp,EWordKind.IDENTIFIER,rs);
        if(end==tmp) return end;

        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);
        node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        //若下一个为(，则退出变量识别
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = tryCheckToken(tmp,EWordKind.LBRACKET,rs))!=tmp) {
            return begin;
        }

        //尝试识别[
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = tryCheckToken(tmp,EWordKind.LARRAY,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            //识别数组下标
            msg="数组下标缺失";
            tmp = end;
            word = wordsinfo.get(end);
            if((end = isExpression(tmp,node))==tmp){
                throw new SAException(rs,msg,word.line,word.position);
            }
            checkEnd(end,rs,msg);

            //识别]
            tmp = end;
            word = wordsinfo.get(tmp);
            if((end = checkToken(tmp,EWordKind.RARRAY,rs))!=tmp){
                node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            }
        }

        root.children.add(node);
        return end;
    }


    /**
     * 识别expression
     * @param begin
     * @return
     * @throws SAException
     */
    private int isExpression(int begin,SATreeNode root) throws SAException{
        int end = begin,tmp;
        String msg = "expression结构缺失";
        EAWordKind rs = EAWordKind.EXPRESSION;
        Word word;

        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        if((end=isCalculation(begin,node))!=begin||(end=isCharacter(begin,node))!=begin){
            root.children.add(node);
        }

        return end;
    }

    private int isCharacter(int begin, SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "字符";
        EAWordKind rs = EAWordKind.EXPRESSION;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        tmp=end;
        word = wordsinfo.get(tmp);
        if((end = tryCheckToken(tmp,EWordKind.CHARACTER,rs))!=tmp){
            root.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            return end;
        }
        return begin;
    }

    /**
     * 识别calculation
     * @param begin
     * @param root
     * @return
     * @throws SAException
     */
    private int isCalculation(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "calculation结构缺失";
        EAWordKind rs = EAWordKind.CALCULATION;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别item
        if((end = isItem(begin,node))==begin){
            return begin;
        }

        //尝试识别+|-号
        tmp=end;
        word = wordsinfo.get(tmp);
        if(((end = tryCheckToken(tmp,EWordKind.PLUS,rs))==tmp)&&
            (end = tryCheckToken(tmp,EWordKind.MINUS,rs))==tmp){
            root.children.add(node);
            return end;
        }
        node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

        tmp = end;
        if((end = isCalculation(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        root.children.add(node);
        return end;
    }

    /**
     * 识别item
     * @param begin
     * @param root
     * @return
     * @throws SAException
     */
    private int isItem(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "item结构缺失";
        EAWordKind rs = EAWordKind.ITEM;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别factor
        if((end = isFactor(begin,node))==begin){
            return begin;
        }

        //尝试识别*|/号
        tmp=end;
        word = wordsinfo.get(tmp);
        if(((end = tryCheckToken(tmp,EWordKind.ASTERISK,rs))==tmp)&&
                (end = tryCheckToken(tmp,EWordKind.DIVIDE,rs))==tmp){
            root.children.add(node);
            return end;
        }
        node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

        tmp = end;
        if((end = isItem(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        root.children.add(node);
        return end;
    }

    private int isFactor(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "factor结构缺失";
        EAWordKind rs = EAWordKind.FACTOR;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别数字、变量、函数调用
        if((end=isNumber(begin,node))==begin&&(end=isVariable(begin,node))==begin
                &&(end=isCallFunc(begin,node))==begin){
            //识别(
            tmp=end;
            word = wordsinfo.get(tmp);
            if((end = tryCheckToken(tmp,EWordKind.LBRACKET,rs))==tmp){
                root.children.add(node);
                return end;
            }
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

            //识别calculation
            tmp = end;
            if((end = isCalculation(tmp,node))==tmp){
                word = wordsinfo.get(end);
                throw new SAException(rs,msg,word.line,word.position);
            }

            //识别)
            tmp = end;
            word = wordsinfo.get(end);
            if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
                node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            }
        }
        root.children.add(node);
        return end;
    }

    /**
     * 识别number
     * @param begin
     * @param root
     * @return
     * @throws SAException
     */
    private int isNumber(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "数字结构缺失";
        EAWordKind rs = EAWordKind.NUMBER;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        word = wordsinfo.get(begin);
        if((end = tryCheckToken(begin,EWordKind.PLUS,rs))==begin&&
                (end = tryCheckToken(begin,EWordKind.MINUS,rs))==begin){
            tmp = end;
            //识别无符号数字
            if((end = isUNmber(tmp,node))==tmp){
                return begin;
            }
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = isNumber(end,node);
        }
        root.children.add(node);
        return end;
    }

    /**
     * 识别unimber
     * @param begin
     * @param root
     * @return
     * @throws SAException
     */
    private int isUNmber(int begin,SATreeNode root) throws SAException {
        int end = begin;
        String msg = "无符号数识别失败";
        EAWordKind rs = EAWordKind.JUMP;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别数字
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.INUMBER&&
                word.type!=EWordKind.FNUMBER){
            return begin;
        }else{
            root.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
            return end;
        }
    }

    /**
     * 识别关系运算符
     * @param begin
     * @return
     * @throws SAException
     */
    private int isRelation(int begin,SATreeNode root) throws SAException {
        int end = begin;
        String msg = "关系运算符识别失败";
        EAWordKind rs = EAWordKind.JUMP;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别关系运算符
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.BIGGER&&
                word.type!=EWordKind.BIGGERE&&
                word.type!=EWordKind.SMALLER&&
                word.type!=EWordKind.SMALLERE&&
                word.type!=EWordKind.EQUAL&&
                word.type!=EWordKind.NEQUAL){
            return begin;
        }else{
            root.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
            return end;
        }
    }

    /**
     * 识别声明语句
     * @param begin
     * @return
     * @throws SAException
     */
    private int isDeclaration(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "declaration结构缺失";
        EAWordKind rs = EAWordKind.DECLARATION;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        word = wordsinfo.get(begin);
        //识别type
        if((end=isType(begin,node))==begin){
            return begin;
        }else{
            checkEnd(end,rs,msg);
        }

        //识别variable
        tmp = end;
        if((end = isVariable(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        checkEnd(end,rs,msg);

        //识别rest-variable
        end = isRestVariable(end,node);
        root.children.add(node);
        return end;
    }

    /**
     * 识别restVariable
     * @param begin
     * @param root
     * @return
     * @throws SAException
     */
    private int isRestVariable(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "声明结构缺失";
        EAWordKind rs = EAWordKind.RESTVARIABLE;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        word = wordsinfo.get(begin);
        if((end=tryCheckToken(begin,EWordKind.COMMA,rs))!=begin){
            //识别逗号
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

            //识别variable
            tmp = end;
            if((end = isVariable(tmp,node))==tmp){
                word = wordsinfo.get(end);
                throw new SAException(rs,msg,word.line,word.position);
            }
            checkEnd(end,rs,msg);

            //识别rest-variable
            tmp = end;
            if((end = isRestVariable(tmp,node))==tmp){
                root.children.add(node);
                return end;
            }
        }
        root.children.add(node);
        return end;
    }

    /**
     * 识别类型关键字
     * @param begin
     * @return
     * @throws SAException
     */
    private int isType(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "类型结构缺失";
        EAWordKind rs = EAWordKind.TYPE;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别跳转关键字
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.INT&&
                word.type!=EWordKind.REAL&&
                word.type!=EWordKind.CHAR){
            return begin;
        }else{
            root.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
            return end;
        }
    }

    /**
     * 赋值语句识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isAssignment(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "assignment结构缺失";
        EAWordKind rs = EAWordKind.ASSIGNMENT;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        if((end=isVariable(end,node))==begin){
            return begin;
        }else{
            checkEnd(end,rs,msg);
        }

        //识别等号
        tmp = end;
        word = wordsinfo.get(end);
        if((end = tryCheckToken(tmp,EWordKind.ASSIGN,rs))==tmp){
            return begin;
        }
        node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

        //识别expression
        tmp = end;
        if((end = isExpression(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }

        root.children.add(node);
        return end;
    }


    /**
     * if-else语句识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isIfElse(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "if结构缺失";
        EAWordKind rs = EAWordKind.IFELSE;
        Word word;
        if(begin==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别if
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.IF){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }
        //识别(
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别condition
        tmp = end;
        if((end = isCondition(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        checkEnd(end,rs,msg);

        //识别)
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别顺序内容
        tmp = end;
        if ((end = isBlock(tmp,node)) == tmp) {
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        if (end == wordsinfo.size()) {
            root.children.add(node);
            return end;
        }

        //尝试识别else
        tmp=end;
        word = wordsinfo.get(tmp);
        if((end=tryCheckToken(tmp,EWordKind.ELSE,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

            //识别跳转内容
            tmp = end;
            if ((end = isBlock(tmp,node)) == tmp) {
                word = wordsinfo.get(end);
                throw new SAException(rs,msg,word.line,word.position);
            }
        }
        root.children.add(node);
        return end;
    }

    /**
     * while语句识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isWhile(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        EAWordKind rs = EAWordKind.WHILE;
        String msg = "while结构缺失";
        Word word;
        if(begin==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);
        //识别while
        word = wordsinfo.get(end);
        if(word.type!= EWordKind.WHILE){
            return begin;
        }else{
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
            end = addIndex(end,rs,msg);
        }
        //识别(
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别condition
        tmp = end;
        if((end = isCondition(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        checkEnd(end,rs,msg);

        //识别)
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.RBRACKET,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别循环体
        tmp = end;
        if ((end = isBlock(tmp,node)) == tmp) {
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }

        root.children.add(node);
        return end;
    }

    /**
     * 代码块语句识别
     * @param begin
     * @return
     * @throws SAException
     */
    private int isBlock(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        EAWordKind rs = EAWordKind.BLOCK;
        String msg = "代码块结构缺失";
        Word word;
        if(wordsinfo.size()==begin)
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);
        //识别{
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.LBRACES,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        //识别statement
        while(end!=tmp) {
            tmp = end;
            if ((end = isStatement(tmp, node)) == tmp) {
                break;
            }
            checkEnd(end, rs, msg);
        }

        //识别}
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = checkToken(tmp,EWordKind.RBRACES,rs))!=tmp){
            node.children.add(new SATreeNode(word,EAWordKind.TOKEN));
        }

        root.children.add(node);
        return end;
    }

    private int isCondition(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "condition结构缺失";
        EAWordKind rs = EAWordKind.CONDITION;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别subCon
        tmp = end;
        if((end = isSubCon(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        checkEnd(end,rs,msg);

        //尝试识别逻辑运算号
        tmp=end;
        word = wordsinfo.get(tmp);
        if(((end = tryCheckToken(tmp,EWordKind.AND,rs))==tmp)&&
                (end = tryCheckToken(tmp,EWordKind.OR,rs))==tmp){
            root.children.add(node);
            return end;
        }
        node.children.add(new SATreeNode(word,EAWordKind.TOKEN));

        tmp = end;
        if((end = isCondition(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        root.children.add(node);
        return end;
    }

    private int isSubCon(int begin,SATreeNode root) throws SAException {
        int end = begin,tmp;
        String msg = "subCon结构缺失";
        EAWordKind rs = EAWordKind.SUBCON;
        Word word;
        if(end==wordsinfo.size())
            return begin;
        word = wordsinfo.get(begin);
        SATreeNode node = new SATreeNode(rs,word);

        //识别expression
        tmp = end;
        if((end = isExpression(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        checkEnd(end,rs,msg);

        //识别关系运算符
        tmp = end;
        word = wordsinfo.get(tmp);
        if((end = isRelation(tmp,node))==tmp){
            throw new SAException(rs,msg,word.line,word.position);
        }

        //识别expression
        tmp = end;
        if((end = isExpression(tmp,node))==tmp){
            word = wordsinfo.get(end);
            throw new SAException(rs,msg,word.line,word.position);
        }
        root.children.add(node);
        return end;
    }

    private int addIndex(int index,EAWordKind type,String msg) throws SAException {
        index++;
        return index;
    }

    private void checkEnd(int index,EAWordKind type,String msg) throws SAException {
        if(index==wordsinfo.size()){
            throw new SAException(type,msg,tail.line,tail.position);
        }
    }

    private int checkToken(int index,EWordKind type,EAWordKind res) throws SAException {
        Word word = wordsinfo.get(index);
        if(word.type!=type){
            ErrorExecutor.showError("SA","缺少"+type.toString(),word.line,word.position);
            //忽略错误，暂不报错
        }else{
            index = addIndex(index,res,res.toString()+"识别未完成");
        }
        return index;
    }

    private int tryCheckToken(int index,EWordKind type,EAWordKind res) throws SAException {
        Word word = wordsinfo.get(index);
        if(word.type==type){
            index=addIndex(index,res,res.toString()+"识别未完成");
        }
        return index;
    }
}
