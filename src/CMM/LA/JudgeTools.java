package CMM.LA;

import CMM.util.EAWordKind;
import CMM.util.EWordKind;

import java.math.BigDecimal;
import java.security.interfaces.ECKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JudgeTools {
    /**
     * 判断单词是否为合法变量名
     * @param word
     * @return
     */
    public static EWordKind isIdentifier(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (i == 0 &&(c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) return EWordKind.WRONG;
            if (!((c <= '9' && c >= '0') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'))
                return EWordKind.WRONG;
            if(i==word.length()-1&&c=='_')
                return EWordKind.WRONG;
        }
        return EWordKind.IDENTIFIER;
    }

    /**
     * 判断单词是否为合法浮点数字
     * @param str
     * @return
     */
    public static EWordKind isFNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+");
        String bigStr;
        try {
            bigStr = new BigDecimal(str).toString();
        } catch (Exception e) {
            return EWordKind.WRONG;//异常 说明包含非数字。
        }

        Matcher isNum = pattern.matcher(bigStr); // matcher是全匹配
        if (!isNum.matches()) {
            return EWordKind.WRONG;
        }
        return EWordKind.FNUMBER;
    }

    /**
     * 判断单词是否为合法整型数字
     * @param str
     * @return
     */
    public static EWordKind isINumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]+");
        String bigStr;
        try {
            bigStr = new BigDecimal(str).toString();
        } catch (Exception e) {
            return EWordKind.WRONG;//异常 说明包含非数字。
        }

        Matcher isNum = pattern.matcher(bigStr); // matcher是全匹配
        if (!isNum.matches()) {
            return EWordKind.WRONG;
        }
        return EWordKind.INUMBER;
    }

    /**
     * 判断单词是否为关键字
     * @param word
     * @return
     */
    public static EWordKind isKeyWord(String word){
        switch (word){
            case "if":
                return EWordKind.IF;
            case "return":
                return EWordKind.RETURN;
            case "def":
                return EWordKind.DEF;
            case "else":
                return EWordKind.ELSE;
            case "while":
                return EWordKind.WHILE;
            case "for":
                return EWordKind.FOR;
            case "break":
                return EWordKind.BREAK;
            case "continue":
                return EWordKind.CONTINUE;
            case "print":
                return EWordKind.PRINT;
            case "scan":
                return EWordKind.SCAN;
            case "+":
                return EWordKind.PLUS;
            case "-":
                return EWordKind.MINUS;
            case "*":
                return EWordKind.ASTERISK;
            case "/":
                return EWordKind.DIVIDE;
            case "//":
                return EWordKind.COMMENT;
            case "/*":
                return EWordKind.LCOMMENT;
            case "*/":
                return EWordKind.RCOMMENT;
            case "\\t":
                return EWordKind.TAB;
            case "\\n":
                return EWordKind.NEWLINE;
            case "\\r":
                return EWordKind.ENTER;
            case "(":
                return EWordKind.LBRACKET;
            case ")":
                return EWordKind.RBRACKET;
            case "<":
                return EWordKind.SMALLER;
            case ">":
                return EWordKind.BIGGER;
            case "=":
                return EWordKind.ASSIGN;
            case "==":
                return EWordKind.EQUAL;
            case "<>":
                return EWordKind.NEQUAL;
            case "<=":
                return EWordKind.SMALLERE;
            case ">=":
                return EWordKind.BIGGERE;
            case ";":
                return EWordKind.BRANCH;
            case "'":
                return EWordKind.SQUOTE;
            case "\"":
                return EWordKind.DQUOTE;
            case "int":
                return EWordKind.INT;
            case "real":
                return EWordKind.REAL;
            case "char":
                return EWordKind.CHAR;
            case "[":
                return EWordKind.LARRAY;
            case "]":
                return EWordKind.RARRAY;
            case "{":
                return EWordKind.LBRACES;
            case "}":
                return EWordKind.RBRACES;
            case "&&":
                return EWordKind.AND;
            case "||":
                return EWordKind.OR;
            case ",":
                return EWordKind.COMMA;
            default:
                return EWordKind.WRONG;
        }
    }

    public static EWordKind isCharacter(String word) {
        if(word.charAt(0)=='\''&&word.charAt(word.length()-1)=='\''){
            return EWordKind.CHARACTER;
        }else{
            return EWordKind.WRONG;
        }
    }

    public static EWordKind isString(String word) {
        if(word.charAt(0)=='"'&&word.charAt(word.length()-1)=='"'){
            return EWordKind.STRING;
        }else{
            return EWordKind.WRONG;
        }
    }
}
