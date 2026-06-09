package es.guepardito.gbe.cpu;

public enum Flag {
    Z(0x80), // 7 Zero Flag, set when the result of a mathematical instruction is zero.
    N(0x40), // 6 Subtraction flag, set when the instruction is a subtraction.
    H(0x20), // 5 Half carry flag, set when a mathematical operation makes the lower 4 bits of a byte overflow.
    C(0x10); // 4 Carry flag, set when a mathematical operation makes a byte overflow.

    public final int value;

    Flag(int value) {
        this.value = value;
    }
}
