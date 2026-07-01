package es.guepardito.gbe.memory;

import es.guepardito.gbe.cartridge.Cartridge;

/**
 * Represents the Game Boy memory bus.
 *
 * <p>The bus provides a unified interface for all memory accesses
 * performed by the CPU. It is responsible for routing reads and writes
 * to the appropriate hardware component based on the accessed address.</p>
 *
 * <p>This implementation currently supports cartridge ROM, work RAM
 * (WRAM), and high RAM (HRAM). Additional memory regions and hardware
 * devices will be mapped here as the emulator evolves.</p>
 */
public class Bus {
    /** Loaded game cartridge. */
    private final Cartridge cartridge;
    private final int[] vram;
    private final int[] eram;
    /** Internal Work RAM (8 KB). */
    private final int[] wram;
    private final int[] oam;
    private final int[] ioRegisters;
    /** High RAM (127 bytes). */
    private final int[] hram;
    private int ie;

    /**
     * Creates a new memory bus.
     *
     * @param cartridge Cartridge connected to the system.
     */
    public Bus(Cartridge cartridge) {
        this.cartridge = cartridge;
        vram = new int[0x2000]; // 8kb of Video RAM
        eram = new int[0x2000]; // 8kb of External RAM
        wram = new int[0x2000]; // 8KB of Work RAM
        oam = new int[0xA0];
        ioRegisters = new int[0x80]; // 127 bytes of I/O registers
        hram = new int[0x80];   // 127 bytes of High RAM
        ie = 0;
    }

    /**
     * Reads a byte from the Game Boy memory map.
     *
     * <p>The returned value is always an unsigned 8-bit integer in the
     * range {@code 0x00-0xFF}.</p>
     *
     * @param address 16-bit memory address.
     * @return Value stored at the specified address.
     */
    public int read(int address) {
        if (address >= 0 && address <= 0x7FFF) {
            return cartridge.readByte(address);
        } else if (address >= 0x8000 && address <= 0x9FFF) {
            return vram[address - 0x8000] & 0xFF;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            return eram[address - 0xA000] & 0xFF;
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            return wram[address - 0xC000] & 0xFF;
        } else if (address >= 0xE000 && address <= 0xFDFF) {
            return wram[address - 0xE000] & 0xFF; // Echo RAM
        } else if (address >= 0xFE00 && address <= 0xFE9F) {
            return oam[address - 0xFE00] & 0xFF;
        } else if (address >= 0xFF00 && address <= 0xFF7F) {
            return ioRegisters[address - 0xFF00]  & 0xFF;
        }  else if (address >= 0xFF80 && address <= 0xFFFE) {
            return hram[address - 0xFF80]  & 0xFF;
        } else if (address == 0xFFFF) {
            return ie;
        }
        return 0xFF;
    }

    /**
     * Writes a byte to the Game Boy memory map.
     *
     * <p>Writes targeting ROM regions are ignored, as cartridge ROM is
     * read-only from the CPU's perspective.</p>
     *
     * @param address 16-bit memory address.
     * @param value Value to write. Only the lower 8 bits are stored.
     */
    public void write(int address, int value) {
        if (address >= 0 && address <= 0x7FFF) {
            cartridge.writeByte(address, value);
        } else if (address >= 0x8000 && address <= 0x9FFF) {
            vram[address - 0x8000] = (byte) value;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            eram[address - 0xA000] = (byte) value;
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            wram[address - 0xC000] = (byte) value;
        } else if (address >= 0xE000 && address <= 0xFDFF) {
            wram[address - 0xE000] = (byte) value; // Echo RAM
        } else if (address >= 0xFE00 && address <= 0xFE9F) {
            oam[address - 0xFE00] = (byte) value;
        } else if (address >= 0xFF00 && address <= 0xFF7F) {
            ioRegisters[address - 0xFF00] = (byte) value;
        }   else if (address >= 0xFF80 && address <= 0xFFFE) {
            hram[address - 0xFF80] = (byte) value;
        } else if (address == 0xFFFF) {
            // TODO
        }
        // TODO: Hacer el resto que esta en read
    }

    public Cartridge getCartridge() {
        return cartridge;
    }
}
