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

    private void buildOpcodeTable() {
        opcodes = new Runnable[256];
        opcodesCB = new Runnable[256];

        for (int i = 0; i < 256; i++) {
            final int opcode = i;
            opcodes[i] = () -> unkownOpcode(opcode);
            opcodesCB[i] = () -> unkownOpcode(opcode);
        }

        opcodes[0x00] = this::nop;

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
}
