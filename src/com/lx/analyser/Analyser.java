package com.lx.analyser;

import com.lx.analyser.instruction.Constant;
import com.lx.analyser.instruction.Function;
import com.lx.analyser.instruction.Operation;
import com.lx.error.AnalyserException;
import com.lx.error.NotFitException;
import com.lx.tokenizer.Token;
import com.lx.util.Expression;
import com.lx.util.TokenReader;

import java.util.ArrayList;
import java.util.HashMap;

import static com.lx.tokenizer.Type.*;

public class Analyser {
    private TokenReader reader;

    private int currentParamNum;

    private HashMap<String,Integer> currentFuncVarIndexs;
    private int currentFuncStackIndex;
    private ArrayList<Operation> currentOperations;//记得重新new

    private static HashMap<String,Object> functionIndex = new HashMap<>();

    private static HashMap<String,Object> globalVarIndex = new HashMap<>();
    private HashMap<String,Object> currentFuncConstantsIndex;//指函数栈的下标

    private static ArrayList<Operation> starts = new ArrayList<>();

    private static ArrayList<Function> functions = new ArrayList<>();


    private static HashMap<String,Object> globalConstantsIndex = new HashMap<>();//指.constant里的下标
    private static ArrayList<Constant> constants = new ArrayList<>();
    private boolean currentConstFlag = false;
//    private static int constantsIndex = 0;
    private static int startIndex = 0;
    private static int functionsIndex = 0;
    private Expression expr = Expression.getExpresser();
    //每个函数的局部变量用局部的hashmap存，指令的index也是
    private static int stackIndex = 0;//全局栈位置
    private static boolean isStart = true;
    public Analyser(TokenReader reader) {
        this.reader = reader;
    }

//.constants:
//0 S "fun"
//.start:
//0    bipush 42
//.functions:
//0 0 1 1                   # .F0 fun
//.F0: #fun
//0    loada 0, 0
//.F1: #main
//0    loadc 4
    public void writeOut(){
        System.out.println(".constants:");
        for (Constant each:constants){
            System.out.println(each.toString());
        }
        System.out.println(".start:");
        for(Operation each:starts){
            System.out.println(each.toString());
        }
        System.out.println(".functions:");
        for(Function each:functions){
            System.out.println(each.toString());
        }
        for (Function each:functions){
            System.out.println(".F"+each.index+":");
            for(Operation eachOperation:each.operations){
                System.out.println(eachOperation.toString());
            }
        }
    }


//<C0-program> ::= {<variable-declaration>}{<function-definition>}
//    <variable-declaration> ::=  [<const-qualifier>]<type-specifier><init-declarator-list>';'
//<simple-type-specifier>  ::= 'void'|'int'
//    <function-definition> ::= <type-specifier><identifier><parameter-clause><compound-statement>
    //    有或的情况，判断first集：const void int
    public void c0_program() throws AnalyserException {
        boolean varFinish = false;
        Token currentToken=reader.readToken();
        if(currentToken==null)
            throw new AnalyserException("没有可以识别的Token");
        if(!(currentToken.getType()==CONST_QUALIFIER||currentToken.getType()==SIMPLE_TYPE_SPECIFIER)){
            throw new AnalyserException("起始位置非法");
        }
        reader.unreadToken(currentToken);
//
        if(currentToken.getType()==CONST_QUALIFIER){
            try{
                variable_declaration();
            }
            catch (NotFitException e){
                throw  new AnalyserException("const定义错误");
            }
        }
        else {
            try{
                variable_declaration();
            }
            catch (NotFitException e){
                //说明只能是function
                try{
                    function_definition();
                    varFinish = true;
                }
                catch (NotFitException e1){
                    e1.printStackTrace();
                    throw  new AnalyserException("开头定义错误");
                }
                catch (NullPointerException e2){
                    e2.printStackTrace();
                }
            }
        }
        while (!varFinish){
            try{
                variable_declaration();
            }
            catch (NotFitException e){
//                varFinish=true;
            }
        }
        while (true){
            try{
                function_definition();
            }
            catch (NotFitException e){
                break;
            }
            catch (NullPointerException e2){
                e2.printStackTrace();
                break;
            }
        }
        currentToken = reader.readToken();
        if(currentToken!=null)
            throw new RuntimeException(currentToken.toString());
        writeOut();
    }
//<variable-declaration> ::=
//    [<const-qualifier>]<type-specifier><init-declarator-list>';'
//    <init-declarator-list> ::=
//    <init-declarator>{','<init-declarator>}
//<init-declarator> ::=
//    <identifier>[<initializer>]
//<initializer> ::=
//    '='<expression>
    public void variable_declaration() throws NotFitException {
        currentConstFlag = false;
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        if(currentToken.getType()==CONST_QUALIFIER) {
            currentConstFlag = true;
            currentToken = reader.readToken();
        }
        if(currentToken.getType()!=SIMPLE_TYPE_SPECIFIER){
            reader.pushBackTokens();
            throw new NotFitException("<variable-declaration>中识别不到合法的<type-specifier>");
        }
        if(!currentToken.getValue().equals("int")){
            reader.pushBackTokens();
            throw new NotFitException("void不能声明变量");
        }
        currentToken=reader.readToken();
        if(currentToken.getType()!=IDENTIFIER){
            reader.pushBackTokens();
            throw new NotFitException("<variable-declaration>中识别不到合法的<init-declarator>");
        }
        String idenName = (String) currentToken.getValue();//变量名
        currentToken=reader.readToken();
        if(currentToken.getType()!=ASSIGNMENT_OPERATOR){
            Token simi = reader.readToken();
            if(simi.getType()!=SEMICOLON){
                reader.pushBackTokens();
                throw new NotFitException("");
            }
            else {
                reader.unreadToken(simi);
            }
            reader.unreadToken(currentToken);
            //此时比如定义了个int a, 是start的话，如果是const，就存.constants，start里用loadc,不是const就bipush
            if(isStart){
                if(currentConstFlag){
                    throw new RuntimeException("const变量必须显式初始化");
                }
                else {
                    //直接存到.start
                    int index = starts.size();
                    starts.add(new Operation(index,"ipush","0",null));
                    globalVarIndex.put(idenName,stackIndex);
                    stackIndex++;
                }
            }
            else {
                //在函数里
                if(currentConstFlag){
                    throw new RuntimeException("const变量必须显式初始化");
                }
                else {
                    int index = currentOperations.size();
                    currentOperations.add(new Operation(index,"ipush",0,null));
                    currentFuncVarIndexs.put(idenName,currentFuncStackIndex);
                    currentFuncStackIndex++;
                    //默认为0
                }
            }
        }
        else {//有赋值语句
            try{
                if(!(currentConstFlag&&isStart))
                    expression();//计算完后index-1处存结果
            }
            catch (NotFitException e){
                throw new NotFitException("<variable-declaration>中识别不到合法的<expression>");
            }
            if(currentConstFlag){
                if(isStart){
                    //这里的constant计算由编译器完成
                    String exprStr = "";
                    while (currentToken.getType()!=SEMICOLON&&currentToken.getType()!=COMMA){
                        exprStr+=currentToken.getValue();
                        currentToken = reader.readToken();
                    }
                    reader.unreadToken(currentToken);
                    String result;
                    try {
                        result = expr.calculate(exprStr).toString();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        throw new NotFitException("表达式不正确");
                    }
                    int index = constants.size();
                    constants.add(new Constant(index,'I',result));
                    globalConstantsIndex.put(idenName,index);
                }
                else {
                    //函数里的const，已经由expression计算完成放到栈顶
                    currentFuncConstantsIndex.put(idenName,currentFuncStackIndex-1);
                }
            }
            else {//已经赋值的，不是constantd的
                if(isStart){
                    //int a = 3+3;
                    globalVarIndex.put(idenName,stackIndex-1);
                }
                else {
                    currentFuncVarIndexs.put(idenName,currentFuncStackIndex-1);
                }
            }
        }
        currentToken = reader.readToken();
        Token temp = currentToken;
        while(currentToken.getType()==COMMA){
            try{
                init_declarator();
                currentToken = reader.readToken();
                if(currentToken.getType()!=COMMA){
                    reader.unreadToken(currentToken);
                    return;
                }
                temp = currentToken;
            }
            catch (NotFitException e){
                reader.unreadToken(temp);
                return;
            }
        }
//        <init-declarator-list> ::=
//        <init-declarator>{','<init-declarator>}
//        <init-declarator> ::=
//        <identifier>[<initializer>]
//        <initializer> ::=
//        '='<expression>
        //a=3,b=4,c=a+b
    }
    public void init_declarator() throws NotFitException{
        Token currentToken=reader.readToken();
        if(currentToken.getType()!=IDENTIFIER){
            reader.unreadToken(currentToken);
            throw new NotFitException("<variable-declaration>中识别不到合法的<init-declarator>");
        }
        String idenName = (String) currentToken.getValue();//变量名
        currentToken=reader.readToken();
        if(currentToken.getType()!=ASSIGNMENT_OPERATOR){
            reader.unreadToken(currentToken);
//此时比如定义了个int a, 是start的话，如果是const，就存.constants，start里用loadc,不是const就bipush
            if(isStart){
                if(currentConstFlag){
                    throw new RuntimeException("const变量必须显式初始化");
                }
                else {
                    //直接存到.start
                    int index = starts.size();
                    starts.add(new Operation(index,"ipush","0",null));
                    globalVarIndex.put(idenName,stackIndex);
                    stackIndex++;
                }
            }
            else {
                //在函数里
                if(currentConstFlag){
                    throw new RuntimeException("const变量必须显式初始化");
                }
                else {
                    int index = currentOperations.size();
                    currentOperations.add(new Operation(index,"ipush",0,null));
                    currentFuncVarIndexs.put(idenName,currentFuncStackIndex);
                    currentFuncStackIndex++;
                    //默认为0
                }
            }
        }
        else {//有赋值语句
            try{
                if(!(currentConstFlag&&isStart))
                    expression();//计算完后index-1处存结果
            }
            catch (NotFitException e){
                throw new NotFitException("<variable-declaration>中识别不到合法的<expression>");
            }
            if(currentConstFlag){
                if(isStart){
                    //这里的constant计算由编译器完成
                    String exprStr = "";
                    while (currentToken.getType()!=SEMICOLON&&currentToken.getType()!=COMMA){
                        exprStr+=currentToken.getValue();
                        currentToken = reader.readToken();
                    }
                    reader.unreadToken(currentToken);
                    String result;
                    try {
                        result = expr.calculate(exprStr).toString();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        throw new NotFitException("表达式不正确");
                    }
                    int index = constants.size();
                    constants.add(new Constant(index,'I',result));
                    globalConstantsIndex.put(idenName,index);
                }
                else {
                    //函数里的const，已经由expression计算完成放到栈顶
                    currentFuncConstantsIndex.put(idenName,currentFuncStackIndex-1);
                }
            }
            else {//已经赋值的，不是constant的
                if(isStart){
                    //int a = 3+3;
                    globalVarIndex.put(idenName,stackIndex-1);
                }
                else {
                    currentFuncVarIndexs.put(idenName,currentFuncStackIndex-1);
                }
            }
        }
        /*Token currentToken=reader.readToken();
        Token temp1 = currentToken;
        if(currentToken.getType()!=IDENTIFIER){
            reader.unreadToken(currentToken);
            throw new NotFitException("<init_declarator>中识别不到合法的<identifier>");
        }
        currentToken=reader.readToken();
        if(currentToken.getType()!=ASSIGNMENT_OPERATOR){
            reader.unreadToken(currentToken);
        }
        else {
            Token temp = currentToken;
            try{
                expression();
            }
            catch (NotFitException e){
                reader.unreadToken(temp);
                reader.unreadToken(temp1);
                throw new NotFitException("<initializer>中识别不到合法的<expression>");
            }
        }*/
    }
//    <function-definition> ::= <type-specifier><identifier><parameter-clause><compound-statement>
//    int func(int a){}
//0 0（函数名的位置） 4（参数个数） 1（作用域，基础只为0或者1）
//    functions
    public void function_definition() throws NotFitException,NullPointerException{
        currentFuncVarIndexs = new HashMap<>();
        currentOperations = new ArrayList<>();
        currentParamNum = 0;
        currentFuncStackIndex = 0;
        currentConstFlag = false;
        currentFuncConstantsIndex = new HashMap<>();
        isStart = false;
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=SIMPLE_TYPE_SPECIFIER) {
            reader.unreadToken(currentToken);
            throw new NotFitException("functionDefinition中没有type-specifier"+"  "+currentToken.toString());
        }
        Token temp = currentToken;//void int
        currentToken=reader.readToken();
        if(currentToken.getType()!=IDENTIFIER) {
            reader.unreadToken(currentToken);
            reader.unreadToken(temp);
            throw new NotFitException("functionDefinition中没有identifier");
        }
        String funcName = new String(currentToken.getValue().toString());//防止浅拷贝
        //把函数名存到constants里
        int nameIndex = constants.size();
        constants.add(new Constant(nameIndex,'S',"\""+funcName+"\""));
        if(globalConstantsIndex.containsKey("\""+funcName+"\"")){
            throw new RuntimeException("函数重定义！");
        }
        globalConstantsIndex.put("\""+funcName+"\"",nameIndex);
        currentParamNum = 0;
        try{
            parameter_clause();
            compound_statement();
        }
        catch (NotFitException e){
            reader.pushBackTokens();//恢复现场，谨慎考虑其与其他函数关系，因为pushback数组是全局的
            e.printStackTrace();
            throw new NotFitException("functionDefinition构建失败");
        }
        //funcName currentParamNum已知 作用域都为1；接下来考虑operation
        int fIndex = functions.size();
        functionIndex.put(funcName,fIndex);
        Function currentFunction = new Function(fIndex,nameIndex,currentParamNum,1);
        currentFunction.operations=currentOperations;//每次重新调用函数定义时候都会new一个，因此这里是硬拷贝
        currentFunction.type = temp.getValue().toString();
        functions.add(currentFunction);
    }
