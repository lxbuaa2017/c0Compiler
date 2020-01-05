package com.lx.tokenizer;

public class Token {
    public Type type;
    public Object value;

    public Token(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value=\"" + value +
                "\"}";
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
