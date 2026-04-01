package nz.co.electricbolt.xt.cpu;

public class RegSet {

    public final Flags flags = new Flags();
    public final Reg16 AX = new Reg16("AX"); // Accumulator
    public final Reg8 AL = AX.low();
    public final Reg8 AH = AX.high();
    public final Reg16 BX = new Reg16("BX"); // Base
    public final Reg8 BL = BX.low();
    public final Reg8 BH = BX.high();
    public final Reg16 CX = new Reg16("CX"); // Counting
    public final Reg8 CL = CX.low();
    public final Reg8 CH = CX.high();
    public final Reg16 DX = new Reg16("DX"); // Data
    public final Reg8 DL = DX.low();
    public final Reg8 DH = DX.high();
    public final Reg16 SP = new Reg16("SP");
    public final Reg16 BP = new Reg16("BP");
    public final Reg16 SI = new Reg16("SI");
    public final Reg16 DI = new Reg16("DI");
    public final Reg16 IP = new Reg16("IP", (short) 0xFFF0);
    public final Reg16 CS = new Reg16("CS",  (short) 0xF000); // Code segment
    public final Reg16 DS = new Reg16("DS"); // Data segment
    public final Reg16 SS = new Reg16("SS"); // Stack segment
    public final Reg16 ES = new Reg16("ES"); // Extra segment

    public String toString() {
        return CS +
                " " + IP +
                " " + flags +
                " " + AX +
                " " + BX +
                " " + CX +
                " " + DX +
                " " + DS +
                " " + SI +
                " " + ES +
                " " + DI +
                " " + SS +
                " " + SP +
                " " + BP;
    }

    public RegSet clone() {
        RegSet copy = new RegSet();
        copy.AX.setValue(AX.getValue());
        copy.BX.setValue(BX.getValue());
        copy.CX.setValue(CX.getValue());
        copy.DX.setValue(DX.getValue());
        copy.SP.setValue(SP.getValue());
        copy.BP.setValue(BP.getValue());
        copy.SI.setValue(SI.getValue());
        copy.DI.setValue(DI.getValue());
        copy.CS.setValue(CS.getValue());
        copy.DS.setValue(DS.getValue());
        copy.ES.setValue(ES.getValue());
        copy.SS.setValue(SS.getValue());
        copy.flags.setValue16(flags.getValue16());
        return copy;
    }

    public void setFrom(RegSet other) {
        AX.setValue(other.AX.getValue());
        BX.setValue(other.BX.getValue());
        CX.setValue(other.CX.getValue());
        DX.setValue(other.DX.getValue());
        SP.setValue(other.SP.getValue());
        BP.setValue(other.BP.getValue());
        SI.setValue(other.SI.getValue());
        DI.setValue(other.DI.getValue());
        CS.setValue(other.CS.getValue());
        DS.setValue(other.DS.getValue());
        ES.setValue(other.ES.getValue());
        SS.setValue(other.SS.getValue());
        flags.setValue16(other.flags.getValue16());
    }
}