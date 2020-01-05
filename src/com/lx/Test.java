package com.lx;

import com.lx.analyser.Analyser;
import com.lx.error.TokenizerException;
import com.lx.tokenizer.Token;
import com.lx.tokenizer.Tokenizer;
import com.lx.tokenizer.Type;
import com.lx.util.SourceFileReader;
import com.lx.util.TokenReader;

import java.io.IOException;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args) throws IOException, TokenizerException {
//        ArrayList<Token> result = Tokenizer.tokenizer(new SourceFileReader("/home/lx/IdeaProjects/c0Compiler/src/com/lx/test.c0"));
//        TokenReader reader = new TokenReader(result);
//        Analyser analyser =new Analyser(reader);
//        try{
//            analyser.c0_program();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
        String num = "babe";
//        num=num.substring(2);
        int decimal = (int)Long.parseLong(num,16);//十六进制转十进制
        System.out.println(decimal);
    }
}
