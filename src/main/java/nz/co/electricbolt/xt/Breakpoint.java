package nz.co.electricbolt.xt;

import nz.co.electricbolt.xt.cpu.CPU;

public class Breakpoint {
    private final short segment;
    private final short offset;
    private final String condition;
    private boolean hit;

    public Breakpoint(short segment, short offset) {
        this(segment, offset, null);
    }

    public Breakpoint(short segment, short offset, String condition) {
        this.segment = segment;
        this.offset = offset;
        this.condition = condition;
        this.hit = false;
    }

    public boolean checkCondition(CPU cpu) {
        if (condition == null) return true;
        return evaluateCondition(cpu);
    }
    private boolean evaluateCondition(CPU cpu) {
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            String reg = parts[0].trim();
            int value = Integer.parseInt(parts[1].trim(), 16);
            switch (reg.toUpperCase()) {
                case "AX": return cpu.getReg().AX.getValue() == value;
                case "BX": return cpu.getReg().BX.getValue() == value;
                case "CX": return cpu.getReg().CX.getValue() == value;
                case "DX": return cpu.getReg().DX.getValue() == value;
                case "DI": return cpu.getReg().DI.getValue() == value;
                case "SI": return cpu.getReg().SI.getValue() == value;
                case "BP": return cpu.getReg().BP.getValue() == value;
                case "SP": return cpu.getReg().SP.getValue() == value;
                case "ES": return cpu.getReg().ES.getValue() == value;
                case "DS": return cpu.getReg().DS.getValue() == value;
                case "SS": return cpu.getReg().SS.getValue() == value;
                case "FLAGS.CARRY": return cpu.getReg().flags.isCarry() == (value != 0);
                case "FLAGS.ZERO": return cpu.getReg().flags.isZero() == (value != 0);
                case "FLAGS.OVERFLOW": return cpu.getReg().flags.isOverflow() == (value != 0);
            }
        }
        return true;
    }

    public short getSegment() {
        return segment;
    }
    
    public short getOffset() {
        return offset;
    }
    
    public boolean isHit() {
        return hit;
    }
    
    public void setHit(boolean hit) {
        this.hit = hit;
    }
    
    @Override
    public String toString() {
        return String.format("%04X:%04X", segment & 0xFFFF, offset & 0xFFFF);
    }
}
