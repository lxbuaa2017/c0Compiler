package com.lx;

import com.lx.analyser.Analyser;
import com.lx.error.TokenizerException;
import com.lx.tokenizer.Token;
import com.lx.tokenizer.Tokenizer;
import com.lx.util.SourceFileReader;
import com.lx.util.TokenReader;

import java.io.IOException;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args) throws IOException, TokenizerException {
        ArrayList<Token> result = Tokenizer.tokenizer(new SourceFileReader("F:\\2019秋季\\编译原理\\c0Compiler\\src\\com\\lx\\test.c0"));
        TokenReader reader = new TokenReader(result);
        Analyser analyser =new Analyser(reader);
        try{
            analyser.c0_program();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
