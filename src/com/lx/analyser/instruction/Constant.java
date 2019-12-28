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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
