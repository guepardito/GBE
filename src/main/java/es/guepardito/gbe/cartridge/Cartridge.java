package es.guepardito.gbe.cartridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Cartridge {
    private String pathToRom;
    private byte[] rom;

    public Cartridge(String pathToRom) {
        loadCartridge(pathToRom);
    }

    public Cartridge(byte[] rom) {
        this.rom = rom;
    }

    private void loadCartridge(String pathToRom) {
        try {
            Path p = Paths.get(pathToRom);
            rom = Files.readAllBytes(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        boolean isValid = validateChecksum(rom[0x014D]);
        System.out.println("Cartridge is valid? " + isValid);
    }

    // x=0:FOR i=0134h TO 014Ch:x=x-MEM[i]-1:NEXT
    private boolean validateChecksum(byte checksum) {
        int x = 0;
        for (int i = 0x0134; i <= 0x014C; i++) {
            x = x - rom[i] - 1;
        }

        x = x & 0xFF;
        int expectedChecksum = checksum & 0xFF;

        return x == expectedChecksum;
    }

    public int readByte(int address) {
        return rom[address] & 0xFF;
    }
}
