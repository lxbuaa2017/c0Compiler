package com.lx.analyser;

public class Operation {
    private String opCode;
    private Object opRand1;
    private Object opRand2;

    public Operation(String opCode, Object opRand1, Object opRand2) {
        this.opCode = opCode;
        this.opRand1 = opRand1;
        this.opRand2 = opRand2;
    }
    @Override
    public String toString() {
        if(opRand1!=null&&opRand2==null)
            return opCode+' '+opRand1;
        else if(opRand1!=null&&opRand2!=null)
            return opCode+' '+opRand1+opRand2;
        else
            return opCode;
    }
}
