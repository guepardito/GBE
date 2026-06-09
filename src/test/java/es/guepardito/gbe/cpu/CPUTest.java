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
}
