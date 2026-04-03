package nz.co.electricbolt.xt;

public class DumpRegion {
    private final short segment;
    private final short offset;
    private final int length;   // в байтах, 0..65535

    public DumpRegion(short segment, short offset, int length) {
        this.segment = segment;
        this.offset = offset;
        this.length = length;
    }

    public short getSegment() { return segment; }
    public short getOffset() { return offset; }
    public int getLength() { return length; }

    @Override
    public String toString() {
        return String.format("%04X:%04X len=%d", segment & 0xFFFF, offset & 0xFFFF, length);
    }
}
