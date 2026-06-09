package es.guepardito.gbe.cpu;

import es.guepardito.gbe.memory.Bus;

public class CPU {
    private Registers registers;
    private Bus bus;
    private Runnable[] opcodes;
    private Runnable[] opcodesCB;

    public CPU(Bus bus) {
        this.bus = bus;
        this.registers = new Registers();
        buildOpcodeTable();
    }

    public void step() {
        // Fetch
        int opcode = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        opcodes[opcode].run();
    }

    public int readNextByte() {
        int _byte = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        return _byte;
    }

    public int readNextWord() {
        int low = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        int high = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        return (high << 8) | low;
    }

    private void buildOpcodeTable() {
        opcodes = new Runnable[256];
        opcodesCB = new Runnable[256];

        for (int i = 0; i < 256; i++) {
            final int opcode = i;
            opcodes[i] = () -> unkownOpcode(opcode);
            opcodesCB[i] = () -> unkownOpcode(opcode);
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

        opcodes[0xCB] = this::stepCB;
    }

    private void unkownOpcode(int opcode) {
        throw new IllegalArgumentException("Unknown opcode " + opcode);
    }

    private void stepCB() {
        int opcode = bus.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        opcodesCB[opcode].run();
    }

    private void nop() {}


    public Registers getRegisters() {
        return registers;
    }
}
