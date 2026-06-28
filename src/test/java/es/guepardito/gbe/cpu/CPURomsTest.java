package es.guepardito.gbe.cpu;

import es.guepardito.gbe.cartridge.Cartridge;
import es.guepardito.gbe.memory.Bus;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CPURomsTest {
    @Test
    public void cpu_instrs_Test() throws URISyntaxException {
        String path = Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource("roms/cpu/cpu_instrs.gb")).toURI()
        ).toString();

        StringBuilder output = new StringBuilder();
        CPU cpu = new CPU(new Bus(new Cartridge(path)) {
            @Override
            public void write(int address, int value) {
                super.write(address, value);
                if (address == 0xFF02 && value == 0x81) {
                    char c = (char) read(0xFF01);
                    output.append(c);
                    System.out.print(c);
                }
            }
        });

        for (int i = 0; i < 500_000_000; i++) {
            cpu.step();
            if (output.toString().contains("Passed all") ||
                    output.toString().contains("Failed")) break;
        }
        assertTrue(output.toString().contains("Passed"), "Output: " + output);
    }
}
