package es.guepardito.gbe.cpu;

import es.guepardito.gbe.cartridge.Cartridge;
import es.guepardito.gbe.memory.Bus;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CPURomsTest {
    private void runBlargg(String rom) throws URISyntaxException {
        String path = Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource("roms/cpu/" + rom)).toURI()
        ).toString();

        StringBuilder output = new StringBuilder();
        CPU cpu = new CPU(new Bus(new Cartridge(path)) {
            @Override
            public void write(int address, int value) {
                super.write(address, value);
                if (address == 0xFF02 && value == 0x81) {
                    char c = (char) read(0xFF01);
                    output.append(c);
                    System.out.printf("[0x%02X '%c']", (int)c, c >= 32 ? c : '?');
                }
            }
        });

        for (int i = 0; i < 500_000_000; i++) {
            if (i % 10_000_000 == 0) {
                System.out.printf("i=%d PC=0x%04X%n", i, cpu.getRegisters().getPC());
            }
            cpu.step();
            if (output.toString().contains("Passed") || output.toString().contains("Failed")) {
                System.out.println("\n");
                break;
            }
        }
        assertTrue(output.toString().contains("Passed"), "Output: " + output);
    }

//    @Test public void test01() throws URISyntaxException { runBlargg("individual/01-special.gb"); }
//    @Test public void test02() throws URISyntaxException { runBlargg("individual/02-interrupts.gb"); }
//    @Test public void test03() throws URISyntaxException { runBlargg("individual/03-op sp,hl.gb"); }
//    @Test public void test04() throws URISyntaxException { runBlargg("individual/04-op r,imm.gb"); }
//    @Test public void test05() throws URISyntaxException { runBlargg("individual/05-op rp.gb"); }
    @Test public void test06() throws URISyntaxException { runBlargg("individual/06-ld r,r.gb"); }
//    @Test public void test07() throws URISyntaxException { runBlargg("individual/07-jr,jp,call,ret,rst.gb"); }
//    @Test public void test08() throws URISyntaxException { runBlargg("individual/08-misc instrs.gb"); }
//    @Test public void test09() throws URISyntaxException { runBlargg("individual/09-op r,r.gb"); }
//    @Test public void test10() throws URISyntaxException { runBlargg("individual/10-bit ops.gb"); }
//    @Test public void test11() throws URISyntaxException { runBlargg("individual/11-op a,(hl).gb"); }
//    @Test public void test12() throws URISyntaxException { runBlargg("cpu_instrs.gb"); }

}