//<parameter-clause> ::=
//    '(' [<parameter-declaration-list>] ')'
    public void parameter_clause() throws NotFitException{
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_PARENTHESES){
            reader.unreadToken(currentToken);
            throw new NotFitException("<parameter-clause>没有左括号");
        }
        try{
            parameter_declaration_list();
        }
        catch (NotFitException e){
            //
        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES){
            reader.pushBackTokens();//只能这样，因为不确定parameter_declaration_list()读到了什么
            throw new NotFitException("<parameter-clause>没有右括号");
        }
    }

//    <parameter-declaration-list> ::=
//    <parameter-declaration>{','<parameter-declaration>}

    public void parameter_declaration_list() throws NotFitException{
        try{
            parameter_declaration();//加载参数
            currentParamNum++;
        }
        catch (NotFitException e) {
            throw new NotFitException("parameter_declaration_list不合法");
        }
        //{','<parameter-declaration>}
        while (true){//注意程序走到这里了必然是合法的。不要抛异常。
            Token currentToken = reader.readToken();
            if(currentToken==null)
                break;
            if(currentToken.getType()!=COMMA){
                reader.unreadToken(currentToken);
                break;
            }
            try{
                parameter_declaration();
            }
            catch (NotFitException e){
                reader.unreadToken(currentToken);
                break;
            }
        }
    }
