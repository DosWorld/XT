package nz.co.electricbolt.xt.cpu;

public class FPU8087 {

    private final CPU cpu;
    private final double[] st = new double[8];
    private final int[] tag = new int[8];
    private int top;
    private short control;
    private short status;
    private short fpuIp;
    private short fpuCs;
    private short fpuOp;
    private short fpuOpSeg;

    public FPU8087(CPU cpu) {
        this.cpu = cpu;
        reset();
    }

    public void reset() {
        top = 0;
        control = (short) 0x037F;
        status = 0;
        fpuIp = 0;
        fpuCs = 0;
        fpuOp = 0;
        fpuOpSeg = 0;
        for (int i = 0; i < 8; i++) {
            st[i] = 0.0;
            tag[i] = 3;
        }
        updateStatus();
    }

    private void updateStatus() {
        status = (short) ((status & ~0x3800) | ((top & 7) << 11));
    }

    private int sti(int i) {
        return (top + i) & 7;
    }

    private double getST(int i) {
        int idx = sti(i);
        if (tag[idx] == 3) {
            status |= 0x0400;
            status |= 0x0100;
            return Double.NaN;
        }
        return st[idx];
    }

    private void writeST(int i, double val) {
        int idx = sti(i);
        st[idx] = val;
        tag[idx] = 0;
    }

    private void setST(int i, double val) {
        writeST(i, val);
        if (Double.isNaN(val)) {
            status |= 0x0100;
        }
    }

    private void push(double val) {
        top = (top - 1) & 7;
        setST(0, val);
        updateStatus();
    }

    private void pop() {
        tag[sti(0)] = 3;
        top = (top + 1) & 7;
        updateStatus();
    }

    private void compare(double a, double b) {
        status &= ~0x4500;
        if (Double.isNaN(a) || Double.isNaN(b)) {
            status |= 0x4500;
            return;
        }
        if (a < b) {
            status |= 0x0100;
        } else if (a == b) {
            status |= 0x4000;
        }
    }

    private void comparePop(double a, double b) {
        compare(a, b);
        pop();
    }

    private short readInteger16(SegOfs addr) {
        return cpu.getMemory().readWord(addr);
    }

