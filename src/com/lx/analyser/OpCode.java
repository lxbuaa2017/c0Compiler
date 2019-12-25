package com.lx.analyser;

public enum OpCode {
    NOP("nop"),
    BIPUSH("bipush"),
    IPUSH("ipush"),
    POPN("popN"),
    DUP("dup"),
    DUP2("dup2"),
    LOADC("loadc"),
    LOADA("loada"),
    NEW("new"),
    SNEW("snew");
    //用时再加

    private final String opCode;

    OpCode(String opCode) {
        this.opCode = opCode;
    }

    @Override
    public String toString() {
        return this.opCode;
    }
}