//<parameter-declaration> ::=
//    [<const-qualifier>]<type-specifier><identifier>
    public void parameter_declaration() throws NotFitException{ //这里需要把当前函数的参数入栈，loada 0,0(1,2,3) 然后iload
//        currentFuncIndex里存当前参数位置
        boolean constParam = false;
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=CONST_QUALIFIER){  //是普通的int a，存到currentVar
            if(!currentToken.getValue().equals("int")){
                reader.unreadToken(currentToken);
                throw new NotFitException("parameter-declaration不合法");
            }
            //<type-specifier><identifier>的情况

            Token temp = currentToken;
            currentToken = reader.readToken();
            if(currentToken.getType()!=IDENTIFIER){
                reader.unreadToken(currentToken);
                reader.unreadToken(temp);
                throw new NotFitException("parameter-declaration不合法");
            }
            currentFuncVarIndexs.put(currentToken.getValue().toString(),currentFuncStackIndex);
            currentFuncStackIndex++;
        }
        else {
            constParam = true;//需要把参数存到currentConstants里
            Token temp = currentToken;
            currentToken = reader.readToken();
            if(currentToken.getType()!=SIMPLE_TYPE_SPECIFIER){
                reader.unreadToken(currentToken);
                reader.unreadToken(temp);
                throw new NotFitException("parameter-declaration不合法");
            }
            Token temp1 = currentToken;
            currentToken = reader.readToken();
            if(currentToken.getType()!=IDENTIFIER){
                reader.unreadToken(currentToken);
                reader.unreadToken(temp1);
                reader.unreadToken(temp);
                throw new NotFitException("parameter-declaration不合法");
            }
            currentFuncConstantsIndex.put(currentToken.getValue().toString(),currentFuncStackIndex);
            currentFuncStackIndex++;
        }
    }
