package es.guepardito.gbe.cpu;

import es.guepardito.gbe.memory.Bus;

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

        opcodes = new Runnable[256];
        opcodesCB = new Runnable[256];

        for (int i = 0; i < 256; i++) {
            final int opcode = i;
            opcodes[i] = () -> unknownOpcode(opcode);
            opcodesCB[i] = () -> unknownOpcode(opcode);
        }

        buildOpcodeTable();
        buildOpcodeTableCB();
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
    private int getRegisterFromCode(int code) {
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
    private void setRegisterFromCode(int code, int value) {
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
        // nop
        opcodes[0x00] = this::nop;

        // CALL u16
        opcodes[0xCD] = () -> {
            int dest = readNextWord();

            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getPC() >> 8);
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getPC() & 0xFF);

            registers.setPC(dest);
        };

        // CALL NZ, u16
        opcodes[0xC4] = () -> {
            int dest = readNextWord();

            if (registers.getFlag(Flag.Z) == 0) {
                registers.setSP(registers.getSP() - 1);
                bus.write(registers.getSP(), registers.getPC() >> 8);
                registers.setSP(registers.getSP() - 1);
                bus.write(registers.getSP(), registers.getPC() & 0xFF);

                registers.setPC(dest);
            }
        };

        // CALL Z, u16
        opcodes[0xCC] = () -> {
            int dest = readNextWord();

            if (registers.getFlag(Flag.Z) == 1) {
                registers.setSP(registers.getSP() - 1);
                bus.write(registers.getSP(), registers.getPC() >> 8);
                registers.setSP(registers.getSP() - 1);
                bus.write(registers.getSP(), registers.getPC() & 0xFF);

                registers.setPC(dest);
            }
        };

        // RET
        opcodes[0xC9] = () -> {
            int low = bus.read(registers.getSP());
            registers.setSP(registers.getSP() + 1);
            int high = bus.read(registers.getSP());
            registers.setSP(registers.getSP() + 1);

            registers.setPC((high << 8) | low);
        };

        // RET NZ
        opcodes[0xC0] = () -> {
            if (registers.getFlag(Flag.Z) == 0) {
                int low = bus.read(registers.getSP());
                registers.setSP(registers.getSP() + 1);
                int high = bus.read(registers.getSP());
                registers.setSP(registers.getSP() + 1);

                registers.setPC((high << 8) | low);
            }
        };

        // RET Z
        opcodes[0xC8] = () -> {
            if (registers.getFlag(Flag.Z) == 1) {
                int low = bus.read(registers.getSP());
                registers.setSP(registers.getSP() + 1);
                int high = bus.read(registers.getSP());
                registers.setSP(registers.getSP() + 1);

                registers.setPC((high << 8) | low);
            }
        };

        // PUSH BC
        opcodes[0xC5] = () -> {
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getB());
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getC());
        };

        // PUSH DE
        opcodes[0xD5] = () -> {
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getD());
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getE());
        };

        // PUSH HL
        opcodes[0xE5] = () -> {
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getH());
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getL());
        };

        // PUSH AF
        opcodes[0xF5] = () -> {
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getA());
            registers.setSP(registers.getSP() - 1);
            bus.write(registers.getSP(), registers.getF());
        };

        // POP BC
        opcodes[0xC1] = () -> {
            registers.setC(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
            registers.setB(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
        };

        // POP DE
        opcodes[0xD1] = () -> {
            registers.setE(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
            registers.setD(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
        };

        // POP HL
        opcodes[0xE1] = () -> {
            registers.setL(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
            registers.setH(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
        };

        // POP AF
        opcodes[0xF1] = () -> {
            registers.setF(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
            registers.setA(bus.read(registers.getSP()));
            registers.setSP(registers.getSP() + 1);
        };

        // DI
        opcodes[0xF3] = this::nop;

        // EI
        opcodes[0xFB] = this::nop;

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

        // LD (u16), SP
        opcodes[0x08] = () -> {
            int address = readNextWord();
            int lowSP = registers.getSP() & 0xFF;
            int highSP = (registers.getSP() >> 8) & 0xFF;
            bus.write(address, lowSP);
            bus.write(address + 1, highSP);
        };
        // LD SP, HL
        opcodes[0xF9] = () -> registers.setSP(registers.getHL());

        // 8-bit immediate to register loads
        for (int i = 0; i <= 7; i++) {
            int dest = i;
            int opcode = (i << 3) | 0x06;

            opcodes[opcode] = () -> setRegisterFromCode(dest, readNextByte());
        }

        // 8-bit register to register loads
        for (int i = 0x40; i <= 0x7F; i++) {
            if (i == 0x76) continue;
            int dest = i >> 3 & 0x07;
            int src = i & 0x07;

            opcodes[i] = () -> setRegisterFromCode(dest, getRegisterFromCode(src));
        }

        // LD (FF00+u8), A
        opcodes[0xE0] = () -> {
            int imm = readNextByte();
            bus.write(0xFF00 + imm, registers.getA());
        };

        // LD A, (FF00+u8)
        opcodes[0xF0] = () -> {
            int imm = readNextByte();
            registers.setA(bus.read(0xFF00 + imm));
        };

        // LD (FF00+C), A
        opcodes[0xE2] = () -> bus.write(0xFF00 + registers.getC(), registers.getA());

        // LD A, (FF00+C)
        opcodes[0xF2] = () -> registers.setA(bus.read(0xFF00 + registers.getC()));

        // LD (u16), A
        opcodes[0xEA] = () -> {
            int address = readNextWord();
            bus.write(address, registers.getA());
        };

        // LD A, (u16)
        opcodes[0xFA] = () -> {
            int address = readNextWord();
            registers.setA(bus.read(address));
        };

        // JP u16
        opcodes[0xC3] = () -> registers.setPC(readNextWord());

        // JR NZ, u8
        opcodes[0x20] = () -> {
            int offset = readNextByte();
            if (registers.getFlag(Flag.Z) == 0) {
                registers.setPC(registers.getPC() + (byte) offset);
            }
        };

        // JR Z, u8
        opcodes[0x28] = () -> {
            int offset = readNextByte();
            if (registers.getFlag(Flag.Z) == 1) {
                registers.setPC(registers.getPC() + (byte) offset);
            }
        };

        // JR NC, u8
        opcodes[0x30] = () -> {
            int offset = readNextByte();
            if (registers.getFlag(Flag.C) == 0) {
                registers.setPC(registers.getPC() + (byte) offset);
            }
        };

        // JR C, u8
        opcodes[0x38] = () -> {
            int offset = readNextByte();
            if (registers.getFlag(Flag.C) == 1) {
                registers.setPC(registers.getPC() + (byte) offset);
            }
        };

        // JR u8
        opcodes[0x18] = () -> registers.setPC(registers.getPC() + (byte) readNextByte());

        // INC r8
        for (int i = 0; i <= 7; i++) {
            int dest = i;
            int opcode = (dest << 3) | 0x04;

            opcodes[opcode] = () -> {
                int value = getRegisterFromCode(dest);
                int result = value + 1;

                setRegisterFromCode(dest, getRegisterFromCode(dest) + 1);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, (value & 0xF) == 0xF ? 1 : 0);
            };
        }

        // DEC r8
        for (int i = 0; i <= 7; i++) {
            int dest = i;
            int opcode = (dest << 3) | 0x05;

            opcodes[opcode] = () -> {
                int value = getRegisterFromCode(dest);
                int result = value - 1;

                setRegisterFromCode(dest, getRegisterFromCode(dest) - 1);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 1);
                registers.setFlag(Flag.H, (value & 0xF) == 0 ? 1 : 0);
            };
        }

        // INC BC
        opcodes[0x03] = () -> registers.setBC(registers.getBC() + 1);

        // INC DE
        opcodes[0x13] = () -> registers.setDE(registers.getDE() + 1);

        // INC HL
        opcodes[0x23] = () -> registers.setHL(registers.getHL() + 1);

        // INC SP
        opcodes[0x33] = () -> registers.setSP(registers.getSP() + 1);

        // DEC BC
        opcodes[0x0B] = () -> registers.setBC(registers.getBC() - 1);

        // DEC DE
        opcodes[0x1B] = () -> registers.setDE(registers.getDE() - 1);

        // DEC HL
        opcodes[0x2B] = () -> registers.setHL(registers.getHL() - 1);

        // DEC SP
        opcodes[0x3B] = () -> registers.setSP(registers.getSP() - 1);

        // ADD A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0x80 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int a = registers.getA();
                int result = a + operand;

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, ((a & 0xF) + (operand & 0xF)) > 0xF ? 1 : 0);
                registers.setFlag(Flag.C, result > 0xFF ? 1 : 0);
            };
        }

        // ADD A, u8
        opcodes[0xC6] = () -> {
            int operand = readNextByte();
            int a = registers.getA();
            int result = a + operand;

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 0);
            registers.setFlag(Flag.H, ((a & 0xF) + (operand & 0xF)) > 0xF ? 1 : 0);
            registers.setFlag(Flag.C, result > 0xFF ? 1 : 0);
        };

        // ADC A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0x88 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int a = registers.getA();
                int result = a + operand + registers.getFlag(Flag.C);

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, ((a & 0xF) + (operand & 0xF) + registers.getFlag(Flag.C)) > 0xF ? 1 : 0);
                registers.setFlag(Flag.C, result > 0xFF ? 1 : 0);
            };
        }

        // ADC A, u8
        opcodes[0xCE] = () -> {
            int operand = readNextByte();
            int a = registers.getA();
            int result = a + operand + registers.getFlag(Flag.C);

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 0);
            registers.setFlag(Flag.H, ((a & 0xF) + (operand & 0xF) + registers.getFlag(Flag.C)) > 0xF ? 1 : 0);
            registers.setFlag(Flag.C, result > 0xFF ? 1 : 0);
        };

        // SUB A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0x90 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int a = registers.getA();
                int result = a - operand;

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 1);
                registers.setFlag(Flag.H, (a & 0xF) < (operand & 0xF) ? 1 : 0);
                registers.setFlag(Flag.C, result < 0 ? 1 : 0);
            };
        }

        // SUB A, u8
        opcodes[0xD6] = () -> {
            int operand = readNextByte();
            int a = registers.getA();
            int result = a - operand;

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 1);
            registers.setFlag(Flag.H, (a & 0xF) < (operand & 0xF) ? 1 : 0);
            registers.setFlag(Flag.C, result < 0 ? 1 : 0);
        };

        // SBC A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0x98 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int a = registers.getA();
                int result = a - operand - registers.getFlag(Flag.C);

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 1);
                registers.setFlag(Flag.H, (a & 0xF) < ((operand & 0xF) + registers.getFlag(Flag.C)) ? 1 : 0);
                registers.setFlag(Flag.C, result < 0 ? 1 : 0);
            };
        }

        // SBC A, u8
        opcodes[0xDE] = () -> {
            int operand = readNextByte();
            int a = registers.getA();
            int result = a - operand - registers.getFlag(Flag.C);

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 1);
            registers.setFlag(Flag.H, ((a & 0xF) < (operand & 0xF) + registers.getFlag(Flag.C)) ? 1 : 0);
            registers.setFlag(Flag.C, result < 0 ? 1 : 0);
        };

        // AND A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0xA0 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int result = registers.getA() & operand;

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, 1);
                registers.setFlag(Flag.C, 0);
            };
        }

        // AND A, u8
        opcodes[0xE6] = () -> {
            int operand = readNextByte();
            int result = registers.getA() & operand;

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 0);
            registers.setFlag(Flag.H, 1);
            registers.setFlag(Flag.C, 0);
        };

        // OR A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0xB0 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int result = registers.getA() | operand;

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, 0);
                registers.setFlag(Flag.C, 0);
            };
        }

        // OR A, u8
        opcodes[0xF6] = () -> {
            int operand = readNextByte();
            int result = registers.getA() | operand;

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 0);
            registers.setFlag(Flag.H, 0);
            registers.setFlag(Flag.C, 0);
        };

        // XOR A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0xA8 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int result = registers.getA() ^ operand;

                registers.setA(result);

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, 0);
                registers.setFlag(Flag.C, 0);
            };
        }

        // XOR A, u8
        opcodes[0xEE] = () -> {
            int operand = readNextByte();
            int result = registers.getA() ^ operand;

            registers.setA(result);

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 0);
            registers.setFlag(Flag.H, 0);
            registers.setFlag(Flag.C, 0);
        };

        // CP A, r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0xB8 + i;

            opcodes[opcode] = () -> {
                int operand = getRegisterFromCode(code);
                int a = registers.getA();
                int result = a - operand;

                registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
                registers.setFlag(Flag.N, 1);
                registers.setFlag(Flag.H, (a & 0xF) < (operand & 0xF) ? 1 : 0);
                registers.setFlag(Flag.C, result < 0 ? 1 : 0);
            };
        }

        // CP A, u8
        opcodes[0xFE] = () -> {
            int operand = readNextByte();
            int a = registers.getA();
            int result = a - operand;

            registers.setFlag(Flag.Z, (result & 0xFF) == 0 ? 1 : 0);
            registers.setFlag(Flag.N, 1);
            registers.setFlag(Flag.H, (a & 0xF) < (operand & 0xF) ? 1 : 0);
            registers.setFlag(Flag.C, result < 0 ? 1 : 0);
        };

        // RRCA
        opcodes[0x1F] = () -> {
            int b0 = registers.getA() & 0x1;

            registers.setA(registers.getA() >> 1 | b0 << 7);

            registers.setFlag(Flag.Z, 0);
            registers.setFlag(Flag.N, 0);
            registers.setFlag(Flag.H, 0);
            registers.setFlag(Flag.C, b0);
        };


        // 0xCB prefix
        opcodes[0xCB] = this::stepCB;
    }

    private void buildOpcodeTableCB() {
        // RR r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0x18 + code;

            opcodesCB[opcode] = () -> {
                int b0 = getRegisterFromCode(code) & 0x1;
                int newValue = getRegisterFromCode(code) >> 1 | registers.getFlag(Flag.C) << 7;

                setRegisterFromCode(code, newValue);

                registers.setFlag(Flag.Z, (newValue == 0) ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, 0);
                registers.setFlag(Flag.C, b0);
            };
        }

        // SRL r8
        for (int i = 0; i < 8; i++) {
            int code = i;
            int opcode = 0x38 + code;

            opcodesCB[opcode] = () -> {
                int b0 = getRegisterFromCode(code) & 0x1;
                int newValue = getRegisterFromCode(code) >> 1;

                setRegisterFromCode(code, newValue);

                registers.setFlag(Flag.Z, (newValue == 0) ? 1 : 0);
                registers.setFlag(Flag.N, 0);
                registers.setFlag(Flag.H, 0);
                registers.setFlag(Flag.C, b0);
            };
        }
    }

    /**
     * Invoked when an unimplemented opcode is executed.
     *
     * @param opcode Opcode value.
     */
    private void unknownOpcode(int opcode) {
        System.out.printf("Before unknown opcode: 0x%02X at PC: 0x%04X%n", bus.read(registers.getPC()), registers.getPC() - 2);
        System.out.printf("Unknown opcode: 0x%02X at PC: 0x%04X%n \n\n", opcode, registers.getPC() - 1);
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
