package com.lx.util;

import com.lx.tokenizer.Token;

import java.util.ArrayList;

public class TokenReader {
    private ArrayList<Token> tokens;
    private ArrayList<Token> pushBack;
    public TokenReader(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.pushBack = new ArrayList<>();
    }
    public Token readToken(){
        if(tokens.size()==0)
            return null;
        else {
            Token result = tokens.get(0);
            tokens.remove(0);
            pushBack.add(result);
            return result;
        }
    }
    public void unreadToken(Token token){
        tokens.add(0,token);
    }
    public void pushBackTokens(){
        int len = pushBack.size();
        for(int i = len-1;i>=0;i--){
            tokens.add(0,pushBack.get(i));
        }
        pushBack.clear();
    }
    public void clearPushBack(){
        pushBack.clear();
    }
}
