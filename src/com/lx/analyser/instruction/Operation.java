package com.lx.analyser.instruction;

public class Operation {
    public int index;
    public String opCode;
    public Object opRand1;
    public Object opRand2;

    public Operation(int index,String opCode, Object opRand1, Object opRand2) {
        this.index=index;
        this.opCode = opCode;
        this.opRand1 = opRand1;
        this.opRand2 = opRand2;
    }
    @Override
    public String toString() {
        if(opRand1!=null&&opRand2==null)
            return index+"    "+opCode+' '+opRand1;
        else if(opRand1!=null&&opRand2!=null)
            return  index+"    "+opCode+' '+opRand1+','+opRand2;
        else
            return  index+"    "+opCode;
    }

    public void setOperation(int index,String opCode, Object opRand1, Object opRand2) {
        this.index=index;
        this.opCode = opCode;
        this.opRand1 = opRand1;
        this.opRand2 = opRand2;
    }
}