    private int readInteger32(SegOfs addr) {
        int low = cpu.getMemory().readWord(addr) & 0xFFFF;
        int high = cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2))) & 0xFFFF;
        return (high << 16) | low;
    }

    private long readInteger64(SegOfs addr) {
        long low = (long) readInteger32(addr) & 0xFFFFFFFFL;
        long high = (long) readInteger32(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4))) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    private void writeInteger16(SegOfs addr, short value) {
        cpu.getMemory().writeWord(addr, value);
    }

    private void writeInteger32(SegOfs addr, int value) {
        cpu.getMemory().writeWord(addr, (short) (value & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2)), (short) ((value >> 16) & 0xFFFF));
    }

    private void writeInteger64(SegOfs addr, long value) {
        writeInteger32(addr, (int) (value & 0xFFFFFFFFL));
        writeInteger32(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4)), (int) ((value >> 32) & 0xFFFFFFFFL));
    }

    private double readReal32(SegOfs addr) {
        int low = cpu.getMemory().readWord(addr) & 0xFFFF;
        int high = cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2))) & 0xFFFF;
        int bits = (high << 16) | low;
        return Float.intBitsToFloat(bits);
    }

    private double readReal64(SegOfs addr) {
        long b1 = cpu.getMemory().readWord(addr) & 0xFFFFL;
        long b2 = (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2))) & 0xFFFFL) << 16;
        long b3 = (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4))) & 0xFFFFL) << 32;
        long b4 = (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 6))) & 0xFFFFL) << 48;
        return Double.longBitsToDouble(b4 | b3 | b2 | b1);
    }

    private void writeReal32(SegOfs addr, double value) {
        int bits = Float.floatToIntBits((float) value);
        cpu.getMemory().writeWord(addr, (short) (bits & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2)), (short) ((bits >> 16) & 0xFFFF));
    }

    private void writeReal64(SegOfs addr, double value) {
        long bits = Double.doubleToLongBits(value);
        cpu.getMemory().writeWord(addr, (short) (bits & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2)), (short) ((bits >> 16) & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4)), (short) ((bits >> 32) & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 6)), (short) ((bits >> 48) & 0xFFFF));
    }

    // 80-bit extended precision (10 bytes): bits 79=sign, 78-64=exponent (bias 16383), 63-0=mantissa (explicit integer bit)
    private double readReal80(SegOfs addr) {
        long mLow  = cpu.getMemory().readWord(addr) & 0xFFFFL;
        long mMid1 = (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 2))) & 0xFFFFL) << 16;
        long mMid2 = (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 4))) & 0xFFFFL) << 32;
        long mHigh = (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 6))) & 0xFFFFL) << 48;
        long mantissa = mHigh | mMid2 | mMid1 | mLow;
        int expSign = cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 8))) & 0xFFFF;
        int sign = (expSign >> 15) & 1;
        int exp = expSign & 0x7FFF;

        if (exp == 0 && mantissa == 0) return sign == 0 ? 0.0 : -0.0;
        if (exp == 0x7FFF) return (mantissa << 1) == 0 ? (sign == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY) : Double.NaN;

        // Convert from 80-bit bias (16383) to 64-bit bias (1023), drop explicit integer bit
        int exp64 = exp - 16383 + 1023;
        long frac = mantissa & 0x7FFFFFFFFFFFFFFFL; // drop explicit integer bit
        long bits64;
        if (exp64 <= 0) {
            // Underflow: denormalized or zero
            bits64 = ((long) sign << 63);
        } else if (exp64 >= 0x7FF) {
            // Overflow: infinity
            bits64 = ((long) sign << 63) | (0x7FFL << 52);
        } else {
            bits64 = ((long) sign << 63) | ((long) exp64 << 52) | (frac >>> 11);
        }
        return Double.longBitsToDouble(bits64);
    }

    private void writeReal80(SegOfs addr, double value) {
        long bits64 = Double.doubleToRawLongBits(value);
        int sign = (int)(bits64 >>> 63);
        int exp64 = (int)((bits64 >>> 52) & 0x7FF);
        long frac52 = bits64 & 0x000FFFFFFFFFFFFFL;

        int exp80;
        long mantissa;
        if (exp64 == 0x7FF) {
            exp80 = 0x7FFF;
            mantissa = (frac52 == 0) ? 0x8000000000000000L : 0xC000000000000000L;
        } else if (exp64 == 0) {
            exp80 = 0;
            mantissa = frac52 << 11;
        } else {
            exp80 = exp64 - 1023 + 16383;
            mantissa = 0x8000000000000000L | (frac52 << 11);
        }

        cpu.getMemory().writeWord(addr, (short)(mantissa & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 2)), (short)((mantissa >> 16) & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 4)), (short)((mantissa >> 32) & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 6)), (short)((mantissa >> 48) & 0xFFFF));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 8)), (short)((sign << 15) | exp80));
    }

    private void storeEnv(SegOfs addr) {
        cpu.getMemory().writeWord(addr, control);
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2)), status);
        short tagWord = 0;
        for (int i = 0; i < 8; i++) tagWord |= (tag[i] << (i * 2));
        cpu.getMemory().writeWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4)), tagWord);
    }

    private void loadEnv(SegOfs addr) {
        control = cpu.getMemory().readWord(addr);
        status = cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2)));
        short tagWord = cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4)));
        for (int i = 0; i < 8; i++) tag[i] = (tagWord >> (i * 2)) & 3;
        top = (status >> 11) & 7;
    }

    public boolean hasException() {
        int unmasked = (status & 0x3F) & (~control & 0x3F);
        return unmasked != 0;
    }

    public void execute(int opcode, int modRM) {
        fpuCs = cpu.getReg().CS.getValue();
        fpuIp = (short) (cpu.getReg().IP.getValue() - 2);
        fpuOp = (short) (opcode << 8 | modRM);
        int group = opcode & 0x7;
        int mod = (modRM >> 6) & 3;
        int reg = (modRM >> 3) & 7;
        int rm = modRM & 7;
        if (mod != 3) {
            SegOfs addr = computeAddress(mod, rm);
            switch (group) {
                case 0: d8_mem(reg, addr); break;
                case 1: d9_mem(reg, addr); break;
                case 2: da_mem(reg, addr); break;
                case 3: db_mem(reg, addr); break;
                case 4: dc_mem(reg, addr); break;
                case 5: dd_mem(reg, addr); break;
                case 6: de_mem(reg, addr); break;
                case 7: df_mem(reg, addr); break;
            }
        } else {
            switch (group) {
                case 0: d8_reg(reg, rm); break;
                case 1: d9_reg(reg, rm); break;
                case 2: da_reg(reg, rm); break;
                case 3: db_reg(reg, rm); break;
                case 4: dc_reg(reg, rm); break;
                case 5: dd_reg(reg, rm); break;
                case 6: de_reg(reg, rm); break;
                case 7: df_reg(reg, rm); break;
            }
        }
        cpu.setSegmentOverride(null);
    }

    private SegOfs computeAddress(int mod, int rm) {
        Reg16 seg = cpu.getSegmentOverride();
        int offset = 0;
        if (mod == 0 && rm == 6) {
            offset = cpu.fetch16() & 0xFFFF;
            if (seg == null) seg = cpu.getReg().DS;
        } else {
            switch (rm) {
                case 0: offset = (cpu.getReg().BX.getValue() & 0xFFFF) + (cpu.getReg().SI.getValue() & 0xFFFF); break;
                case 1: offset = (cpu.getReg().BX.getValue() & 0xFFFF) + (cpu.getReg().DI.getValue() & 0xFFFF); break;
                case 2: offset = (cpu.getReg().BP.getValue() & 0xFFFF) + (cpu.getReg().SI.getValue() & 0xFFFF); break;
                case 3: offset = (cpu.getReg().BP.getValue() & 0xFFFF) + (cpu.getReg().DI.getValue() & 0xFFFF); break;
                case 4: offset = (cpu.getReg().SI.getValue() & 0xFFFF); break;
                case 5: offset = (cpu.getReg().DI.getValue() & 0xFFFF); break;
                case 6: offset = (cpu.getReg().BP.getValue() & 0xFFFF); break;
                case 7: offset = (cpu.getReg().BX.getValue() & 0xFFFF); break;
            }
            if (mod == 1) offset += (byte) cpu.fetch8();
            else if (mod == 2) offset += (cpu.fetch16() & 0xFFFF);
            if (seg == null) {
                if (rm == 2 || rm == 3 || (mod != 0 && rm == 6)) seg = cpu.getReg().SS;
                else seg = cpu.getReg().DS;
            }
        }
        return new SegOfs(seg.getValue(), (short) (offset & 0xFFFF));
    }

    private void d8_mem(int reg, SegOfs addr) {
        double val = readReal32(addr);
        switch (reg) {
            case 0: setST(0, getST(0) + val); break;
            case 1: setST(0, getST(0) * val); break;
            case 2: compare(getST(0), val); break;
            case 3: comparePop(getST(0), val); break;
            case 4: setST(0, getST(0) - val); break;
            case 5: setST(0, val - getST(0)); break;
            case 6:
                if (val == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, getST(0) / val);
                }
                break;
            case 7:
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (val == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = val > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (val > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, val / getST(0));
                }
                break;
        }
    }

    private void d9_mem(int reg, SegOfs addr) {
        switch (reg) {
            case 0: push(readReal32(addr)); break;
            case 2: writeReal32(addr, getST(0)); break;
            case 3: writeReal32(addr, getST(0)); pop(); break;
            case 4: loadEnv(addr); break;
            case 5: control = cpu.getMemory().readWord(addr); break;
            case 6: storeEnv(addr); break;
            case 7: cpu.getMemory().writeWord(addr, control); break;
        }
    }

    private void da_mem(int reg, SegOfs addr) {
        double val = (double) readInteger32(addr);
        switch (reg) {
            case 0: setST(0, getST(0) + val); break;
            case 1: setST(0, getST(0) * val); break;
            case 2: compare(getST(0), val); break;
            case 3: comparePop(getST(0), val); break;
            case 4: setST(0, getST(0) - val); break;
            case 5: setST(0, val - getST(0)); break;
            case 6:
                if (val == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, getST(0) / val);
                }
                break;
            case 7:
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (val == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = val > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (val > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, val / getST(0));
                }
                break;
        }
    }

    private void db_mem(int reg, SegOfs addr) {
        switch (reg) {
            case 0: push((double) readInteger32(addr)); break;
            case 2: writeInteger32(addr, (int) roundWithControlWord(getST(0))); break;
            case 3: writeInteger32(addr, (int) roundWithControlWord(getST(0))); pop(); break;
            case 5: push(readReal80(addr)); break;
            case 7: writeReal80(addr, getST(0)); pop(); break;
        }
    }

    private void dc_mem(int reg, SegOfs addr) {
        double val = readReal64(addr);
        switch (reg) {
            case 0: setST(0, getST(0) + val); break;
            case 1: setST(0, getST(0) * val); break;
            case 2: compare(getST(0), val); break;
            case 3: comparePop(getST(0), val); break;
            case 4: setST(0, getST(0) - val); break;
            case 5: setST(0, val - getST(0)); break;
            case 6:
                if (val == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, getST(0) / val);
                }
                break;
            case 7:
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (val == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = val > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (val > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, val / getST(0));
                }
                break;
        }
    }

    private void dd_mem(int reg, SegOfs addr) {
        switch (reg) {
            case 0: push(readReal64(addr)); break;
            case 2: writeReal64(addr, getST(0)); break;
            case 3: writeReal64(addr, getST(0)); pop(); break;
            case 7: cpu.getMemory().writeWord(addr, status); break;
        }
    }

    private void de_mem(int reg, SegOfs addr) {
        double val = (double) readInteger16(addr);
        switch (reg) {
            case 0: setST(0, getST(0) + val); break;
            case 1: setST(0, getST(0) * val); break;
            case 2: compare(getST(0), val); break;
            case 3: comparePop(getST(0), val); break;
            case 4: setST(0, getST(0) - val); break;
            case 5: setST(0, val - getST(0)); break;
            case 6:
                if (val == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, getST(0) / val);
                }
                break;
            case 7:
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (val == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = val > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (val > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, val / getST(0));
                }
                break;
        }
    }

    private void df_mem(int reg, SegOfs addr) {
        switch (reg) {
            case 0: push((double) readInteger16(addr)); break;
            case 2: writeInteger16(addr, (short) roundWithControlWord(getST(0))); break;
            case 3: writeInteger16(addr, (short) roundWithControlWord(getST(0))); pop(); break;
            case 4: push((double) readInteger64(addr)); break;
            case 5: push((double) readInteger64(addr)); break;
            case 6: {
                // FBSTP: store ST(0) as 18-digit packed BCD (10 bytes), then pop
                // Format: bytes 0-8 = 9 pairs of BCD digits (low digit in low nibble),
                //         byte 9 = sign (0x80 if negative, 0x00 if positive)
                double val = getST(0);
                pop();
                long intVal = Math.abs(Math.round(val));
                for (int i = 0; i < 9; i++) {
                    int lo = (int)(intVal % 10); intVal /= 10;
                    int hi = (int)(intVal % 10); intVal /= 10;
                    cpu.getMemory().writeByte(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + i)), (byte)((hi << 4) | lo));
                }
                cpu.getMemory().writeByte(new SegOfs(addr.getSegment(), (short)(addr.getOffset() + 9)), val < 0.0 ? (byte)0x80 : (byte)0x00);
                break;
            }
            case 7: writeInteger64(addr, (long) roundWithControlWord(getST(0))); pop(); break;
        }
    }

    private void d8_reg(int reg, int rm) {
        switch (reg) {
            case 0: setST(0, getST(0) + getST(rm)); break;
            case 1: setST(0, getST(0) * getST(rm)); break;
            case 2: compare(getST(0), getST(rm)); break;
            case 3: comparePop(getST(0), getST(rm)); break;
            case 4: setST(0, getST(0) - getST(rm)); break;
            case 5: setST(0, getST(rm) - getST(0)); break;
            case 6:
                if (getST(rm) == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, getST(0) / getST(rm));
                }
                break;
            case 7:
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (getST(rm) == 0.0) {
                        status |= 0x01;
                        setST(0, Double.NaN);
                    } else {
                        double result = getST(rm) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(0, result);
                        if (getST(rm) > 0) status |= 0x0200;
                    }
                } else {
                    setST(0, getST(rm) / getST(0));
                }
                break;
        }
    }

    private double roundWithControlWord(double val) {
        int rc = (control >> 10) & 3;
        switch (rc) {
            case 0: return Math.rint(val);           // round to nearest (even)
            case 1: return Math.floor(val);           // round toward -infinity
            case 2: return Math.ceil(val);            // round toward +infinity
            case 3: return val < 0 ? Math.ceil(val) : Math.floor(val); // truncate toward zero
            default: return Math.rint(val);
        }
    }

    private void d9_reg(int reg, int rm) {
        switch (reg) {
            case 0: push(getST(rm)); break;
            case 1:
                double tmp = getST(0);
                writeST(0, getST(rm));
                writeST(rm, tmp);
                break;
            case 4:
                switch (rm) {
                    case 0: setST(0, -getST(0)); break; // FCHS
                    case 1: setST(0, Math.abs(getST(0))); break; // FABS
                    case 4: compare(getST(0), 0.0); break; // FTST
                    case 5: { // FXAM: examine ST(0), set C0/C2/C3/C1
                        status &= ~0x4700; // clear C0/C1/C2/C3
                        int idx = sti(0);
                        if (tag[idx] == 3) { // empty: C3=1, C2=0, C0=1
                            status |= 0x4100;
                        } else {
                            double val = st[idx];
                            long bits = Double.doubleToRawLongBits(val);
                            int sign = (int)(bits >>> 63);
                            if (sign != 0) status |= 0x0200; // C1 = sign
                            if (Double.isNaN(val)) {
                                status |= 0x0100; // C0=1 (NaN: C3=0, C2=0, C0=1)
                            } else if (Double.isInfinite(val)) {
                                status |= 0x0500; // C2=1, C0=1 (Infinity: C3=0, C2=1, C0=1)
                            } else if (val == 0.0) {
                                status |= 0x4000; // C3=1 (Zero: C3=1, C2=0, C0=0)
                            } else {
                                status |= 0x0400; // C2=1 (Normal: C3=0, C2=1, C0=0)
                            }
                        }
                        break;
                    }
                }
                break;
            case 5:
                switch (rm) {
                    case 0: push(1.0); break;
                    case 1: push(3.3219280948873623); break;
                    case 2: push(1.4426950408889634); break;
                    case 3: push(Math.PI); break;
                    case 4: push(0.3010299956639812); break;
                    case 5: push(0.6931471805599453); break;
                    case 6: push(0.0); break;
                }
                break;
            case 6:
                switch (rm) {
                    case 0: {                        // F2XM1: ST(0) = 2^ST(0) - 1  (|ST(0)| <= 1)
                        setST(0, Math.pow(2.0, getST(0)) - 1.0);
                        break;
                    }
                    case 1: {                        // FYL2X: ST(1) = ST(1) * log2(ST(0)), pop
                        double x = getST(0), y = getST(1);
                        setST(1, y * (Math.log(x) / Math.log(2.0)));
                        pop();
                        break;
                    }
                    case 2: {                        // FPTAN: ST(0) = tan(ST(0)), push 1.0
                        double val = getST(0);
                        setST(0, Math.tan(val));
                        push(1.0);
                        status &= ~0x0400; // C2=0 (result complete)
                        break;
                    }
                    case 3: {                        // FPATAN: ST(1) = atan2(ST(1), ST(0)), pop
                        double y = getST(1), x = getST(0);
                        setST(1, Math.atan2(y, x));
                        pop();
                        break;
                    }
                    case 4: {                        // FXTRACT
                        double val = getST(0);
                        if (val == 0.0) {
                            push(Double.NEGATIVE_INFINITY);
                        } else {
                            long bits = Double.doubleToRawLongBits(val);
                            int exp = (int)((bits >>> 52) & 0x7FF) - 1023;
                            double sig = Double.longBitsToDouble((bits & 0x800FFFFFFFFFFFFFL) | 0x3FF0000000000000L);
                            setST(0, (double) exp);
                            push(sig);
                        }
                        break;
                    }
                    case 6: // FDECSTP
                        status &= ~0x4500;
                        top = (top - 1) & 7; updateStatus(); break;
                    case 7: // FINCSTP
                        status &= ~0x4500;
                        top = (top + 1) & 7; updateStatus(); break;
                }
                break;
            case 7:
                switch (rm) {
                    case 0: { // FPREM (partial remainder, truncate toward zero)
                        double st0 = getST(0), st1 = getST(1);
                        if (st1 == 0) { status |= 0x0400; break; }
                        double q = st0 < 0 ? Math.ceil(st0 / st1) : Math.floor(st0 / st1);
                        setST(0, st0 - q * st1);
                        status &= ~0x4500;
                        long qi = (long) q;
                        if ((qi & 4) != 0) status |= 0x0100; // C0
                        if ((qi & 2) != 0) status |= 0x4000; // C3
                        if ((qi & 1) != 0) status |= 0x0200; // C1
                        break;
                    }
                    case 1: { // FYL2XP1: ST(1) = ST(1) * log2(ST(0)+1), pop
                        double x = getST(0), y = getST(1);
                        setST(1, y * (Math.log1p(x) / Math.log(2.0)));
                        pop();
                        break;
                    }
                    case 2: setST(0, Math.sqrt(getST(0))); break; // FSQRT
                    case 4: setST(0, roundWithControlWord(getST(0))); break; // FRNDINT
                    case 5: { // FSCALE: ST(0) = ST(0) * 2^trunc(ST(1))
                        double scale = getST(1);
                        setST(0, getST(0) * Math.pow(2.0, (double)(long)scale));
                        break;
                    }
                }
                break;
        }
    }

    private void da_reg(int reg, int rm) {
        if (reg == 5 && rm == 1) { pop(); pop(); }
    }

    private void db_reg(int reg, int rm) {
        if (reg == 4) {
            switch (rm) {
                case 2: status &= ~0x80FF; break; // FNCLEX: clear exception flags and busy
                case 3: reset(); break;            // FNINIT
            }
        }
    }

    private void dc_reg(int reg, int rm) {
        switch (reg) {
            case 0: setST(rm, getST(rm) + getST(0)); break; // FADD ST(rm), ST(0)
            case 1: setST(rm, getST(rm) * getST(0)); break; // FMUL ST(rm), ST(0)
            case 4: setST(rm, getST(0) - getST(rm)); break; // FSUBR ST(rm), ST(0): ST(rm) = ST(0) - ST(rm)
            case 5: setST(rm, getST(rm) - getST(0)); break; // FSUB ST(rm), ST(0): ST(rm) = ST(rm) - ST(0)
            case 6: // FDIVR ST(rm), ST(0): ST(rm) = ST(0) / ST(rm)
                if (getST(rm) == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(rm, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(rm, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(rm, getST(0) / getST(rm));
                }
                break;
            case 7: // FDIV ST(rm), ST(0): ST(rm) = ST(rm) / ST(0)
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (getST(rm) == 0.0) {
                        status |= 0x01;
                        setST(rm, Double.NaN);
                    } else {
                        double result = getST(rm) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(rm, result);
                        if (getST(rm) > 0) status |= 0x0200;
                    }
                } else {
                    setST(rm, getST(rm) / getST(0));
                }
                break;
        }
    }

    private void dd_reg(int reg, int rm) {
        switch (reg) {
            case 0: tag[sti(rm)] = 3; break;
            case 2: writeST(rm, getST(0)); break;
            case 3: writeST(rm, getST(0)); pop(); break;
            case 4: compare(getST(0), getST(rm)); break;
            case 5: comparePop(getST(0), getST(rm)); break;
        }
    }

    private void de_reg(int reg, int rm) {
        switch (reg) {
            case 0: // FADDP ST(rm), ST(0)
                setST(rm, getST(rm) + getST(0));
                pop();
                break;
            case 1: // FMULP ST(rm), ST(0)
                setST(rm, getST(rm) * getST(0));
                pop();
                break;
            case 3: // FCOMPP (only rm=1)
                if (rm == 1) {
                    compare(getST(0), getST(1));
                    pop();
                    pop();
                }
                break;
            case 4: // FSUBRP ST(rm), ST(0): ST(rm) = ST(0) - ST(rm)
                setST(rm, getST(0) - getST(rm));
                pop();
                break;
            case 5: // FSUBP ST(rm), ST(0): ST(rm) = ST(rm) - ST(0)
                setST(rm, getST(rm) - getST(0));
                pop();
                break;
            case 6: // FDIVRP ST(rm), ST(0): ST(rm) = ST(0) / ST(rm)
                if (getST(rm) == 0.0) {
                    status |= 0x04;
                    if (getST(0) == 0.0) {
                        status |= 0x01;
                        setST(rm, Double.NaN);
                    } else {
                        double result = getST(0) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(rm, result);
                        if (getST(0) > 0) status |= 0x0200;
                    }
                } else {
                    setST(rm, getST(0) / getST(rm));
                }
                pop();
                break;
            case 7: // FDIVP ST(rm), ST(0): ST(rm) = ST(rm) / ST(0)
                if (getST(0) == 0.0) {
                    status |= 0x04;
                    if (getST(rm) == 0.0) {
                        status |= 0x01;
                        setST(rm, Double.NaN);
                    } else {
                        double result = getST(rm) > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        setST(rm, result);
                        if (getST(rm) > 0) status |= 0x0200;
                    }
                } else {
                    setST(rm, getST(rm) / getST(0));
                }
                pop();
                break;
        }
    }

    private void df_reg(int reg, int rm) {
        if (reg == 4 && rm == 0) cpu.getReg().AX.setValue(status);
    }
}