//<compound-statement> ::=
//    '{' {<variable-declaration>} <statement-seq> '}'      //pushBack的锅！！
    public void compound_statement() throws NotFitException{
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_BRACKET){
            reader.unreadToken(currentToken);
            throw new NotFitException("compound_statement没有{");
        }
//         {<variable-declaration>}
        while (true){
            try{
                variable_declaration();
            }
            catch (NotFitException e){
                break;
            }
        }
//       <statement-seq> '}'
        try{
            statement_seq();
        }
        catch (NotFitException e){
            throw new NotFitException("<compound-statement>中没有 <statement-seq>");
        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_BRACKET){
            reader.pushBackTokens();
            throw new NotFitException("<compound-statement>中没有} "+currentToken.toString());
        }
    }

//<statement-seq> ::= {<statement>}
    public void statement_seq() throws NotFitException{
        //不要用pushback
        while(true){
            try{
                statement();
            }
            catch (NotFitException e){
                break;
            }
        }
    }
//<statement> ::=
//     '{' <statement-seq> '}'
//    |<condition-statement>  if
//    |<loop-statement>  while
//    |<jump-statement>  return
//    |<print-statement>  print
//    |<scan-statement>  scan
//    |<assignment-expression>';'    <identifier><assignment-operator><expression>
//    |<function-call>';'             <identifier> '(' [<expression-list>] ')'
//    |';'
    public void statement() throws NotFitException{
        //pushback可能有坑
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        switch (currentToken.getType()){
            case LEFT_BRACKET:
                try {
                    statement_seq();
                }
                catch (NotFitException e){
                    reader.unreadToken(currentToken);
                    throw new NotFitException("非法statement");
                }
                currentToken = reader.readToken();
                if(currentToken.getType()!=RIGHT_BRACKET){
                    reader.pushBackTokens();
                    throw new NotFitException("statement右边没有}");
                }
                break;
            case RESERVEDWORD_IF:
                reader.unreadToken(currentToken);
                try{
                    condition_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的condition-statement解析错误");
                }
                break;
            case RESERVEDWORD_WHILE:
                reader.unreadToken(currentToken);
                try{
                    loop_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的loop-statement解析错误");
                }
                break;
            case RESERVEDWORD_RETURN:
                reader.unreadToken(currentToken);
                try{
                    jump_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的jump-statement解析错误");
                }
                break;
            case RESERVEDWORD_PRINT:
                reader.unreadToken(currentToken);
                try{
                    print_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的print-statement解析错误");
                }
                break;
            case RESERVEDWORD_SCAN:
                reader.unreadToken(currentToken);
                try{
                    scan_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的scan-statement解析错误");
                }
                break;
            case SEMICOLON:
                break;
//    |<assignment-expression>';'    <identifier><assignment-operator><expression>
//    |<function-call>';'             <identifier> '(' [<expression-list>] ')'
            case IDENTIFIER:
                Token temp = currentToken;
                currentToken=reader.readToken();
                reader.unreadToken(currentToken);
                if(currentToken.getType()==LEFT_PARENTHESES){
                    try{
                        function_call();
                    }
                    catch (NotFitException e){
                        throw new NotFitException("statement中的function-call解析错误");
                    }
                    currentToken = reader.readToken();
                    if(currentToken.getType()!=SEMICOLON){
                        reader.pushBackTokens();
                        throw new NotFitException("statement中的function-call后没有分号");
                    }
                }
                else {
                    try{
                        assignment_expression();
                    }
                    catch (NotFitException e){
                        throw new NotFitException("statement中的assignment_expression解析错误");
                    }
                    currentToken = reader.readToken();
                    if(currentToken.getType()!=SEMICOLON){
                        reader.pushBackTokens();
                        throw new NotFitException("statement中的assignment_expression后没有分号");
                    }
                }
                break;
            default:
                reader.unreadToken(currentToken);
                throw new NotFitException("没有有效的statement头符号");
        }
    }
