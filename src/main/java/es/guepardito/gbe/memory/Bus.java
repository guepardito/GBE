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
    /** Internal Work RAM (8 KB). */
    private final byte[] wram;
    /** High RAM (127 bytes). */
    private final byte[] hram;
    private final byte[] ioRegisters;

    /**
     * Creates a new memory bus.
     *
     * @param cartridge Cartridge connected to the system.
     */
    public Bus(Cartridge cartridge) {
        this.cartridge = cartridge;
        wram = new byte[0x2000]; // 8KB of Work RAM
        hram = new byte[0x80];   // 127 bytes of High RAM
        ioRegisters = new byte[0x80]; // 127 bytes of I/O registers
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
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            return wram[address - 0xC000] & 0xFF;
        } else if (address >= 0xFF00 && address <= 0xFF7F) {
            return ioRegisters[address - 0xFF00]  & 0xFF;
        }  else if (address >= 0xFF80 && address <= 0xFFFE) {
            return hram[address - 0xFF80]  & 0xFF;
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
            // ROM is read-only, do nothing
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            wram[address - 0xC000] = (byte) value;
        } else if (address >= 0xFF00 && address <= 0xFF7F) {
            if (address == 0xFF02) {
                System.out.println("FF02 write: " + value);
            }
            ioRegisters[address - 0xFF00] = (byte) value;

            if (address == 0xFF02 && value == 0x81) {
                System.out.print((char) read(0xFF01));
            }
        }   else if (address >= 0xFF80 && address <= 0xFFFE) {
            hram[address - 0xFF80] = (byte) value;
        }
    }

    public Cartridge getCartridge() {
        return cartridge;
    }
}
