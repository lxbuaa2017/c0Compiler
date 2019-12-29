package com.lx.analyser.instruction;

import java.util.ArrayList;

public class Function {
//    .functions:
//0 0（函数名的位置） 4（参数个数） 1（作用域，基础只为0或者1）    # .F0 hanoi
//1 2 0 1    # .F1 main
    private int index;
    public String type;
    public int nameIndex;
    public int paramNum;
    public int level;
    public ArrayList<Operation> operations;
    public Function(int index,int nameIndex, int paramNum, int level) {
        this.index=index;
        this.nameIndex = nameIndex;
        this.paramNum = paramNum;
        this.level = level;
        this.operations = new ArrayList<>();
    }

    public void setOperations(ArrayList<Operation> operations) {
        this.operations = operations;
    }

    @Override
    public String toString() {
        return nameIndex+" "+paramNum+" "+level;
    }
}
