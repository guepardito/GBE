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

        // 8-bit register to register loads
        for (int i = 0x40; i <= 0x7F; i++) {
            if (i == 0x76) continue;
            int dest = i >> 3 & 0x07;
            int src = i & 0x07;

            opcodes[i] = () -> {
                int srcValue;
                switch (src) {
                    case 0b000:
                        srcValue = registers.getB();
                        break;
                    case 0b001:
                        srcValue = registers.getC();
                        break;
                    case 0b010:
                        srcValue = registers.getD();
                        break;
                    case 0b011:
                        srcValue = registers.getE();
                        break;
                    case 0b100:
                        srcValue = registers.getH();
                        break;
                    case 0b101:
                        srcValue = registers.getL();
                        break;
                    case 0b110:
                        srcValue = bus.read(registers.getHL());
                        break;
                    case 0b111:
                        srcValue = registers.getA();
                        break;
                    default:
                        srcValue = 0;
                }

                switch (dest) {
                    case 0b000:
                        registers.setB(srcValue);
                        break;
                    case 0b001:
                        registers.setC(srcValue);
                        break;
                    case 0b010:
                        registers.setD(srcValue);
                        break;
                    case 0b011:
                        registers.setE(srcValue);
                        break;
                    case 0b100:
                        registers.setH(srcValue);
                        break;
                    case 0b101:
                        registers.setL(srcValue);
                        break;
                    case 0b110:
                        bus.write(registers.getHL(), srcValue);
                        break;
                    case 0b111:
                        registers.setA(srcValue);
                        break;
                }

            };
        }

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
