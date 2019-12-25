package com.lx.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;

public class SourceFileReader {
    private  String filePath;
    private  PushbackReader pushbackReader;

    public  SourceFileReader(String path) throws FileNotFoundException {
        filePath=path;
        pushbackReader=new PushbackReader(new FileReader(filePath));
    }

    public  char readChar() throws IOException {
        return (char)pushbackReader.read();
    }

    public  void unReadChar(char unReadChar) throws IOException {
        pushbackReader.unread(unReadChar);
    }

    public  boolean hasNext() throws IOException {
        int i = pushbackReader.read();
        if(i==-1){
            //是文件尾就不需要回退
            return false;
        }
        else {
            //如果不是文件尾需要回退
            pushbackReader.unread(i);
            return true;
        }
    }
}
