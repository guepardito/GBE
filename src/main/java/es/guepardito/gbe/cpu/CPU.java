package es.guepardito.gbe.cpu;

import es.guepardito.gbe.memory.Bus;

public class CPU {
    private Registers registers;
    private Bus bus;

    public CPU(Bus bus) {
        this.bus = bus;
        this.registers = new Registers();
    }

    public void step() {

    }
}
