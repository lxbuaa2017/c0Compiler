package com.lx.analyser;

import com.lx.error.AnalyserException;
import com.lx.error.NotFitException;
import com.lx.tokenizer.Token;
import com.lx.tokenizer.Type;
import com.lx.util.TokenReader;

import java.util.ArrayList;

import static com.lx.tokenizer.Type.*;

public class Analyser {
    private TokenReader reader;
    private Token currentToken;
    public Analyser(TokenReader reader) {
        this.reader = reader;
    }

//<C0-program> ::= {<variable-declaration>}{<function-definition>}
//    <variable-declaration> ::=  [<const-qualifier>]<type-specifier><init-declarator-list>';'
//<simple-type-specifier>  ::= 'void'|'int'
//    <function-definition> ::= <type-specifier><identifier><parameter-clause><compound-statement>
    //    有或的情况，判断first集：const void int
    public void c0Program() throws AnalyserException {
        boolean varFinish = false;
        currentToken=reader.readToken();
        if(currentToken==null)
            throw new AnalyserException("没有可以识别的Token");
        if(!(currentToken.getType()==CONST_QUALIFIER||currentToken.getType()==SIMPLE_TYPE_SPECIFIER)){
            throw new AnalyserException("起始位置非法");
        }
        reader.unreadToken(currentToken);
//
        if(currentToken.getType()==CONST_QUALIFIER){
            try{
                variableDeclaration();
            }
            catch (NotFitException e){
                throw  new AnalyserException("const定义错误");
            }
        }
        else {
            try{
                variableDeclaration();
            }
            catch (NotFitException e){
                //说明只能是function
                try{
                    functionDefinition();
                    varFinish = true;
                }
                catch (NotFitException e1){
                    throw  new AnalyserException("开头定义错误");
                }
            }
        }
        while (!varFinish){
            try{
                variableDeclaration();
            }
            catch (NotFitException e){
                varFinish=true;
                break;
            }
        }
        while (true){
            try{
                functionDefinition();
            }
            catch (NotFitException e){
                break;
            }
        }
    }
//<variable-declaration> ::=
//    [<const-qualifier>]<type-specifier><init-declarator-list>';'
    public void variableDeclaration() throws NotFitException{
        reader.clearPushBack();
        currentToken = reader.readToken();
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
        currentToken=reader.readToken();
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

    public void functionDefinition() throws NotFitException{

    }

    public void constant_declaration(){

    }

    public void constantDeclaration(){

    }

    public void expression() throws NotFitException{

    }
}
