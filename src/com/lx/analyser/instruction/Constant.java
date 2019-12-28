package com.lx.analyser.instruction;

public class Constant {
    private int index;
    private char type;
    private String value;

    public Constant(int index,char type, String value) {
        this.index=index;
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return index+" "+type+" "+value;
    }
}
