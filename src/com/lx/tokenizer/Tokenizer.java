package com.lx.tokenizer;

import com.lx.error.TokenizerException;
import com.lx.util.SourceFileReader;

import java.io.IOException;
import java.util.ArrayList;

public class Tokenizer {
//    public static int line = 0;
//    public static int column = 0;//预留给错误处理，记录当前行列
    public static ArrayList<Token> tokenizer(SourceFileReader reader) throws IOException, TokenizerException {
        ArrayList<Token> tokens = new ArrayList<>();
        String currentToken="";
        char currentChar;//记录一下当前分析的东西
        boolean currentTokenFinish = true;//用于判断当前token是否分析完毕

        //开始进行分析
        //First集：字母，数字，符号
        while(true){
            if(!reader.hasNext()){
                break;
            }
            else {
                currentToken="";
                currentChar = reader.readChar();
            }

            //首先处理空格
            while(currentChar==' '||currentChar=='\n'||currentChar=='\t'||currentChar=='\r'){
                currentChar = reader.readChar();
            }
            //处理单行注释
            if(currentChar=='/'){
                char temp = reader.readChar();
                if(temp!='/'&&temp!='*'){
                    reader.unReadChar(temp);
                }
                else if(temp=='/'){
                    while (temp!='\n'&&temp!='\r'){
                        temp=reader.readChar();
                    }
                    currentChar=reader.readChar();
                }
                else if (temp=='*'){
                    currentChar='0';
                    while (true){
                        currentChar=reader.readChar();
                        if(currentChar=='*'){
                            char next = reader.readChar();
                            if(next=='/'){
                                currentChar=reader.readChar();
                                break;
                            }
                            else {
                                reader.unReadChar(next);
                            }
                        }
                    }
                }
            }
            while(currentChar==' '||currentChar=='\n'||currentChar=='\t'||currentChar=='\r'){
                currentChar = reader.readChar();
            }

            //开始进行分析，一次循环输出一个token
            //字母开头
            //<identifier> ::=<nondigit>{<nondigit>|<digit>}  nondigit是英文字母
            //<reserved-word>
            if(Character.isLetter(currentChar)){
                //先读完
                while(Character.isLetter(currentChar)||Character.isDigit(currentChar)){//最大吞噬
                    currentToken+=currentChar;//性能问题，有时间再换成StringBuffer
                    if(reader.hasNext())
                        currentChar=reader.readChar();
                    else
                        throw new TokenizerException("非法结尾！");
                }
                reader.unReadChar(currentChar);
                //以字母开头有两种情况：保留字，identifer（变量或函数名）
                //<simple-type-specifier>  ::= 'void'|'int'
                //<const-qualifier>        ::= 'const'
                switch (currentToken){
                    case "const":
                        tokens.add(new Token(Type.CONST_QUALIFIER,currentToken));break;
                    case "void":
                        tokens.add(new Token(Type.SIMPLE_TYPE_SPECIFIER,currentToken));break;
                    case "int":
                        tokens.add(new Token(Type.SIMPLE_TYPE_SPECIFIER,currentToken));break;
                    case "char":
                        tokens.add(new Token(Type.RESERVEDWORD_CHAR,currentToken));break;
                    case "double":
                        tokens.add(new Token(Type.RESERVEDWORD_DOUBLE,currentToken));break;
                    case "if":
                        tokens.add(new Token(Type.RESERVEDWORD_IF,currentToken));break;
                    case "else":
                        tokens.add(new Token(Type.RESERVEDWORD_ELSE,currentToken));break;
                    case "switch":
                        tokens.add(new Token(Type.RESERVEDWORD_SWITCH,currentToken));break;
                    case "struct":
                        tokens.add(new Token(Type.RESERVEDWORD_STRUCT,currentToken));break;
                    case "case":
                        tokens.add(new Token(Type.RESERVEDWORD_CASE,currentToken));break;
                    case "default":
                        tokens.add(new Token(Type.RESERVEDWORD_DEFAULT,currentToken));break;
                    case "while":
                        tokens.add(new Token(Type.RESERVEDWORD_WHILE,currentToken));break;
                    case "for":
                        tokens.add(new Token(Type.RESERVEDWORD_FOR,currentToken));break;
                    case "do":
                        tokens.add(new Token(Type.RESERVEDWORD_DO,currentToken));break;
                    case "return":
                        tokens.add(new Token(Type.RESERVEDWORD_RETURN,currentToken));break;
                    case "break":
                        tokens.add(new Token(Type.RESERVEDWORD_BREAK,currentToken));break;
                    case "continue":
                        tokens.add(new Token(Type.RESERVEDWORD_CONTINUE,currentToken));break;
                    case "print":
                        tokens.add(new Token(Type.RESERVEDWORD_PRINT,currentToken));break;
                    case "scan":
                        tokens.add(new Token(Type.RESERVEDWORD_SCAN,currentToken));break;
                    default:
                        tokens.add(new Token(Type.IDENTIFIER,currentToken));break;
                }
                continue;
            }

            //数字开头，考虑0x、0X和普通数字（不以0开头）和0
//            <digit> ::=
//    '0'|<nonzero-digit>
//<nonzero-digit> ::=
//    '1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'
//<hexadecimal-digit> ::=
//    <digit>|'a'|'b'|'c'|'d'|'e'|'f'|'A'|'B'|'C'|'D'|'E'|'F'
//
//<integer-literal> ::=
//    <decimal-literal>|<hexadecimal-literal>
//<decimal-literal> ::=
//    '0'|<nonzero-digit>{<digit>}
//<hexadecimal-literal> ::=
//    ('0x'|'0X')<hexadecimal-digit>{<hexadecimal-digit>}
//            <hexadecimal-digit> ::=
//    <digit>|'a'|'b'|'c'|'d'|'e'|'f'|'A'|'B'|'C'|'D'|'E'|'F'
            if(Character.isDigit(currentChar)){
                //先处理开头0的情况，考虑x回退
                if(currentChar=='0'){
                    currentChar = reader.readChar();
                    if(currentChar=='x'||currentChar=='X'){
                        char x = currentChar;
                        if(reader.hasNext())
                            currentChar=reader.readChar();
                        else
                            throw new TokenizerException("不是完整有效的十六进制数字");
                        //    <digit>|'a'|'b'|'c'|'d'|'e'|'f'|'A'|'B'|'C'|'D'|'E'|'F'
                        if(!(Character.isDigit(currentChar)||(currentChar>='a'&&currentChar<='f')||(currentChar>='A'&&currentChar<='F'))){//如0x+，则把x+塞回去返回0
                            reader.unReadChar(currentChar);
                            reader.unReadChar(x);
                            tokens.add(new Token(Type.DECIMAL_LITERAL,0));
                            continue;
                        }
                        while (Character.isDigit(currentChar)||(currentChar>='a'&&currentChar<='f')||(currentChar>='A'&&currentChar<='F')){
                            currentToken+=currentChar;
                            if(reader.hasNext())
                                currentChar=reader.readChar();
                            else
                                throw new TokenizerException("非法结尾！");
                        }
                        reader.unReadChar(currentChar);
                        tokens.add(new Token(Type.HEXADECIMAL_LITERAL,"0x"+currentToken));
                        continue;
                    }
                    else {
                        reader.unReadChar(currentChar);
                        tokens.add(new Token(Type.DECIMAL_LITERAL,0));
                        continue;
                    }
                }
                else {
                    while(Character.isDigit(currentChar)){
                        currentToken+=currentChar;
                        if(reader.hasNext())
                            currentChar=reader.readChar();
                        else
                            throw new TokenizerException("非法结尾");
                    }
                    reader.unReadChar(currentChar);
                    tokens.add(new Token(Type.DECIMAL_LITERAL,Integer.parseInt(currentToken)));
                    continue;
                }
            }


            //字符和数字开头的都考虑了，接下来考虑符号
//<additive-operator>       ::= '+' | '-'
//<multiplicative-operator> ::= '*' | '/'
//<relational-operator>     ::= '<' | '<=' | '>' | '>=' | '!=' | '=='
//<assignment-operator>     ::= '='
//            可见是不用考虑与或非的
            if(currentChar=='+'||currentChar=='-'){
                currentToken+=currentChar;
                tokens.add(new Token(Type.ADDITIVE_OPERATOR,currentToken));
                continue;
            }
            if(currentChar=='*'||currentChar=='/'){
                currentToken+=currentChar;
                tokens.add(new Token(Type.MULTIPLICATIVE_OPERATOR,currentToken));
                continue;
            }
            if(currentChar=='<'||currentChar=='>'||currentChar=='!'){
                currentToken+=currentChar;
                if(currentChar=='!'){
                    if(reader.hasNext())
                        currentChar=reader.readChar();
                    else
                        throw new TokenizerException("非法结尾");
                    if(currentChar!='='){
//                        System.out.println("'！'后面没有'='号");
                        throw new TokenizerException("'！'后面没有'='号");
                    }
                    else {
                        currentToken+=currentChar;
                        tokens.add(new Token(Type.RELATIONAL_OPERATOR,currentToken));
                        continue;
                    }
                }
                else {
                    currentChar = reader.readChar();
                    if(currentChar=='='){
                        currentToken+=currentChar;
                        tokens.add(new Token(Type.RELATIONAL_OPERATOR,currentToken));
                        continue;
                    }
                    else {
                        reader.unReadChar(currentChar);
                        tokens.add(new Token(Type.RELATIONAL_OPERATOR,currentToken));
                        continue;
                    }
                }
            }
            if(currentChar=='='){
                currentToken+=currentChar;
                if(reader.hasNext())
                    currentChar=reader.readChar();
                else
                    throw new TokenizerException("非法结尾");
                if (currentChar=='='){
                    currentToken+=currentChar;
                    tokens.add(new Token(Type.RELATIONAL_OPERATOR,currentToken));
                    continue;
                }
                else {
                    reader.unReadChar(currentChar);
                    tokens.add(new Token(Type.ASSIGNMENT_OPERATOR,currentToken));
                    continue;
                }
            }
//字符 ( ) , { } ;
            currentToken+=currentChar;
            switch (currentChar){
                case '(':
                    tokens.add(new Token(Type.LEFT_PARENTHESES,currentToken));break;
                case ')':
                    tokens.add(new Token(Type.RIGHT_PARENTHESES,currentToken));break;
                case ',':
                    tokens.add(new Token(Type.COMMA,currentToken));break;
                case ';':
                    tokens.add(new Token(Type.SEMICOLON,currentToken));break;
                case '{':
                    tokens.add(new Token(Type.LEFT_BRACKET,currentToken));break;
                case '}':
                    tokens.add(new Token(Type.RIGHT_BRACKET,currentToken));break;
                default:
                    throw new TokenizerException((int)currentChar+"是非法字符！\n");
            }
        }
        return tokens;
    }
}
