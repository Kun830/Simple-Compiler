package CMM.SEA;

import CMM.SA.SATreeNode;
import CMM.util.EAWordKind;
import CMM.util.EWordKind;
import CMM.util.ErrorExecutor;
import org.omg.CORBA.INTERNAL;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import static CMM.util.EAWordKind.*;

public class SEAnalyzer {
    SATreeNode root;
    public ArrayList<String> midProcess;
    Val rax = new Val();
    ArrayList<ArrayList<Val>> valTable;
    ArrayList<FuncDef> funcDefList;
    public SEAnalyzer(SATreeNode node){
        root = node;
        valTable = new ArrayList<>();
        funcDefList=new ArrayList<>();
        midProcess = new ArrayList<>();
    }
    enum JUMPTYPE{
        NOJUMP,
        BREAK,
        CONTINUE,
        RETURN,
        ;
    }
    public void show(){
        for(int i=0;i<midProcess.size();i++){
            System.out.println(midProcess.get(i));
        }
    }

    /**
     * 执行语义分析
     */
    public void Executor(){
        ArrayList<Val> table = new ArrayList<>();

        valTable.add(table);
        for(int i=0;i<root.children.size();i++){
            SATreeNode node = root.children.get(i);
            doUnit(node,table);
        }
        ArrayList<Val> globe = valTable.get(0);
        for(int i=0;i<valTable.size();i++) {
            Val tmp = globe.get(i);
            midProcess.add("Type: "+tmp.type+" Name: "+tmp.name+" Val: "+tmp.value.toString());
        }
        valTable.remove(valTable.size()-1);
    }

    /**
     * unit语义单元执行
     * @param root
     * @param table
     */
    private void doUnit(SATreeNode root, ArrayList<Val> table) {
        SATreeNode node = root.children.get(0);
        switch (node.type){
            case STATEMENT:
                doStatement(node,table);
                break;
            case DEFFUNC:
                doDefFunc(node);
                break;
        }
    }

    /**
     * 函数定义语义执行
     * @param root
     */
    private void doDefFunc(SATreeNode root) {
        SATreeNode name = root.children.get(1);
        SATreeNode defValList = root.children.get(3);
        SATreeNode block = root.children.get(5);

        ArrayList<Val> vals = doDefValList(defValList);
        FuncDef funcDef = new FuncDef(name.word.word,vals,block);
        if(checkFuncDef(funcDef)) {
            funcDefList.add(funcDef);
            midProcess.add("funcList.add("+name+")");
        }else{
            ErrorExecutor.showError("SEA","函数重复定义",name.word.line,name.word.position);
        }
    }

    /**
     * 检查函数是否重复定义
     * @param funcDef
     * @return
     */
    private boolean checkFuncDef(FuncDef funcDef) {
        for(int i=0;i<funcDefList.size();i++){
            if(compareFunc(funcDefList.get(i),funcDef))
                return false;
        }
        return true;
    }

