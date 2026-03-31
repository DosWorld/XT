// Breakpoint.java
package nz.co.electricbolt.xt;

public class Breakpoint {
    private final short segment;
    private final short offset;
    private boolean hit;
    
    public Breakpoint(short segment, short offset) {
        this.segment = segment;
        this.offset = offset;
        this.hit = false;
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
