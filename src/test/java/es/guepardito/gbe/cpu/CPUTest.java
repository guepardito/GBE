package es.guepardito.gbe.cpu;

import es.guepardito.gbe.cartridge.Cartridge;
import es.guepardito.gbe.memory.Bus;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CPUTest {
    @Test
    public void CPUInitialization() throws URISyntaxException {
        String path = Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource("roms/cpu_instrs.gb")).toURI()
        ).toString();
        CPU cpu = new CPU(new Bus(new Cartridge(path)));

        assertEquals(0x0100, cpu.getRegisters().getPC());
        assertEquals(0xFFFE, cpu.getRegisters().getSP());
        assertEquals(0x01B0, cpu.getRegisters().getAF());
        assertEquals(0x0013, cpu.getRegisters().getBC());
    }

    @Test
    public void LoadInstructions() {
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
}
