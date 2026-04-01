package nz.co.electricbolt.xt;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.cpu.SegOfs;

public class Watchpoint {

    public enum Type {
        READ, WRITE, ACCESS
    }
    
    private final short segment;
    private final short offset;
    private final Type type;
    private boolean hit;
    
    public Watchpoint(short segment, short offset, Type type) {
        this.segment = segment;
        this.offset = offset;
        this.type = type;
        this.hit = false;
    }
    
    public short getSegment() {
        return segment;
    }
    
    public short getOffset() {
        return offset;
    }
    
    public Type getType() {
        return type;
    }
    
    public boolean isHit() {
        return hit;
    }
    
    public void setHit(boolean hit) {
        this.hit = hit;
    }
    
    public boolean check(CPU cpu, Type accessType, SegOfs address) {
        if (hit) return false;
        if (type != Type.ACCESS && type != accessType) return false;
        
        SegOfs watchAddr = new SegOfs(segment, offset);
        return watchAddr.toLinearAddress() == address.toLinearAddress();
    }
    
    @Override
    public String toString() {
        String typeStr;
        switch (type) {
            case READ: typeStr = "read";
                break;
            case WRITE: typeStr = "write";
                break;
            case ACCESS: typeStr = "access";
                break;
            default: typeStr = "unknown";
        }
        return String.format("%04X:%04X (%s)", segment & 0xFFFF, offset & 0xFFFF, typeStr);
    }
}
