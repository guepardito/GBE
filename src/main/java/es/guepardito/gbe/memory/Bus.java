package es.guepardito.gbe.memory;

import es.guepardito.gbe.cartridge.Cartridge;

public class Bus {
    private Cartridge cartridge;
    private byte[] wram;
    private byte[] hram;

    public Bus(Cartridge cartridge) {
        this.cartridge = cartridge;
        wram = new byte[0x2000]; // 8KB of Work RAM
        hram = new byte[0x7F];   // 127 bytes of High RAM
    }

    public int read(int address) {
        if (address >= 0 && address <= 0x7FFF) {
            return cartridge.readByte(address);
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            return wram[address - 0xC000] & 0xFF;
        }  else if (address >= 0xFF80 && address <= 0xFFFE) {
            return hram[address - 0xFF80]  & 0xFF;
        }

        return 0xFF;
    }

    public void write(int address, int value) {
        if (address >= 0 && address <= 0x7FFF) {
            // ROM is read-only, do nothing
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            wram[address - 0xC000] = (byte) value;
        } else if (address >= 0xFF80 && address <= 0xFFFE) {
            hram[address - 0xFF80] = (byte) value;
        }
    }
}
