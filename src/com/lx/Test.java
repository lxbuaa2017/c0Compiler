package com.lx;

import com.lx.analyser.Analyser;
import com.lx.error.TokenizerException;
import com.lx.tokenizer.Token;
import com.lx.tokenizer.Tokenizer;
import com.lx.tokenizer.Type;
import com.lx.util.SourceFileReader;
import com.lx.util.TokenReader;

import java.io.*;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args) throws IOException, TokenizerException {
//        SourceFileReader sourceFileReader=new SourceFileReader("/home/lx/IdeaProjects/c0Compiler/src/com/lx/a.c0");
//        while (true){
//            char c=sourceFileReader.readChar();
//            System.out.print(c);
//            if(!sourceFileReader.hasNext())
//                break;
//        }
        ArrayList<Token> result = Tokenizer.tokenizer(new SourceFileReader("/home/lx/IdeaProjects/c0Compiler/src/com/lx/test.c0"));
        for(Token each:result){
            System.out.println(each.toString());
        }
//        try{
//            analyser.c0_program();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        String num = "babe";
//        num=num.substring(2);
//        int decimal = (int)Long.parseLong(num,16);//十六进制转十进制
//        System.out.println(decimal);
//        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File("/home/lx/IdeaProjects/c0Compiler/out/artifacts/c0Compiler_jar/temp")), "utf-8"); //或GB2312,GB18030
//        BufferedReader read = new BufferedReader(isr);
//        PushbackReader pushbackReader=new PushbackReader(read);
//        while (true){
//            int c = pushbackReader.read();
//            if(c==-1)
//                break;
//            System.out.print((char)c);
//        }
    }
}
