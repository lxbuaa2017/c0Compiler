package com.lx.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SourceFileReader {
    private  String filePath;
    private  PushbackReader pushbackReader;

    public  SourceFileReader(String path) throws IOException {
        filePath=path;
        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(filePath)), StandardCharsets.UTF_8); //或GB2312,GB18030
        BufferedReader read = new BufferedReader(isr);
        pushbackReader=new PushbackReader(read);
//        pushbackReader=new PushbackReader(new FileReader(filePath, Charset.forName("utf-8")));

    }

    public  char readChar() throws IOException {
        if(!hasNext())
            throw new RuntimeException("已无字符，不可再读");
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