//    <condition> ::= <expression>[<relational-operator><expression>]
    //expression把结果放在栈顶
    public String condition() throws NotFitException{
        try{
            expression();
        }
        catch (NotFitException e){
            throw new NotFitException("condition前面没有expression");
        }
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RELATIONAL_OPERATOR){
            reader.unreadToken(currentToken);
            return null;
            //只有一个表达式
            //将表达式结果取出来判断。
        }
        //两个表达式，icmp
//      <relational-operator>     ::= '<' | '<=' | '>' | '>=' | '!=' | '=='
        else {
            try{
                expression();
            }
            catch (NotFitException e){
                throw new NotFitException("condition的relational-operator后面没有expression");
            }
            int index = currentOperations.size();
            currentOperations.add(new Operation(index,"icmp",null,null));
            return currentToken.getValue().toString();
        }
    }

//    <condition-statement> ::=
//     'if' '(' <condition> ')' <statement> ['else' <statement>]
    public void condition_statement() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RESERVEDWORD_IF){
            reader.unreadToken(currentToken);
            throw new NotFitException("condition-statement前没有if");
        }
        Token ifToken =currentToken;
        currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_PARENTHESES){
            reader.unreadToken(currentToken);
            reader.unreadToken(ifToken);
            throw new NotFitException("condition-statement中没有(");
        }
        //      <relational-operator>     ::= '<' | '<=' | '>' | '>=' | '!=' | '=='

        String relational_operator = condition();//有异常就让它自动往上层抛就行
        //后面出错了就该停止运行，不会再考虑回退
        currentToken=reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES)
            throw new NotFitException("condition-statement中没有)");
        //预留判断语句的位置
        int index = currentOperations.size();
        Operation judgeCondition = new Operation(index,null,null,null);
        currentOperations.add(judgeCondition);//利用浅拷贝的特性之后将其更新
        //执行if(){}大括号里的内容
        statement();
        //只有一个表达式的情况
        if(relational_operator == null){
            //je,if(0)就跳到if语句段后面去
            int jmpIndex = currentOperations.size()+1;
            judgeCondition.setOperation(index,"je",jmpIndex,null);
        }
        else {
            int jmpIndex = currentOperations.size()+1;
            switch (relational_operator){
                case "<":
                    //左边大于等于的情况下跳，即result=1或0,jge
                    judgeCondition.setOperation(index,"jge",jmpIndex,null);
                    break;
                case "<=":
                    //result 0或1，不是负数,jge
                    judgeCondition.setOperation(index,"jg",jmpIndex,null);
                    break;
                case ">":
                    judgeCondition.setOperation(index,"jle",jmpIndex,null);
                    break;
                case ">=":
                    judgeCondition.setOperation(index,"jl",jmpIndex,null);
                    break;
                case "!=":
                    //result 0,je
                    judgeCondition.setOperation(index,"je",jmpIndex,null);
                    break;
                case "==":
                    judgeCondition.setOperation(index,"jne",jmpIndex,null);
                    break;
                default:
                    throw new RuntimeException();
            }
        }
        currentToken = reader.readToken();
        if(currentToken!=null){
            if(currentToken.getType()!=RESERVEDWORD_ELSE){
                reader.unreadToken(currentToken);
            }
            else {
                statement();
            }
        }
    }
