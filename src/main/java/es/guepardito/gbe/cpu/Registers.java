package es.guepardito.gbe.cpu;

public class Registers {
    int A; // Accumulator
    int B;
    int C;
    int D;
    int E;
    int F; // Flags
    int H;
    int L;

    int SP;
    int PC;

    // Getters and Setters
    // Registers
    public int getA() {
        return A;
    }

    public void setA(int a) {
        A = a & 0xFF;
    }

    public int getB() {
        return B;
    }

    public void setB(int b) {
        B = b & 0xFF;
    }

    public int getC() {
        return C;
    }

    public void setC(int c) {
        C = c & 0xFF;
    }

    public int getD() {
        return D;
    }

    public void setD(int d) {
        D = d & 0xFF;
    }

    public int getE() {
        return E;
    }

    public void setE(int e) {
        E = e & 0xFF;
    }

    public int getF() {
        return F;
    }

    public void setF(int f) {
        F = f & 0xF0;
    }

    public int getH() {
        return H;
    }

    public void setH(int h) {
        H = h & 0xFF;
    }

    public int getL() {
        return L;
    }

    public void setL(int l) {
        L = l & 0xFF;
    }

    public int getSP() {
        return SP;
    }

    public void setSP(int SP) {
        this.SP = SP & 0xFFFF;
    }

    public int getPC() {
        return PC;
    }

    public void setPC(int PC) {
        this.PC = PC & 0xFFFF;
    }

    public int getAF() {
        return (A << 8) | F;
    }

    public void setAF(int AF) {
        A = (AF >> 8) & 0xFF;
        F = AF & 0xF0;
    }

    public int getBC() {
        return (B << 8) | C;
    }

    public void setBC(int BC) {
        B = (BC >> 8) & 0xFF;
        C = BC & 0xFF;
    }

    public int getDE() {
        return (D << 8) | E;
    }

    public void setDE(int DE) {
        D = (DE >> 8) & 0xFF;
        E = DE & 0xFF;
    }

    public int getHL() {
        return (H << 8) | L;
    }

    public void setHL(int HL) {
        H = (HL >> 8) & 0xFF;
        L = HL & 0xFF;
    }

    // Flags
    public boolean getFlag(Flag flag) {
        return (flag.value & F) == flag.value;
    }

    public void setFlag(Flag flag, boolean value) {
        F = (F & ~flag.value) | (value ? flag.value : 0);
    }
}
