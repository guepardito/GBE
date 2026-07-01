package es.guepardito.gbe.cpu;

/**
 * Represents the Game Boy CPU register file.
 *
 * <p>The LR35902 CPU exposes eight 8-bit registers (A, B, C, D, E, F, H, L)
 * and two 16-bit registers (SP and PC). Some 8-bit registers can also be
 * accessed as 16-bit register pairs (AF, BC, DE, HL).</p>
 *
 * <p>Register values are stored as Java integers to avoid signed byte
 * issues. All setters automatically mask values to the appropriate
 * register width.</p>
 */
public class Registers {
    private int A; // Accumulator
    private int B;
    private int C;
    private int D;
    private int E;
    /**
     * F register layout:
     *
     * <pre>
     * Bit 7 - Z (Zero)
     * Bit 6 - N (Subtract)
     * Bit 5 - H (Half Carry)
     * Bit 4 - C (Carry)
     * Bit 3-0 - Unused (always zero)
     * </pre>
     */
    private int F; // Flags
    private int H;
    private int L;

    private int SP;
    private int PC;

    public Registers() {
        setAF(0x01B0);
        setBC(0x0013);
        setDE(0x00D8);
        setHL(0x014D);

        SP = 0xFFFE;
        PC = 0x0100;
    }

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
        return F & 0xF0;
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
    /**
     * Returns the current state of a CPU flag.
     *
     * @param flag Flag to query.
     * @return True if the flag is set.
     */
    public int getFlag(Flag flag) {
        return ((F & flag.value) != 0) ? 1 : 0;
    }

    /**
     * Sets or clears a CPU flag.
     *
     * @param flag Flag to modify.
     * @param value True to set the flag, false to clear it.
     */
    public void setFlag(Flag flag, int value) {
        F = (F & ~flag.value) | ((value & 1) * flag.value);
    }
}
