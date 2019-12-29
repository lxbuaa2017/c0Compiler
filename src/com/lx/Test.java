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
//        ArrayList<Token> result = Tokenizer.tokenizer(new SourceFileReader("F:\\2019秋季\\编译原理\\c0Compiler\\src\\com\\lx\\test.c0"));
//        TokenReader reader = new TokenReader(result);
//        Analyser analyser =new Analyser(reader);
//        try{
//            analyser.c0_program();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
        ArrayList<Token> test =new ArrayList<>();
        test.add(new Token(Type.IDENTIFIER,"aaa"));
        test.add(new Token(Type.IDENTIFIER,"bbb"));
        Token a = test.get(0);
        Token b = test.get(1);
        test.remove(0);
        System.out.println(a.toString());
        System.out.println(b.toString());
    }
}
