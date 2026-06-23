package es.guepardito.gbe.cpu;

import es.guepardito.gbe.cartridge.Cartridge;
import es.guepardito.gbe.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CPUTest {
    private byte[] makeRom(int... bytes) {
        byte[] rom = new byte[0x8000];
        for (int i = 0; i < bytes.length; i++) {
            rom[0x0100 + i] = (byte) bytes[i];
        }
        return rom;
    }

    @Test
    public void CPUInitializationTest() {

        CPU cpu = new CPU(new Bus(new Cartridge(makeRom(0x00))));

        assertEquals(0x0100, cpu.getRegisters().getPC());
        assertEquals(0xFFFE, cpu.getRegisters().getSP());
        assertEquals(0x01B0, cpu.getRegisters().getAF());
        assertEquals(0x0013, cpu.getRegisters().getBC());
    }

    @Test
    public void LoadInstructionsTest() {
        byte[] rom = new byte[0x0FFF];
        rom[0x0100] = 0x01; // load BC
        rom[0x0101] = 0x34;
        rom[0x0102] = 0x12;

        rom[0x0103] = 0x11; // load DE
        rom[0x0104] = 0x34;
        rom[0x0105] = 0x12;

        rom[0x0106] = 0x21; // load HL
        rom[0x0107] = 0x34;
        rom[0x0108] = 0x12;

        rom[0x0109] = 0x31; // load SP
        rom[0x010A] = 0x34;
        rom[0x010B] = 0x12;

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));

        cpu.step();
        assertEquals(0x1234, cpu.getRegisters().getBC());

        cpu.step();
        assertEquals(0x1234, cpu.getRegisters().getDE());

        cpu.step();
        assertEquals(0x1234, cpu.getRegisters().getHL());

        cpu.step();
        assertEquals(0x1234, cpu.getRegisters().getSP());
    }

    @Test
    public void Bit8Reg2RegLoadTest() {
        byte[] rom = new byte[0x0FFF];
        rom[0x0100] = 0x47;

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setA(0x12);

        cpu.step();
        assertEquals(0x12, cpu.getRegisters().getB());
    }

    @Test
    public void Bit8Imm2RegLoadTest(){
        byte[] rom = new byte[0x0FFF];
        rom[0x0100] = 0x06; // Load imm to B
        rom[0x0101] = 0x01;

        rom[0x0102] = 0x36; // Load imm to [HL]
        rom[0x0103] = 0x02;

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setHL(0xC000); // Set HL point to WRAM instead of Cartridge

        cpu.step();
        assertEquals(0x01, cpu.getRegisters().getB());

        cpu.step();
        assertEquals(0x02, cpu.getBus().read(cpu.getRegisters().getHL()));
    }

    // TODO: Divide in tests for each instruction
    @Test()
    public void memory2A2memoryLoadTest() {
        byte[] rom = new byte[0x0FFF];
        rom[0x0100] = 0x02; // Load A to Memory at (BC) address
        rom[0x0101] = 0x0A; // Load Memory at (BC) address to A
        rom[0x0102] = 0x22; // Load A to Memory at (HL) address and increment HL
        rom[0x0103] = 0x3A; // Load Memory at (HL) address to A and decrement HL

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setA(0x01);
        cpu.getRegisters().setBC(0xC000); // Set BC point to WRAM
        cpu.getRegisters().setHL(0xC002);

        cpu.step();
        assertEquals(0x01, cpu.getBus().read(cpu.getRegisters().getBC()));

        cpu.step();
        assertEquals(0x01, cpu.getRegisters().getA());

        cpu.step();
        assertEquals(0x01, cpu.getBus().read(cpu.getRegisters().getHL()-1));
        assertEquals(0xC003, cpu.getRegisters().getHL());

        cpu.getRegisters().setHL(0xC002);
        cpu.step();
        assertEquals(0x01, cpu.getRegisters().getA());
        assertEquals(0xC001, cpu.getRegisters().getHL());
    }

    @Test
    public void LD_u16_SP_Test() {
        byte[] rom = makeRom(0x08, 0x00, 0xC0);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setSP(0x1111);

        cpu.step();
        assertEquals(0x11, cpu.getBus().read(0xC000));
        assertEquals(0x11, cpu.getBus().read(0xC001));
    }

    @Test
    public void LD_SP_HL_Test() {
        byte[] rom = makeRom(0xF9);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setHL(0x1111);

        cpu.step();
        assertEquals(0x1111, cpu.getRegisters().getSP());
    }

    @Test
    public void LD_FF00u8_A_Test() {
        byte[] rom = makeRom(0xE0, 0x11);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setA(0x11);

        cpu.step();
        assertEquals(0x11, cpu.getBus().read(0xFF11));
    }

    @Test
    public void LD_A_FF00u8_Test() {
        byte[] rom = makeRom(0xF0, 0x11);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getBus().write(0xFF11, 0x11);

        cpu.step();
        assertEquals(0x11, cpu.getRegisters().getA());
    }

    @Test
    public void LD_FF00C_A_Test() {
        byte[] rom = makeRom(0xE2);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setA(0x11);
        cpu.getRegisters().setC(0x11);

        cpu.step();
        assertEquals(0x11, cpu.getBus().read(0xFF11));
    }

    @Test
    public void LD_A_FF00C_Test() {
        byte[] rom = makeRom(0xF2);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setC(0x11);
        cpu.getBus().write(0xFF11, 0x11);

        cpu.step();
        assertEquals(0x11, cpu.getRegisters().getA());
    }


    @Test
    public void LD_u16_A_Test() {
        byte[] rom = makeRom(0xEA, 0x00, 0xC0);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getRegisters().setA(0x11);

        cpu.step();
        assertEquals(0x11, cpu.getBus().read(0xC000));
    }

    @Test
    public void LD_A_u16_Test() {
        byte[] rom = makeRom(0xFA, 0x00, 0xC0);

        CPU cpu = new CPU(new Bus(new Cartridge(rom)));
        cpu.getBus().write(0xC000, 0x11);

        cpu.step();
        assertEquals(0x11, cpu.getRegisters().getA());
    }
}