//<loop-statement> ::=
//    'while' '(' <condition> ')' <statement>
//如果条件不满足，就jump到后面去
    public void loop_statement() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RESERVEDWORD_WHILE){
            reader.unreadToken(currentToken);
            throw new NotFitException("while");
        }
        Token temp =currentToken;
        currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_PARENTHESES){
            reader.unreadToken(currentToken);
            reader.unreadToken(temp);
            throw new NotFitException("(");
        }
        String relational_operator = condition();
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES){
            throw new NotFitException(")");
        }
        //先插入空指令占位置
        //预留判断语句的位置
        int index = currentOperations.size();
        Operation judgeCondition = new Operation(index,null,null,null);
        currentOperations.add(judgeCondition);//利用浅拷贝的特性之后将其更新
        statement();
        //在最后加一条跳回jump语句的语句
        int jumpBackIndex = currentOperations.size();
        currentOperations.add(new Operation(jumpBackIndex,"jmp",index,null));
        if(relational_operator == null){
            //je,if(0)就跳到if语句段后面去
            int jmpIndex = currentOperations.size()+1;
            judgeCondition.setOperation(index,"je",jmpIndex,null);
        }
        else {
            int jmpIndex = currentOperations.size()+1;
            switch (relational_operator){
                case "<":
                    //左边大于等于的情况下跳，即result=1或0,jge
                    judgeCondition.setOperation(index,"jge",jmpIndex,null);
                    break;
                case "<=":
                    //result 0或1，不是负数,jge
                    judgeCondition.setOperation(index,"jg",jmpIndex,null);
                    break;
                case ">":
                    judgeCondition.setOperation(index,"jle",jmpIndex,null);
                    break;
                case ">=":
                    judgeCondition.setOperation(index,"jl",jmpIndex,null);
                    break;
                case "!=":
                    //result 0,je
                    judgeCondition.setOperation(index,"je",jmpIndex,null);
                    break;
                case "==":
                    judgeCondition.setOperation(index,"jne",jmpIndex,null);
                    break;
                default:
                    throw new RuntimeException();
            }
        }
    }

//    <jump-statement> ::= <return-statement>
    public void jump_statement() throws NotFitException{
        return_statement();
    }

//    <return-statement> ::= 'return' [<expression>] ';'
    public void return_statement() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RESERVEDWORD_RETURN){
            reader.unreadToken(currentToken);
            throw new NotFitException("return");
        }
        try{
            expression();
        }
        catch (NotFitException ignored){

        }
        currentToken = reader.readToken();
        if (currentToken.getType()!=SEMICOLON){
            throw new NotFitException(";");
        }
        int index = currentOperations.size();
        currentOperations.add(new Operation(index,"iret",null,null));
    }
//<scan-statement> ::= 'scan' '(' <identifier> ')' ';'
    public void scan_statement() throws NotFitException{   //iscan
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RESERVEDWORD_SCAN){
            reader.unreadToken(currentToken);
            throw new NotFitException("no scan token");
        }
        Token scan = currentToken;
        currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_PARENTHESES){
            throw new RuntimeException();
        }
        currentToken = reader.readToken();
        Token iden = currentToken;
        if(currentToken.getType()!=IDENTIFIER){
            throw new RuntimeException();
        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES){
            throw new RuntimeException();
        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=SEMICOLON){
            throw new RuntimeException();
        }
        //首先loada获得idend的地址
        //把iden的值取出来，（constant和global）先找全局变量，再找局部变量
        String idenName = iden.getValue().toString();
        //顺序：先找局部
        int idenIndex;
        if(currentFuncConstantsIndex.containsKey(idenName)){
            throw new RuntimeException("constants不可变");
        }
        else if(currentFuncVarIndexs.containsKey(idenName)) {
            idenIndex = (int) currentFuncVarIndexs.get(idenName);
            int index = currentOperations.size();
            currentOperations.add(new Operation(index,"loada",0,idenIndex));
            currentOperations.add(new Operation(index+1,"iscan",null,null));
            currentOperations.add(new Operation(index+2,"istore",null,null));
        }
        else if(globalConstantsIndex.containsKey(idenName)){ //考虑isstart
            throw new RuntimeException("constants不可变");
        }
        else if(globalVarIndex.containsKey(idenName)){//注意这个要loada 1,index 考虑isstart
            idenIndex = (int)globalVarIndex.get(idenName);
            int index = currentOperations.size();
            currentOperations.add(new Operation(index,"loada",1,idenIndex));
            currentOperations.add(new Operation(index+1,"iscan",null,null));
            currentOperations.add(new Operation(index+2,"istore",null,null));
        }
        else {
            throw new RuntimeException();
        }
    }
