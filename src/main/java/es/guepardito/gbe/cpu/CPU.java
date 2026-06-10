package es.guepardito.gbe.cpu;

import es.guepardito.gbe.memory.Bus;

import javax.tools.OptionChecker;

/**
 * Emulates the Sharp LR35902 CPU used by the Nintendo Game Boy.
 *
 * <p>The CPU executes instructions by performing the classic
 * fetch-decode-execute cycle. Instructions are dispatched through
 * precomputed opcode tables for both standard and CB-prefixed opcodes.</p>
 *
 * <p>This implementation currently focuses on instruction execution and
 * does not yet emulate timing, interrupts, HALT, or STOP states.</p>
 */
public class CPU {
    /** CPU registers. */
    private final Registers registers;
    /** Memory bus used by the CPU. */
    private final Bus bus;
    /** Main opcode dispatch table. */
    private Runnable[] opcodes;
    /** CB-prefixed opcode dispatch table. */
    private Runnable[] opcodesCB;

    public CPU(Bus bus) {
        this.bus = bus;
        this.registers = new Registers();
        buildOpcodeTable();
    }

    /**
     * Executes a single CPU instruction.
     *
     * <p>Performs the fetch-decode-execute cycle by reading the opcode
     * at the current program counter and dispatching it through the
     * opcode table.</p>
     */
    public void step() {
        // Fetch
        int opcode = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        opcodes[opcode].run();
    }

    /**
     * Reads the next byte from memory and advances the program counter.
     *
     * @return Unsigned 8-bit value.
     */
    public int readNextByte() {
        int _byte = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        return _byte;
    }

    /**
     * Reads the next 16-bit little-endian value from memory and advances
     * the program counter by two bytes.
     *
     * @return Unsigned 16-bit value.
     */
    public int readNextWord() {
        int low = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        int high = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        return (high << 8) | low;
    }

    /**
     * Resolves a register code used by many LR35902 instructions.
     *
     * <p>The encoding follows the Game Boy opcode format:</p>
     *
     * <pre>
     * 000 = B
     * 001 = C
     * 010 = D
     * 011 = E
     * 100 = H
     * 101 = L
     * 110 = (HL)
     * 111 = A
     * </pre>
     *
     * @param code Register encoding.
     * @return Register value or memory contents at HL.
     */
    private int getRegister(int code) {
        return switch (code) {
            case 0b000 -> registers.getB();
            case 0b001 -> registers.getC();
            case 0b010 -> registers.getD();
            case 0b011 -> registers.getE();
            case 0b100 -> registers.getH();
            case 0b101 -> registers.getL();
            case 0b110 -> bus.read(registers.getHL());
            case 0b111 -> registers.getA();
            default -> 0;
        };
    }

    /**
     * Writes a value to a register specified by an LR35902 register code.
     *
     * <p>When the code corresponds to (HL), the value is written to memory
     * instead of a CPU register.</p>
     *
     * @param code Register encoding.
     * @param value Value to write.
     */
    private void setRegister(int code, int value) {
        switch (code) {
            case 0b000 -> registers.setB(value);
            case 0b001 -> registers.setC(value);
            case 0b010 -> registers.setD(value);
            case 0b011 -> registers.setE(value);
            case 0b100 -> registers.setH(value);
            case 0b101 -> registers.setL(value);
            case 0b110 -> bus.write(registers.getHL(), value);
            case 0b111 -> registers.setA(value);
        }
    }

    /**
     * Builds the opcode dispatch tables.
     *
     * <p>All entries are initialized to an unknown opcode handler and then
     * replaced with implemented instruction handlers.</p>
     *
     * <p>The CB-prefixed instruction set uses a separate dispatch table.</p>
     */
    private void buildOpcodeTable() {
        opcodes = new Runnable[256];
        opcodesCB = new Runnable[256];

        for (int i = 0; i < 256; i++) {
            final int opcode = i;
            opcodes[i] = () -> unknownOpcode(opcode);
            opcodesCB[i] = () -> unknownOpcode(opcode);
        }

        // nop
        opcodes[0x00] = this::nop;
        // LD BC, u16
        opcodes[0x01] = () -> registers.setBC(readNextWord());
        // LD DE, u16
        opcodes[0x11] = () -> registers.setDE(readNextWord());
        // LD HL, u16
        opcodes[0x21] = () -> registers.setHL(readNextWord());
        // LD SP, u16
        opcodes[0x31] = () -> registers.setSP(readNextWord());

        // LD (BC), A
        opcodes[0x02] = () -> bus.write(registers.getBC(), registers.getA());
        // LD (DE), A
        opcodes[0x12] = () -> bus.write(registers.getDE(), registers.getA());
        // LD (HL+), A
        opcodes[0x22] = () -> {
            bus.write(registers.getHL(), registers.getA());
            registers.setHL(registers.getHL() + 1);
        };
        // LD (HL-), A
        opcodes[0x32] = () -> {
            bus.write(registers.getHL(), registers.getA());
            registers.setHL(registers.getHL() - 1);
        };

        // LD A, (BC)
        opcodes[0x0A] = () -> registers.setA(bus.read(registers.getBC()));
        // LD A, (DE)
        opcodes[0x1A] = () -> registers.setA(bus.read(registers.getDE()));
        // LD A, (HL+)
        opcodes[0x2A] = () -> {
            registers.setA(bus.read(registers.getHL()));
            registers.setHL(registers.getHL() + 1);
        };
        // LD A, (HL-)
        opcodes[0x3A] = () -> {
            registers.setA(bus.read(registers.getHL()));
            registers.setHL(registers.getHL() - 1);
        };

        // 8-bit immediate to register loads
        for (int i = 0; i <= 7; i++) {
            int dest = i;
            int opcode = (i << 3) | 0x06;

            opcodes[opcode] = () -> setRegister(dest, readNextByte());
        }

        // 8-bit register to register loads
        for (int i = 0x40; i <= 0x7F; i++) {
            if (i == 0x76) continue;
            int dest = i >> 3 & 0x07;
            int src = i & 0x07;

            opcodes[i] = () -> setRegister(dest, getRegister(src));
        }

        // 0xCB prefix
        opcodes[0xCB] = this::stepCB;
    }

    /**
     * Invoked when an unimplemented opcode is executed.
     *
     * @param opcode Opcode value.
     */
    private void unknownOpcode(int opcode) {
        throw new IllegalArgumentException("Unknown opcode " + opcode);
    }

    /**
     * Executes a CB-prefixed instruction.
     *
     * <p>The CB prefix extends the base instruction set with bit operations,
     * shifts, rotates, and other extended instructions.</p>
     */
    private void stepCB() {
        int opcode = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        opcodesCB[opcode].run();
    }

    /**
     * No Operation.
     *
     * <p>Consumes one instruction without modifying CPU state.</p>
     */
    private void nop() {}


    public Registers getRegisters() {
        return registers;
    }

    public Bus getBus() {
        return bus;
    }
}
