package com.lx.error;

public class TokenizerException extends Exception{
//    private  int row;
//    private  int col;
//    public CompilationException(int row,int col){
//        this.row=row;
//        this.col=col;
//    }
//
//    @Override
//    public String toString() {
//        return "CompilationException{" +
//                "row=" + row +
//                ", col=" + col +
//                '}';
//    }

    public TokenizerException(String message) {
        super(message);
        System.out.println("这是词法分析错误");
    }
}