//<print-statement> ::= 'print' '(' [<printable-list>] ')' ';'
    public void print_statement() throws NotFitException{ //iprint
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RESERVEDWORD_PRINT){
            reader.unreadToken(currentToken);
            throw new NotFitException("no print token");
        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_PARENTHESES){
            throw new RuntimeException();
        }
        try{
            printable_list();
            int index = currentOperations.size();
            currentOperations.add(new Operation(index,"printl",null,null));
        }
        catch (NotFitException ignored){}
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES)
            throw new RuntimeException();
        currentToken = reader.readToken();
        if(currentToken.getType()!=SEMICOLON)
            throw new RuntimeException();
    }
//<printable-list>  ::=  <expression> {','  <expression>}
    public void printable_list() throws NotFitException{
        try{

            expression();
        }
        catch (NotFitException e){
            throw new NotFitException("printable-list without expression");
        }
        Token currentToken=reader.readToken();
        if(currentToken.getType()!=COMMA){
            reader.unreadToken(currentToken);
            //打印单条，带空格
            int index = currentOperations.size();
            currentOperations.add(new Operation(index,"iprint",null,null));
            currentFuncStackIndex--;
            currentOperations.add(new Operation(index+1,"ipush",32,null));//压入空格ascil码
            currentOperations.add(new Operation(index+2,"cprint",null,null));
            return;
        }
        while (currentToken.getType()==COMMA){
            try {
                expression();
                int index = currentOperations.size();
                currentOperations.add(new Operation(index,"iprint",null,null));
                currentFuncStackIndex--;
                currentOperations.add(new Operation(index+1,"ipush",32,null));//压入空格ascil码
                currentOperations.add(new Operation(index+2,"cprint",null,null));
            }
            catch (NotFitException e){
                throw  new RuntimeException();
            }
            currentToken = reader.readToken();
        }
        reader.unreadToken(currentToken);
    }


    public void assignment_expression() throws NotFitException{

    }

//<expression> ::= <additive-expression>
    public void expression() throws NotFitException{  //需要复原。需要把结果放到栈顶
        additive_expression();
    }
//<additive-expression> ::=   <multiplicative-expression>{<additive-operator><multiplicative-expression>}
    public void additive_expression() throws NotFitException{
        multiplicative_expression();
        Token currentToken = reader.readToken();
        Token op = currentToken;
        while (currentToken.getType()==ADDITIVE_OPERATOR){
            try{
                multiplicative_expression();
            }
            catch (NotFitException e){
                reader.unreadToken(currentToken);
                return;
            }
            int index = currentOperations.size();
            if(op.getValue().toString().equals("+")){
                currentOperations.add(new Operation(index,"iadd",null,null));
            }
            else {
                currentOperations.add(new Operation(index,"isub",null,null));
            }
            currentFuncStackIndex--;
            currentToken = reader.readToken();
        }
        reader.unreadToken(currentToken);
    }
//<multiplicative-expression> ::=  <unary-expression>{<multiplicative-operator><unary-expression>}
    public void multiplicative_expression() throws NotFitException{
        unary_expression();
        Token currentToken = reader.readToken();
        Token op = currentToken;
        while (currentToken.getType()==MULTIPLICATIVE_OPERATOR){
            try{
                unary_expression();
            }
            catch (NotFitException e){
                reader.unreadToken(currentToken);
                return;
            }
            int index = currentOperations.size();
            if(op.getValue().toString().equals("*")){
                currentOperations.add(new Operation(index,"imul",null,null));
            }
            else {
                currentOperations.add(new Operation(index,"idiv",null,null));
            }
            currentFuncStackIndex--;
            currentToken = reader.readToken();
        }
        reader.unreadToken(currentToken);
    }

//    <unary-expression> ::= [<additive-operator>]<primary-expression>
    public void unary_expression() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=ADDITIVE_OPERATOR){
            reader.unreadToken(currentToken);
            primary_expression();
        }
        else {
            primary_expression();
            int index = currentOperations.size();
            if(currentToken.getValue().toString().equals("-")){
                currentOperations.add(new Operation(index,"ineg",null,null));//栈无变化
            }
        }
    }
//<primary-expression> ::=
//     '('<expression>')'
//    |<identifier>
//    |<integer-literal>
//    |<function-call>                               <identifier> '(' [<expression-list>] ')'