    /**
     * 检查两个函数签名是否相同
     * @param func1
     * @param func2
     * @return
     */
    private boolean compareFunc(FuncDef func1, FuncDef func2) {
        if(func1.name.equals(func2.name)){
            if(func1.valList.size()==func2.valList.size()){
                for(int i=0;i<func1.valList.size();i++){
                    Val val1 = func1.valList.get(i),val2=func2.valList.get(i);
                    if(val1.type==val2.type) continue;
                    else return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 参数获取
     * @param root
     * @return
     */
    private ArrayList<Val> doDefValList(SATreeNode root) {
        ArrayList<Val> vals = new ArrayList<>();
        for(int i=0;i<root.children.size();i+=2){
            doDeclaration(root.children.get(i),vals);
        }
        return vals;
    }

    /**
     * statement语义分流
     * @param root
     * @param table
     */
    private JUMPTYPE doStatement(SATreeNode root, ArrayList<Val> table) {
        SATreeNode node = root.children.get(0);
        switch (node.type){
            case WHILE:
                return doWhile(node,table);
            case IFELSE:
                return doIfElse(node,table);
            case ASSIGNMENT:
                doAssignment(node);
                break;
            case DECLARATION:
                doDeclaration(node,table);
                break;
            case IN:
                doIn(node);
                break;
            case OUT:
                doOut(node);
                break;
            case JUMP:
                return doJump(node);
            case CALLFUNC:
                doCallFunc(node);
                break;
            case RETURN:
                return doReturn(node);
        }
        return JUMPTYPE.NOJUMP;
    }

    /**
     * 返回值存入rax
     * @param root
     * @return
     */
    private JUMPTYPE doReturn(SATreeNode root) {
        Val val = null;
        if(root.children.size()==1){
            midProcess.add("$rax.Val=null");
            rax = new Val();
        }else{
            val = getExpression(root.children.get(1));
            rax = val;
            midProcess.add("$rax.Val="+val.name+".Val");
        }
        return JUMPTYPE.RETURN;
    }

    /**
     * 执行函数调用
     * @param root
     */
    private Val doCallFunc(SATreeNode root) {
        SATreeNode identifier = root.children.get(0);
        SATreeNode valNode = root.children.get(2);
        String func = identifier.word.word;
        ArrayList<Val> valList = new ArrayList<>();
        //获取参数
        getVals(valNode,valList);
        //获取函数起始节点
        FuncDef funcDef = getFunc(func,valList);
        if(funcDef==null){
            String params = "[";
            for(int i=0;i<valList.size();i++){
                params=params+valList.get(i).type;
            }
            params = params+"]";
            ErrorExecutor.showError("SEA","未定义函数："+func+" 参数列表为："+params,
                    identifier.word.line,identifier.word.position);
        }
        SATreeNode execute = funcDef.funcNode;
        //参数传递
        ValTrans(valList,funcDef.valList);
        midProcess.add("Call func:"+funcDef.name+" Enter address:"+funcDef.funcNode.hashCode());
        //执行函数，变量入栈，执行代码块，返回值存入临时寄存器，变量出栈
        valTable.add(valList);
        doBlock(execute,valList);
        valTable.remove(valTable.size()-1);
        return rax;
    }

    /**
     * 参数传递
     * @param valList
     * @param valList1
     */
    private void ValTrans(ArrayList<Val> valList, List<Val> valList1) {
        for(int i=0;i<valList.size();i++){
            Val val1 = valList.get(i);
            Val val2 = valList1.get(i);
            val1.name=val2.name;
        }
    }

    /**
     * 搜索匹配函数
     * @param func
     * @param table
     * @return
     */
    private FuncDef getFunc(String func, ArrayList<Val> table) {
        FuncDef callFunc = new FuncDef(func,table,null);
        for(int i=0;i<funcDefList.size();i++){
            FuncDef funcDef = funcDefList.get(i);
            if(compareFunc(callFunc,funcDef)) {
                return funcDef;
            }
        }
        return null;
    }

    /**
     * 获取传递参数
     * @param root
     * @return
     */
    private void getVals(SATreeNode root,ArrayList<Val> table) {
        if(root.children.size()==0) return;
        SATreeNode variable = root.children.get(0);
        SATreeNode restVariable = root.children.get(1);
        getRestVariable(variable,restVariable,table);
    }

    private void getRestVariable(SATreeNode variable, SATreeNode restVal,ArrayList<Val> table) {
        Val copy = new Val();
        Val value = getVariable(variable);
        copy.name=value.name;
        switch (value.type){
            case INUMBER:
                copy.type=EWordKind.INUMBER;
                copy.value=Integer.parseInt(value.value.toString());
                break;
            case FNUMBER:
                copy.type=EWordKind.FNUMBER;
                copy.value=Double.parseDouble(value.value.toString());
                break;
            case CHARACTER:
                copy.type=EWordKind.CHARACTER;
                copy.value=value.value;
                break;
        }
        table.add(copy);
        if(restVal.children.size()!=0){
            SATreeNode v = restVal.children.get(1);
            SATreeNode r = restVal.children.get(2);
            getRestVariable(v,r,table);
        }
    }

    /**
     * Jump语义执行
     * @param root
     * @return
     */
    private JUMPTYPE doJump(SATreeNode root) {
        SATreeNode node = root.children.get(0);
        if(node.word.type == EWordKind.BREAK){
            midProcess.add("<Break>"+"第"+node.word.line+"行 第"+node.word.position+"处");
            return JUMPTYPE.BREAK;
        }
        if(node.word.type == EWordKind.CONTINUE){
            midProcess.add("<Continue>"+"第"+node.word.line+"行 第"+node.word.position+"处");
            return JUMPTYPE.CONTINUE;
        }
        return JUMPTYPE.NOJUMP;
    }

    /**
     * Print语义执行
     * @param root
     */
    private void doOut(SATreeNode root) {
        SATreeNode node = root.children.get(2);
        Val value = new Val();
        if(node.type== EXPRESSION) {
            value = getExpression(node);
        }else{
            //获取字符串
            value.type = EWordKind.STRING;
            String s = node.word.word;
            s = s.substring(1,s.length()-1);
            value.value = s;
        }
        midProcess.add("Out.Val ="+value.value.toString());
        System.out.println(value.value.toString());
    }

    /**
     * Scan语义执行
     * @param root
     */
    private void doIn(SATreeNode root) {
        SATreeNode node = root.children.get(2);
        SATreeNode identifier = node.children.get(0);
        int length=1;
        Val val = null;
        if(node.children.size()==4){
            SATreeNode index = node.children.get(2);
            val = getExpression(index);
            if(val.type!=EWordKind.INUMBER){
                ErrorExecutor.showError("SEA","数组大小应该为整数",index.word.line,index.word.position);
                length=0;
                return;
            }
            length = Integer.parseInt(val.value.toString())+1;
            if(length<0){
                ErrorExecutor.showError("SEA","数组大小应该为非负数",index.word.line,index.word.position);
                length=0;
                return;
            }
        }
        Scanner scan = new Scanner(System.in);
        String input = scan.next();
        changeVariable(identifier.word.word,input,length,identifier.word.line,identifier.word.position);
        midProcess.add(identifier.word.word+".Val = "+input);
    }

    /**
     * Declaration语义执行
     * @param root
     */
    private void doDeclaration(SATreeNode root, ArrayList<Val> table) {
        SATreeNode type = root.children.get(0);
        doRestVariable(root,table,type.word.type);
        midProcess.add("LocalScope.add(declaration.ValList);");
    }

    /**
     * RestVariable语义执行
     * @param root
     * @param table
     * @param type
     */
    private void doRestVariable(SATreeNode root, ArrayList<Val> table, EWordKind type){
        if(root.children.size()<1) return;
        SATreeNode define = root.children.get(0);
        SATreeNode variable = root.children.get(1);
        SATreeNode restVariable = root.children.get(2);
        Val val = new Val();
        int length = 1;
        //检查是否存在下标
        if(variable.children.size()>1){
            SATreeNode index = variable.children.get(2);
            Val ind = getExpression(index);
            if(ind.type!=EWordKind.INUMBER){
                ErrorExecutor.showError("SEA","数组大小应该为整数",index.word.line,index.word.position);
                length=0;
                return;
            }
            length = Integer.parseInt(ind.value.toString());
            if(length<=0){
                ErrorExecutor.showError("SEA","数组大小应该为正数",index.word.line,index.word.position);
                length=0;
                return;
            }
        }

        //翻译类型
        EWordKind trans=EWordKind.WRONG;
        switch (type){
            case INT:
                trans=EWordKind.INUMBER;
                break;
            case REAL:
                trans=EWordKind.FNUMBER;
                break;
            case CHAR:
                trans=EWordKind.CHARACTER;
                break;
            default:
                ErrorExecutor.showError("SEA","未知类型",define.word.line,define.word.position);
        }

        //添加变量同时进行变量名检查
        SATreeNode identifier = variable.children.get(0);
        boolean ok = true;
        for(int i=0;i<table.size();i++){
            Val tmp = table.get(i);
            if(tmp.name.equals(identifier.word.word)){
                ErrorExecutor.showError("SEA","重复定义",define.word.line,define.word.position);
                ok=false;
                break;
            }
        }
        midProcess.add("for i in range("+length+")");
        midProcess.add("\tvariables.NameArray.add(variable.Name)");
        if(ok) {
            for (int i = 0; i < length; i++) {
                Val tmp = new Val();
                tmp.type = trans;
                tmp.value = 0;
                tmp.name = identifier.word.word;
                table.add(tmp);
            }
        }
        midProcess.add("declaration.ValList.add(variables);");
        doRestVariable(restVariable,table,type);

    }

    /**
     * Assignment语义执行
     * @param root
     */
    private void doAssignment(SATreeNode root) {
        SATreeNode variable = root.children.get(0);
        SATreeNode expression = root.children.get(2);
        Val value = getExpression(expression);
        int length = 1;
        //检查是否存在下标
        if(variable.children.size()>1){
            SATreeNode index = variable.children.get(2);
            Val val = getExpression(index);
            if(val.type!=EWordKind.INUMBER){
                ErrorExecutor.showError("SEA","数组大小应该为整数",index.word.line,index.word.position);
                length=0;
                return;
            }
            length = Integer.parseInt(val.value.toString())+1;

            if(length<0){
                ErrorExecutor.showError("SEA","数组大小应该为非负数",index.word.line,index.word.position);
                length=0;
                return;
            }
        }
        SATreeNode identifier = variable.children.get(0);
        //修改对应的值
        changeVariable(identifier.word.word,value,length,identifier.word.line,identifier.word.position);
        midProcess.add(identifier.word.word+".Val = "+value.value.toString());
    }

    /**
     * if else语义执行
     * @param root
     * @return
     */
    private JUMPTYPE doIfElse(SATreeNode root,ArrayList<Val> table) {

        SATreeNode condition = root.children.get(2);
        SATreeNode ifPart = root.children.get(4);
        JUMPTYPE isJump;

        if(getCondition(condition)){
            midProcess.add("<if-block>");
            midProcess.add("goto out;");
            //if part scope
            if(ifPart.type== STATEMENT) {
                isJump = doStatement(ifPart,table);
            }else{
                isJump = doBlock(ifPart,table);
            }
        }else{
            midProcess.add("<else-block>");
            midProcess.add("goto out;");
            if(root.children.size()>5){
                SATreeNode elsePart = root.children.get(6);
                //else part scope
                if(elsePart.type== STATEMENT) {
                    isJump = doStatement(elsePart,table);
                }else{
                    isJump = doBlock(elsePart,table);
                }
            }else{
                return JUMPTYPE.NOJUMP;
            }
        }
        midProcess.add("out:;");
        return isJump;
    }

    /**
     * while语义执行
     * @param node
     */
    private JUMPTYPE doWhile(SATreeNode node,ArrayList<Val> table) {
        SATreeNode condition = node.children.get(2);
        SATreeNode whilePart = node.children.get(4);
        JUMPTYPE jumptype=JUMPTYPE.NOJUMP;
        midProcess.add("block.EnterPoint = loop;");
        midProcess.add("loop:");
        while(getCondition(condition)){
            if(whilePart.type==STATEMENT){
                if((jumptype=doStatement(whilePart,table))==JUMPTYPE.BREAK||jumptype==JUMPTYPE.RETURN){
                    break;
                }
            }else{
                if((jumptype=doBlock(whilePart,table))==JUMPTYPE.BREAK||jumptype==JUMPTYPE.RETURN) {
                    midProcess.add("while-loop.returnType = block.returnType;");
                    break;
                }
                midProcess.add("<while-block>;");
            }
            midProcess.add("if(while-loop.returnType==break||while-loop.returnType==return) goto out;");
            midProcess.add("goto loop;");
        }
        midProcess.add("out:;");
        return jumptype;
    }

    /**
     * 处理Block
     * @param root
     * @param table
     * @return
     */
    private JUMPTYPE doBlock(SATreeNode root, ArrayList<Val> table){
        JUMPTYPE isJump;
        for(int i=1;i<root.children.size()-1;i++){
            SATreeNode node = root.children.get(i);
            if((isJump=doStatement(node,table))==JUMPTYPE.BREAK)
                return JUMPTYPE.BREAK;
            else if(isJump==JUMPTYPE.CONTINUE)
                return JUMPTYPE.CONTINUE;
            else if(isJump==JUMPTYPE.RETURN)
                return JUMPTYPE.RETURN;
        }
        return JUMPTYPE.NOJUMP;
    }

    /**
     * 获取Expression的值
     * @param root
     * @return
     */
    private Val getExpression(SATreeNode root){
        SATreeNode node = root.children.get(0);
        Val value = new Val();
        if(node.type== TOKEN){
            value.type = EWordKind.CHARACTER;
            if(node.word.word.charAt(1)=='\\'){
                switch (node.word.word){
                    case "'\\t'":
                        value.value = Character.valueOf('\t');
                        break;
                    case "'\\n'":
                        value.value = Character.valueOf('\n');
                        break;
                    case "'\\r'":
                        value.value = Character.valueOf('\r');
                        break;
                }
            }else{
                value.value = Character.valueOf(node.word.word.charAt(1));
            }
        }else{
            value = getCalculation(node);
        }
        return value;
    }

    /**
     * 获取Calculation的值
     * @param root
     * @return
     */
    private Val getCalculation(SATreeNode root) {
        SATreeNode item1 = root.children.get(0);
        SATreeNode calc;
        Val value1 = getItem(item1);
        if(root.children.size()>1){
            Val result = new Val();
            calc = root.children.get(2);
            Val value2 = getCalculation(calc);
            SATreeNode operation = root.children.get(1);
            //类型转换，char类型转为int
            value1=typeTrans(value1);
            value2=typeTrans(value2);
            if(value1.type==EWordKind.FNUMBER||value2.type==EWordKind.FNUMBER){
                result.type = EWordKind.FNUMBER;
                if(operation.word.type==EWordKind.PLUS) {
                    result.value = Double.parseDouble(value1.value.toString())
                            + Double.parseDouble(value2.value.toString());
                }
                else{
                    result.value = Double.parseDouble(value1.value.toString())
                            - Double.parseDouble(value2.value.toString());
                }
            }else{
                result.type = EWordKind.INUMBER;
                if(operation.word.type==EWordKind.PLUS) {
                    result.value = Integer.parseInt(value1.value.toString())
                            + Integer.parseInt(value2.value.toString());
                }
                else{
                    result.value = Integer.parseInt(value1.value.toString())
                            - Integer.parseInt(value2.value.toString());
                }
            }
            return result;
        }else{
            return value1;
        }
    }

    /**
     * 获取item的值
     * @param root
     * @return
     */
    private Val getItem(SATreeNode root) {
        SATreeNode factor = root.children.get(0);
        SATreeNode item;
        Val value1 = getFactor(factor);
        if(root.children.size()>1){
            Val result = new Val();
            item = root.children.get(2);
            Val value2 = getItem(item);
            SATreeNode operation = root.children.get(1);
            //类型转换，char类型转为int
            value1=typeTrans(value1);
            value2=typeTrans(value2);
            if(value1.type==EWordKind.FNUMBER||value2.type==EWordKind.FNUMBER){
                result.type = EWordKind.FNUMBER;
                if(operation.word.type==EWordKind.ASTERISK) {
                    result.value = Double.parseDouble(value1.value.toString())
                            * Double.parseDouble(value2.value.toString());
                }
                else{
                    if(Double.parseDouble(value2.value.toString())==0){
                        ErrorExecutor.showError("SEA","除数不为0",item.word.line,item.word.position);
                        result.value = new Double(0);
                    }
                    else result.value = Double.parseDouble(value1.value.toString())
                            / Double.parseDouble(value2.value.toString());
                }
            }else{
                result.type = EWordKind.INUMBER;
                if(operation.word.type==EWordKind.ASTERISK) {
                    result.value = Integer.parseInt(value1.value.toString())
                            * Integer.parseInt(value2.value.toString());
                }
                else{
                    if(Integer.parseInt(value2.value.toString())==0){
                        ErrorExecutor.showError("SEA","除数不为0",item.word.line,item.word.position);
                        result.value = new Double(0);
                    }
                    else result.value = Integer.parseInt(value1.value.toString())
                            / Integer.parseInt(value2.value.toString());
                }
            }
            return result;
        }else{
            return value1;
        }
    }

    /**
     * 类型转换
     * @param value
     * @return
     */
    private Val typeTrans(Val value) {
        if(value.type!=EWordKind.CHARACTER)
            return value;
        else{
            Val target = new Val();
            target.type=EWordKind.INT;
            int tmp = value.value.toString().charAt(0);
            target.value=Integer.valueOf(tmp);
            return target;
        }
    }

    /**
     * 获取factor的值
     * @param root
     * @return
     */
    private Val getFactor(SATreeNode root) {
        if(root.children.size()==1){
            SATreeNode node = root.children.get(0);
            if(node.type== VARIABLE){
                return getVariable(node);
            }else if(node.type==NUMBER){
                return getNumber(node);
            }else{
                return doCallFunc(node);
            }
        }else{
            return getCalculation(root.children.get(1));
        }
    }

    /**
     * 获取number的值
     * @param root
     * @return
     */
    private Val getNumber(SATreeNode root) {
        if(root.children.size()>1){
            SATreeNode number = root.children.get(1);
            SATreeNode pm = root.children.get(0);
            Val val = getNumber(number);
            if(pm.word.type==EWordKind.MINUS){
                if(val.type==EWordKind.INUMBER){
                    if((Integer)val.value<0)
                        val.value = Integer.parseInt(val.value.toString().substring(1));
                    else
                        val.value = Integer.parseInt("-"+val.value.toString());
                }else{
                    if((Double)val.value<0)
                        val.value = Double.parseDouble(val.value.toString().substring(1));
                    else
                        val.value = Double.parseDouble("-"+val.value.toString());
                }
            }
            return val;
        }else{
            SATreeNode node = root.children.get(0);
            Val val = new Val();
            if(node.word.type==EWordKind.INUMBER){
                val.type = EWordKind.INUMBER;
                val.value = Integer.parseInt(node.word.word);
            }else{
                val.type = EWordKind.FNUMBER;
                val.value = Double.parseDouble(node.word.word);
            }
            return val;
        }
    }


    /**
     * 获取Variable的值
     * @param root
     * @return
     */
    private Val getVariable(SATreeNode root) {
        SATreeNode identifier = root.children.get(0);
        int length = 0;
        Val ind = null;
        if(root.children.size()>1){
            SATreeNode index = root.children.get(2);
            ind = getExpression(index);
            if(ind.type!=EWordKind.INUMBER){
                ErrorExecutor.showError("SEA","数组大小应该为整数",index.word.line,index.word.position);
                length=0;
            }
            length = Integer.parseInt(ind.value.toString());
            if(length<0){
                ErrorExecutor.showError("SEA","数组大小应该为非负数",index.word.line,index.word.position);
                length=0;
            }
            midProcess.add("variable.Name = "+identifier.word.word);
            midProcess.add("variable.Val = "+ind.value.toString());
        }
        return getVariable(identifier.word.word,length,identifier.word.line,identifier.word.position);
    }

    /**
     * 获取Condition的值
     * @param root
     * @return
     */
    private boolean getCondition(SATreeNode root){
        SATreeNode item1 = root.children.get(0);
        SATreeNode con;
        boolean value1 = getSubCon(item1);
        if(root.children.size()>1){
            Val result = new Val();
            con = root.children.get(2);
            boolean value2 = getCondition(con);
            SATreeNode operation = root.children.get(1);
            if(operation.word.type==EWordKind.AND) {
                midProcess.add("SubCon.Val && Condition.Val");
                return value1 && value2;
            }
            else {
                midProcess.add("SubCon.Val || Condition.Val");
                return value1||value2;
            }
        }else{
            midProcess.add("Condition.Val");
            return value1;
        }
    }

    /**
     * 获取SubCon的值
     * @param root
     * @return
     */
    private boolean getSubCon(SATreeNode root) {
        SATreeNode exp1 = root.children.get(0);
        SATreeNode relation = root.children.get(1);
        SATreeNode exp2 = root.children.get(2);
        Val value1 = typeTrans(getExpression(exp1));
        Val value2 = typeTrans(getExpression(exp2));
        Double val1 = Double.parseDouble(value1.value.toString());
        Double val2 = Double.parseDouble(value2.value.toString());
        midProcess.add(value1.name+relation.word.word+value2.name);
        switch (relation.word.type){
            case SMALLER:
                return val1<val2;
            case SMALLERE:
                return val1<=val2;
            case BIGGER:
                return val1>val2;
            case BIGGERE:
                return val1>=val2;
            case NEQUAL:
                return val1!=val2;
            case EQUAL:
                if(Math.abs(val1-val2)<1e-8)
                return true;
        }
        return false;
    }


    /**
     * 遍历修改最近定义域的符号表中的值
     * @param identifier
     * @param val
     * @param length
     */
    private void changeVariable(String identifier, Val val, int length, int line, int position){
        String input = val.value.toString();
        for(int i=valTable.size()-1;i>=0;i--){
            ArrayList<Val> scope = valTable.get(i);
            for(int j=0;j<scope.size();j++){
                Val tmp = scope.get(j);
                if(tmp!=null&&tmp.name.equals(identifier)){
                    if(j+length-1>=scope.size()){
                        ErrorExecutor.showError("SEA","数组越界",line,position);
                        return;
                    }
                    Val value = scope.get(j+length-1);
                    try {
                        if (value != null && value.name.equals(identifier)) {
                            if(value.type!=EWordKind.CHARACTER&&val.type!=EWordKind.CHARACTER){
                                //高低转换
                                if(value.type==EWordKind.FNUMBER){
                                    value.value = Double.parseDouble(input);
                                }else{
                                    if(val.type==EWordKind.FNUMBER){
                                        //精度误差报错
                                        ErrorExecutor.showError("SEA", "精度丢失", line, position);
                                    }else{
                                        value.value = Integer.parseInt(input);
                                    }
                                }
                            }else{
                                if(value.type==EWordKind.CHARACTER){
                                    if(val.type==EWordKind.CHARACTER){
                                        value.value = val.value;
                                    }else{
                                        //类型转换异常
                                        ErrorExecutor.showError("SEA",
                                                "Char类型变量不接受整数或浮点数数类型", line, position);
                                    }
                                }else{
                                    //类型转换异常
                                    ErrorExecutor.showError("SEA", "精度丢失", line, position);
                                }
                            }
                            return;
                        } else {
                            //越界
                            ErrorExecutor.showError("SEA", "数组越界", line, position);
                            return;
                        }
                    }catch (Exception e){
                        //类型转换错误
                        ErrorExecutor.showError("SEA", "类型转换错误", line, position);
                    }
                }
            }
            //全局域中查找
            i=1;
        }
        //没找到
        ErrorExecutor.showError("SEA","未定义符号: "+identifier,line,position);
    }

    /**
     * 从输入中读取，进行类型转换
     * @param identifier
     * @param input
     * @param length
     * @param line
     * @param position
     */
    private void changeVariable(String identifier, String input, int length, int line, int position){
        for(int i=valTable.size()-1;i>=0;i--){
            ArrayList<Val> scope = valTable.get(i);
            for(int j=0;j<scope.size();j++){
                Val tmp = scope.get(j);
                if(tmp!=null&&tmp.name.equals(identifier)){
                    if(j+length-1>=scope.size()){
                        ErrorExecutor.showError("SEA","数组越界",line,position);
                        return;
                    }
                    Val value = scope.get(j+length-1);
                    try {
                        if (value != null && value.name.equals(identifier)) {
                            if (value.type == EWordKind.INUMBER) {
                                value.value = Integer.parseInt(input);
                            } else if (value.type == EWordKind.FNUMBER) {
                                value.value = Double.parseDouble(input);
                            } else if (value.type == EWordKind.CHARACTER) {
                                value.value = (char) input.charAt(0);
                            }
                            return;
                        } else {
                            //越界
                            ErrorExecutor.showError("SEA", "数组越界", line, position);
                            return;
                        }
                    }catch (Exception e){
                        //类型转换错误
                        ErrorExecutor.showError("SEA", "类型转换错误", line, position);
                    }
                }
            }
            //全局域中查找
            i=1;
        }
        //没找到
        ErrorExecutor.showError("SEA","未定义符号: "+identifier,line,position);
    }

    /**
     * 遍历查找最近定义域的符号表中的对应值
     * @param identifier
     * @param index
     * @param line
     * @param position
     * @return
     */
    private Val getVariable(String identifier, int index, int line, int position){
        for(int i=valTable.size()-1;i>=0;i--){
            if(i!=0&&i!=valTable.size()-1) continue;
            ArrayList<Val> scope = valTable.get(i);
            for(int j=0;j<scope.size();j++){
                Val tmp = scope.get(j);
                if(tmp!=null&&tmp.name.equals(identifier)){
                    if(j+index>=scope.size()){
                        ErrorExecutor.showError("SEA","数组越界",line,position);
                        return null;
                    }
                    Val value = scope.get(j+index);
                    if(value!=null&&value.name.equals(identifier)){
                        return value;
                    }else{
                        //越界
                        ErrorExecutor.showError("SEA","数组越界",line,position);
                        return null;
                    }
                }
            }
        }
        ErrorExecutor.showError("SEA","未定义变量:"+identifier,line,position);
        return null;
    }
}
