package com.lx;

import com.lx.error.TokenizerException;
import com.lx.tokenizer.Token;
import com.lx.tokenizer.Tokenizer;
import com.lx.util.SourceFileReader;

import java.io.IOException;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args) throws IOException, TokenizerException {
        ArrayList<Token> result = Tokenizer.tokenizer(new SourceFileReader("F:\\2019秋季\\编译原理\\c0Compiler\\src\\com\\lx\\test.c0"));
        Token a = result.get(0);
        System.out.println(a.toString());
        result.remove(0);
        System.out.println("\n");
        for(Token each:result){
            System.out.println(each.toString());
        }
    }
}