//    <integer-literal>                              ::= <decimal-literal>|<hexadecimal-literal>

    //需要把对应的结果取出来放到栈顶
    public void primary_expression() throws NotFitException{
        //这里必须考虑isstart
        Token currentToken =reader.readToken();
        reader.unreadToken(currentToken);
        switch (currentToken.getType()){
            case DIGIT:
            case DECIMAL_LITERAL:
            case HEXADECIMAL_LITERAL:
                integer_literal();
                break;
            case IDENTIFIER:
                Token iden = reader.readToken();
                currentToken = reader.readToken();
                if(currentToken.getType()!=LEFT_PARENTHESES){
                    reader.unreadToken(currentToken);
                    //把iden的值取出来，（constant和global）先找全局变量，再找局部变量
                    String idenName = iden.getValue().toString();
                    //顺序：先找局部
                    int idenIndex;
                    if(currentFuncConstantsIndex.containsKey(idenName)){
                        idenIndex = (int)currentFuncConstantsIndex.get(idenName);
                        int index = currentOperations.size();
                        currentOperations.add(new Operation(index,"loada",0,idenIndex));
                        currentFuncStackIndex++;
                        currentOperations.add(new Operation(index+1,"iload",null,null));
                    }
                    else if(currentFuncVarIndexs.containsKey(idenName)) {
                        idenIndex = (int) currentFuncVarIndexs.get(idenName);
                        int index = currentOperations.size();
                        currentOperations.add(new Operation(index,"loada",0,idenIndex));
                        currentFuncStackIndex++;
                        currentOperations.add(new Operation(index+1,"iload",null,null));
                    }
                    else if(globalConstantsIndex.containsKey(idenName)){ //考虑isstart
                        idenIndex = (int) globalConstantsIndex.get(idenName);//注意这个在.constants里
                        if(isStart){
                            int startIndex = starts.size();
                            starts.add(new Operation(startIndex,"loadc",idenIndex,null));
                            stackIndex++;
                        }
                        else {
                            int index = currentOperations.size();
                            currentOperations.add(new Operation(index,"loadc",idenIndex,null));
                            currentFuncStackIndex++;
                        }
                    }
                    else if(globalVarIndex.containsKey(idenName)){//注意这个要loada 1,index 考虑isstart
                        idenIndex = (int)globalVarIndex.get(idenName);
                        if(isStart){
                            int startIndex = starts.size();
                            starts.add(new Operation(startIndex,"loada",0,idenIndex));
                            stackIndex++;
                            starts.add(new Operation(startIndex+1,"iload",null,null));
                        }
                        else {
                            int index = currentOperations.size();
                            currentOperations.add(new Operation(index,"loada",1,idenIndex));
                            currentFuncStackIndex++;
                            currentOperations.add(new Operation(index+1,"iload",null,null));
                        }
                    }
                    else {
                        throw new RuntimeException();
                    }
                    return;
                }

                reader.unreadToken(currentToken);
                reader.unreadToken(iden);
                int fIndex =(int)functionIndex.get(iden.getValue().toString());
                Function f = functions.get(fIndex);
                if(f.type.equals("void")){
                    throw new RuntimeException("表达式中的函数不能为void");
                }
                function_call();
                break;
            case LEFT_PARENTHESES:
                currentToken = reader.readToken();
                try{
                    expression();
                }
                catch (NotFitException e){
                    reader.unreadToken(currentToken);
                    throw new NotFitException("expression fail");
                }
                Token left = currentToken;
                currentToken = reader.readToken();
                if(currentToken.getType()!=RIGHT_PARENTHESES){
                    throw new RuntimeException();
                }
                break;
        }
    }
//<function-call> ::= <identifier> '(' [<expression-list>] ')'
    public void function_call() throws NotFitException{
        //主要工作：压入参数，然后直接call 就行
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=IDENTIFIER){
            reader.unreadToken(currentToken);
            throw new NotFitException("function_call not fit");
        }
        Token iden = currentToken;
        currentToken = reader.readToken();
        if(currentToken.getType()!=LEFT_PARENTHESES){
            reader.unreadToken(currentToken);
            reader.unreadToken(iden);
            throw new NotFitException("function_call not fit");
        }
        try{
            expression_list();//这里要压入参数
        }
        catch (NotFitException e){

        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES)
            throw new RuntimeException();
        //call就行
        int index = currentOperations.size();
        int funcIndex = (int)globalConstantsIndex.get("\""+iden.getValue().toString()+"\"");
        currentOperations.add(new Operation(index,"call",funcIndex,null));
    }
//<expression-list> ::= <expression>{','<expression>}
    public void expression_list() throws NotFitException{
        expression();
        Token currentToken =reader.readToken();
        while(currentToken.getType()==COMMA){
            try{
                expression();
            }
            catch (NotFitException e){
                reader.unreadToken(currentToken);
                return;
            }
            currentToken = reader.readToken();
        }
        reader.unreadToken(currentToken);
    }
//<integer-literal> ::=  <decimal-literal>|<hexadecimal-literal>
    public void integer_literal() throws NotFitException{ //得判断istart
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=DECIMAL_LITERAL&&currentToken.getType()!=HEXADECIMAL_LITERAL){
            reader.unreadToken(currentToken);
        }
        String num = currentToken.getValue().toString();
        if(currentToken.getType()==HEXADECIMAL_LITERAL){
            Integer decimal = Integer.valueOf(num,16);//十六进制转十进制
            num = decimal.toString();
        }
        if(isStart){
            int startIndex = starts.size();
            starts.add(new Operation(startIndex,"ipush",num,null));
            startIndex++;
        }
        else {
            int index = currentOperations.size();
            currentOperations.add(new Operation(index,"ipush",num,null));
            currentFuncStackIndex++;
        }
    }
}
