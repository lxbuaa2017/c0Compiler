package com.lx.error;

public class AnalyserException extends Exception {
    public AnalyserException(String message) {
        super(message);
        System.out.println("这是语法分析错误");
    }
}
