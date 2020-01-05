package com.lx;

import com.lx.analyser.Analyser;
import com.lx.error.TokenizerException;
import com.lx.tokenizer.Token;
import com.lx.tokenizer.Tokenizer;
import com.lx.util.SourceFileReader;
import com.lx.util.TokenReader;

import java.io.*;
import java.util.ArrayList;

public class Main {
    public static String helpInfo = "Usage:\n" +
            "  cc0 [options] input [-o file]\n" +
            "or \n" +
            "  cc0 [-h]\n" +
            "Options:\n" +
            "  -s        将输入的 c0 源代码翻译为文本汇编文件\n" +
            "  -c        将输入的 c0 源代码翻译为二进制目标文件\n" +
            "  -h        显示关于编译器使用的帮助\n" +
            "  -o file   输出到指定的文件 file";
    public static String input="";
    public static String out="out";
    public static void main(String[] args) throws IOException, TokenizerException {
        switch (args.length){
            case 0:
                System.out.println(helpInfo);
                break;
            case 1:
                if(args[0].equals("-h"))
                    System.out.println(helpInfo);
                else{
                    System.out.println("参数不正确");
                    System.out.println(helpInfo);
                }
                break;
            case 2:
                input=args[1];
                if(args[0].equals("-s")||args[0].equals("-c")){

                    ArrayList<Token> result = Tokenizer.tokenizer(new SourceFileReader(input));
                    TokenReader reader = new TokenReader(result);
                    Analyser analyser =new Analyser(reader);
                    try{
                        analyser.c0_program();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else {
                    System.out.println("参数不正确");
                    System.out.println(helpInfo);
                }
        }
    }
}
