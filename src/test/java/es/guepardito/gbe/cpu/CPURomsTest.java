package es.guepardito.gbe.cpu;

import es.guepardito.gbe.cartridge.Cartridge;
import es.guepardito.gbe.memory.Bus;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

public class CPURomsTest {
    @Test
    public void LD_R_R() throws URISyntaxException {
        String path = Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource("roms/cpu/individual/06-ld r,r.gb")).toURI()
        ).toString();

        CPU cpu = new CPU(new Bus(new Cartridge(path)));
        for (int i = 0; i < 1000000; i++) {
            cpu.step();
        }
    }
}
