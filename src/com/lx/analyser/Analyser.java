package com.lx.analyser;

import com.lx.error.AnalyserException;
import com.lx.error.NotFitException;
import com.lx.tokenizer.Token;
import com.lx.util.TokenReader;

import static com.lx.tokenizer.Type.*;

public class Analyser {
    private TokenReader reader;
//    private Token currentToken;
//    currentToken不要用全局变量
    public Analyser(TokenReader reader) {
        this.reader = reader;
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
                    throw  new AnalyserException("开头定义错误");
                }
            }
        }
        while (!varFinish){
            try{
                variable_declaration();
            }
            catch (NotFitException e){
                varFinish=true;
                break;
            }
        }
        while (true){
            try{
                function_definition();
            }
            catch (NotFitException e){
                break;
            }
        }
    }
//<variable-declaration> ::=
//    [<const-qualifier>]<type-specifier><init-declarator-list>';'
    public void variable_declaration() throws NotFitException{
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        if(currentToken.getType()==CONST_QUALIFIER) {
            currentToken = reader.readToken();
        }
        if(currentToken.getType()!=SIMPLE_TYPE_SPECIFIER){
            reader.pushBackTokens();
            throw new NotFitException("<variable-declaration>中识别不到合法的<type-specifier>");
        }
        currentToken=reader.readToken();
        if(currentToken.getType()!=IDENTIFIER){
            reader.pushBackTokens();
            throw new NotFitException("<variable-declaration>中识别不到合法的<init-declarator>");
        }
        currentToken=reader.readToken();
        if(currentToken.getType()!=ASSIGNMENT_OPERATOR){
            reader.unreadToken(currentToken);
        }
        else {
            try{
                expression();
            }
            catch (NotFitException e){
                throw new NotFitException("<variable-declaration>中识别不到合法的<expression>");
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
    }
    public void init_declarator() throws NotFitException{
        Token currentToken=reader.readToken();
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
        }
    }
//    <function-definition> ::= <type-specifier><identifier><parameter-clause><compound-statement>
//    int func(){}
    public void function_definition() throws NotFitException{
        reader.clearPushBack();
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=SIMPLE_TYPE_SPECIFIER) {
            reader.unreadToken(currentToken);
            throw new NotFitException("functionDefinition中没有type-specifier");
        }
        Token temp = currentToken;
        currentToken=reader.readToken();
        if(currentToken.getType()!=IDENTIFIER) {
            reader.unreadToken(currentToken);
            reader.unreadToken(temp);
            throw new NotFitException("functionDefinition中没有type-specifier");
        }
        try{
            parameter_clause();
            compound_statement();
        }
        catch (NotFitException e){
            reader.pushBackTokens();//恢复现场，谨慎考虑其与其他函数关系，因为pushback数组是全局的
            throw new NotFitException("functionDefinition构建失败");
        }
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
            parameter_declaration();
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
    public void parameter_declaration() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=CONST_QUALIFIER){
            if(currentToken.getType()!=SIMPLE_TYPE_SPECIFIER){
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
        }
        else {
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
        }
    }
//<compound-statement> ::=
//    '{' {<variable-declaration>} <statement-seq> '}'
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
            throw new NotFitException("<compound-statement>中没有}");
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
                try{
                    condition_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的condition-statement解析错误");
                }
                break;
            case RESERVEDWORD_WHILE:
                try{
                    loop_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的loop-statement解析错误");
                }
                break;
            case RESERVEDWORD_RETURN:
                try{
                    jump_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的jump-statement解析错误");
                }
                break;
            case RESERVEDWORD_PRINT:
                try{
                    print_statement();
                }
                catch (NotFitException e){
                    throw new NotFitException("statement中的print-statement解析错误");
                }
                break;
            case RESERVEDWORD_SCAN:
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
    public void condition() throws NotFitException{
        try{
            expression();
        }
        catch (NotFitException e){
            throw new NotFitException("condition前面没有expression");
        }
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=RELATIONAL_OPERATOR){
            reader.unreadToken(currentToken);
        }
        else {
            try{
                expression();
            }
            catch (NotFitException e){
                throw new NotFitException("condition的relational-operator后面没有expression");
            }
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
        condition();//有异常就让它自动往上层抛就行
        //后面出错了就该停止运行，不会再考虑回退
        currentToken=reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES)
            throw new NotFitException("condition-statement中没有)");
        statement();
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
        condition();
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES){
            throw new NotFitException(")");
        }
        statement();
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
    }
//<scan-statement> ::= 'scan' '(' <identifier> ')' ';'
    public void scan_statement() throws NotFitException{
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
    }
//<print-statement> ::= 'print' '(' [<printable-list>] ')' ';'
    public void print_statement() throws NotFitException{
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
            return;
        }
        while (currentToken.getType()==COMMA){
            try {
                expression();
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

    public void constant_declaration(){

    }
//<expression> ::= <additive-expression>
    public void expression() throws NotFitException{  //需要复原
        additive_expression();
    }
//<additive-expression> ::=   <multiplicative-expression>{<additive-operator><multiplicative-expression>}
    public void additive_expression() throws NotFitException{
        multiplicative_expression();
        Token currentToken = reader.readToken();
        while (currentToken.getType()==ADDITIVE_OPERATOR){
            try{
                multiplicative_expression();
            }
            catch (NotFitException e){
                reader.unreadToken(currentToken);
                return;
            }
            currentToken = reader.readToken();
        }
        reader.unreadToken(currentToken);
    }
//<multiplicative-expression> ::=  <unary-expression>{<multiplicative-operator><unary-expression>}
    public void multiplicative_expression() throws NotFitException{
        unary_expression();
        Token currentToken = reader.readToken();
        while (currentToken.getType()==MULTIPLICATIVE_OPERATOR){
            try{
                unary_expression();
            }
            catch (NotFitException e){
                reader.unreadToken(currentToken);
                return;
            }
            currentToken = reader.readToken();
        }
        reader.unreadToken(currentToken);
    }

//    <unary-expression> ::= [<additive-operator>]<primary-expression>
    public void unary_expression() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=ADDITIVE_OPERATOR){
            reader.unreadToken(currentToken);
        }
        primary_expression();
    }
//<primary-expression> ::=
//     '('<expression>')'
//    |<identifier>
//    |<integer-literal>
//    |<function-call>  <identifier> '(' [<expression-list>] ')'

//    <integer-literal> ::= <decimal-literal>|<hexadecimal-literal>
    public void primary_expression() throws NotFitException{
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
                    return;
                }
                reader.unreadToken(currentToken);
                reader.unreadToken(iden);
                function_call();
                break;
            case LEFT_PARENTHESES:
                currentToken = reader.readToken();
                try{
                    expression();
                }
                catch (NotFitException e){
                    reader.unreadToken(currentToken);
                    throw new NotFitException("primary_expression fail");
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
            expression_list();
        }
        catch (NotFitException e){

        }
        currentToken = reader.readToken();
        if(currentToken.getType()!=RIGHT_PARENTHESES)
            throw new RuntimeException();
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
    public void integer_literal() throws NotFitException{
        Token currentToken = reader.readToken();
        if(currentToken.getType()!=DECIMAL_LITERAL&&currentToken.getType()!=HEXADECIMAL_LITERAL){
            reader.unreadToken(currentToken);
        }
    }
}
