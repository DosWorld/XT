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
        control = 0x037F;
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
        tag[idx] = 1;
    }

    private void setST(int i, double val) {
        writeST(i, val);
        if (Double.isNaN(val)) {
            status |= 0x0400;
        } else if (val < 0) {
            status |= 0x0200;
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
            status |= 0x0100;
            status |= 0x4000;
            status |= 0x0400;
            return;
        }
        if (a < b) {
            status |= 0x0100;
        } else if (a > b) {
        } else {
            status |= 0x4000;
        }
    }

    private void comparePop(double a, double b) {
        compare(a, b);
        pop();
    }

    private void checkException(int exceptionBit) {
        status |= exceptionBit;
    }

    private void addST(int i) {
        double res = getST(0) + getST(i);
        if (Double.isInfinite(res) || Double.isNaN(res)) {
            checkException(0x0800);
        }
        setST(0, res);
    }

    private void subST(int i) {
        double res = getST(0) - getST(i);
        if (Double.isInfinite(res) || Double.isNaN(res)) {
            checkException(0x0800);
        }
        setST(0, res);
    }

    private void mulST(int i) {
        double res = getST(0) * getST(i);
        if (Double.isInfinite(res) || Double.isNaN(res)) {
            checkException(0x0800);
        }
        setST(0, res);
    }

    private void divST(int i) {
        double divisor = getST(i);
        double dividend = getST(0);
        if (divisor == 0) {
            if (dividend == 0) {
                status |= 0x0400;
                status |= 0x0100;
                status |= 0x4000;
                checkException(0x0004);
                writeST(0, Double.NaN);
            } else {
                status |= 0x0200;
                checkException(0x0004);
                writeST(0, dividend < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            }
        } else {
            double res = dividend / divisor;
            if (Double.isInfinite(res) || Double.isNaN(res)) {
                checkException(0x0800);
            }
            setST(0, res);
        }
    }

    private void addSTi(int i) {
        double res = getST(i) + getST(0);
        if (Double.isInfinite(res) || Double.isNaN(res)) {
            checkException(0x0800);
        }
        setST(i, res);
    }

    private void subSTi(int i) {
        double res = getST(i) - getST(0);
        if (Double.isInfinite(res) || Double.isNaN(res)) {
            checkException(0x0800);
        }
        setST(i, res);
    }

    private void mulSTi(int i) {
        double res = getST(i) * getST(0);
        if (Double.isInfinite(res) || Double.isNaN(res)) {
            checkException(0x0800);
        }
        setST(i, res);
    }

    private void divSTi(int i) {
        double divisor = getST(i);
        double dividend = getST(0);
        if (divisor == 0) {
            if (dividend == 0) {
                status |= 0x0400;
                status |= 0x0100;
                status |= 0x4000;
                checkException(0x0004);
                writeST(i, Double.NaN);
            } else {
                status |= 0x0200;
                checkException(0x0004);
                writeST(i, dividend < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            }
        } else {
            double res = dividend / divisor;
            if (Double.isInfinite(res) || Double.isNaN(res)) {
                checkException(0x0800);
            }
            setST(i, res);
        }
    }

    private void addPop(int i) {
        addST(i);
        pop();
    }

    private void subPop(int i) {
        subST(i);
        pop();
    }

    private void mulPop(int i) {
        mulST(i);
        pop();
    }

    private void divPop(int i) {
        divST(i);
        pop();
    }

    private void fchs() {
        setST(0, -getST(0));
    }

    private void fabs() {
        setST(0, Math.abs(getST(0)));
    }

    private void fxch(int i) {
        double temp = getST(0);
        setST(0, getST(i));
        setST(i, temp);
    }

    private void fincstp() {
        top = (top - 1) & 7;
        updateStatus();
    }

    private void fdecstp() {
        top = (top + 1) & 7;
        updateStatus();
    }

    private void ffree(int i) {
        tag[sti(i)] = 3;
    }

    private void fld1() {
        push(1.0);
    }

    private void fldz() {
        push(0.0);
    }

    private void fldl2e() {
        push(1.4426950408889634);
    }

    private void fldl2t() {
        push(3.3219280948873626);
    }

    private void fldlg2() {
        push(0.3010299956639812);
    }

    private void fldpi() {
        push(Math.PI);
    }

    private void fldln2() {
        push(0.6931471805599453);
    }

    private void fsin() {
        setST(0, Math.sin(getST(0)));
    }

    private void fcos() {
        setST(0, Math.cos(getST(0)));
    }

    private void fsincos() {
        double a = getST(0);
        setST(0, Math.sin(a));
        push(Math.cos(a));
    }

    private void fptan() {
        double a = getST(0);
        double tan = Math.tan(a);
        double one = 1.0;
        setST(0, one);
        push(tan);
    }

    private void fpatan() {
        double y = getST(1);
        double x = getST(0);
        double result = Math.atan2(y, x);
        pop();
        setST(0, result);
    }

    private void fyl2x() {
        double y = getST(1);
        double x = getST(0);
        if (x <= 0) {
            status |= 0x0400;
            status |= 0x0001;
            checkException(0x0001);
            setST(0, Double.NaN);
            pop();
            return;
        }
        double result = y * (Math.log(x) / Math.log(2));
        pop();
        setST(0, result);
    }

    private void fyl2xp1() {
        double y = getST(1);
        double x = getST(0);
        double arg = 1 + x;
        if (arg <= 0) {
            status |= 0x0400;
            status |= 0x0001;
            checkException(0x0001);
            setST(0, Double.NaN);
            pop();
            return;
        }
        double result = y * (Math.log(arg) / Math.log(2));
        pop();
        setST(0, result);
    }

    private void f2xm1() {
        double x = getST(0);
        double result = Math.pow(2, x) - 1;
        setST(0, result);
    }

    private void fscale() {
        double x = getST(0);
        double y = getST(1);
        long n = (long) Math.floor(y);
        double result = x * Math.pow(2, n);
        setST(0, result);
    }

    private void frndint() {
        double x = getST(0);
        double result = Math.rint(x);
        setST(0, result);
    }

    private void fprem() {
        double x = getST(0);
        double y = getST(1);
        if (y == 0) {
            status |= 0x0400;
            status |= 0x0004;
            checkException(0x0004);
            setST(0, Double.NaN);
            return;
        }
        double result = x % y;
        setST(0, result);
        if (Math.abs(result) >= Math.abs(y)) {
            status |= 0x0400;
        } else {
            status &= ~0x0400;
        }
    }

    private void fxam() {
        double val = getST(0);
        status &= ~0x4500;
        if (Double.isNaN(val)) {
            status |= 0x0100;
            status |= 0x4000;
        } else if (Double.isInfinite(val)) {
            status |= 0x0100;
        } else if (val == 0) {
            status |= 0x4000;
        } else if (val < 0) {
            status |= 0x0200;
        }
    }

    private short readInteger16(SegOfs addr) {
        return cpu.getMemory().readWord(addr);
    }

    private int readInteger32(SegOfs addr) {
        return cpu.getMemory().readWord(addr) & 0xFFFF |
               (cpu.getMemory().readWord(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 2))) << 16);
    }

    private long readInteger64(SegOfs addr) {
        long low = readInteger32(addr) & 0xFFFFFFFFL;
        long high = readInteger32(new SegOfs(addr.getSegment(), (short) (addr.getOffset() + 4))) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    private void writeInteger16(SegOfs addr, short value) {
        cpu.getMemory().writeWord(addr, value);
    }

    private void writeInteger32(SegOfs addr, int value) {
        cpu.getMemory().writeWord(addr, (short) (value & 0xFFFF));
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, (short) ((value >> 16) & 0xFFFF));
    }

    private void writeInteger64(SegOfs addr, long value) {
        writeInteger32(addr, (int) (value & 0xFFFFFFFFL));
        addr.addOffset((short) 4);
        writeInteger32(addr, (int) ((value >> 32) & 0xFFFFFFFFL));
    }

    private double readReal32(SegOfs addr) {
        int bits = cpu.getMemory().readWord(addr) & 0xFFFF;
        addr.addOffset((short) 2);
        bits |= (cpu.getMemory().readWord(addr) << 16);
        return Float.intBitsToFloat(bits);
    }

    private double readReal64(SegOfs addr) {
        long bits = cpu.getMemory().readWord(addr) & 0xFFFF;
        addr.addOffset((short) 2);
        bits |= ((long) cpu.getMemory().readWord(addr) << 16);
        addr.addOffset((short) 2);
        bits |= ((long) cpu.getMemory().readWord(addr) << 32);
        addr.addOffset((short) 2);
        bits |= ((long) cpu.getMemory().readWord(addr) << 48);
        return Double.longBitsToDouble(bits);
    }

    private void writeReal32(SegOfs addr, double value) {
        int bits = Float.floatToIntBits((float) value);
        cpu.getMemory().writeWord(addr, (short) (bits & 0xFFFF));
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, (short) ((bits >> 16) & 0xFFFF));
    }

    private void writeReal64(SegOfs addr, double value) {
        long bits = Double.doubleToLongBits(value);
        cpu.getMemory().writeWord(addr, (short) (bits & 0xFFFF));
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, (short) ((bits >> 16) & 0xFFFF));
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, (short) ((bits >> 32) & 0xFFFF));
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, (short) ((bits >> 48) & 0xFFFF));
    }

    private void storeEnv(SegOfs addr) {
        cpu.getMemory().writeWord(addr, control);
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, status);
        addr.addOffset((short) 2);
        short tagWord = 0;
        for (int i = 0; i < 8; i++) {
            tagWord |= (tag[i] << (i * 2));
        }
        cpu.getMemory().writeWord(addr, tagWord);
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, fpuIp);
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, fpuCs);
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, fpuOp);
        addr.addOffset((short) 2);
        cpu.getMemory().writeWord(addr, fpuOpSeg);
    }

    private void loadEnv(SegOfs addr) {
        control = cpu.getMemory().readWord(addr);
        addr.addOffset((short) 2);
        status = cpu.getMemory().readWord(addr);
        addr.addOffset((short) 2);
        short tagWord = cpu.getMemory().readWord(addr);
        for (int i = 0; i < 8; i++) {
            tag[i] = (tagWord >> (i * 2)) & 3;
        }
        addr.addOffset((short) 2);
        fpuIp = cpu.getMemory().readWord(addr);
        addr.addOffset((short) 2);
        fpuCs = cpu.getMemory().readWord(addr);
        addr.addOffset((short) 2);
        fpuOp = cpu.getMemory().readWord(addr);
        addr.addOffset((short) 2);
        fpuOpSeg = cpu.getMemory().readWord(addr);
        top = (status >> 11) & 7;
    }

    public void execute(int opcode, int modRM) {
        fpuCs = cpu.getReg().CS.getValue();
        fpuIp = cpu.getReg().IP.getValue();
        fpuOp = (short) (opcode << 8 | modRM);
        int group = opcode & 0x7;
        int mod = (modRM >> 6) & 3;
        int reg = (modRM >> 3) & 7;
        int rm = modRM & 7;

        if (mod != 3) {
            SegOfs addr = computeAddress(mod, rm);
            switch (group) {
                case 0: d8_mem(opcode, reg, addr); break;
                case 1: d9_mem(opcode, reg, addr); break;
                case 2: da_mem(opcode, reg, addr); break;
                case 3: db_mem(opcode, reg, addr); break;
                case 4: dc_mem(opcode, reg, addr); break;
                case 5: dd_mem(opcode, reg, addr); break;
                case 6: de_mem(opcode, reg, addr); break;
                case 7: df_mem(opcode, reg, addr); break;
            }
        } else {
            switch (group) {
                case 0: d8_reg(opcode, reg, rm); break;
                case 1: d9_reg(opcode, reg, rm); break;
                case 2: da_reg(opcode, reg, rm); break;
                case 3: db_reg(opcode, reg, rm); break;
                case 4: dc_reg(opcode, reg, rm); break;
                case 5: dd_reg(opcode, reg, rm); break;
                case 6: de_reg(opcode, reg, rm, modRM); break;
                case 7: df_reg(opcode, reg, rm); break;
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
                case 0: offset = cpu.getReg().BX.getValue() + cpu.getReg().SI.getValue(); break;
                case 1: offset = cpu.getReg().BX.getValue() + cpu.getReg().DI.getValue(); break;
                case 2: offset = cpu.getReg().BP.getValue() + cpu.getReg().SI.getValue(); break;
                case 3: offset = cpu.getReg().BP.getValue() + cpu.getReg().DI.getValue(); break;
                case 4: offset = cpu.getReg().SI.getValue(); break;
                case 5: offset = cpu.getReg().DI.getValue(); break;
                case 6: offset = cpu.getReg().BP.getValue(); break;
                case 7: offset = cpu.getReg().BX.getValue(); break;
            }
            if (mod == 1) {
                offset += (byte) cpu.getMemory().fetchByte(new SegOfs(cpu.getReg().CS, cpu.getReg().IP));
                cpu.getReg().IP.add((short) 1);
            } else if (mod == 2) {
                offset += cpu.getMemory().fetchWord(new SegOfs(cpu.getReg().CS, cpu.getReg().IP));
                cpu.getReg().IP.add((short) 2);
            }
            if (seg == null) {
                if (rm == 2 || rm == 3 || rm == 6) seg = cpu.getReg().SS;
                else seg = cpu.getReg().DS;
            }
        }
        return new SegOfs(seg.getValue(), (short) (offset & 0xFFFF));
    }

    private void d8_mem(int op, int reg, SegOfs addr) {
        double val = readReal32(addr);
        double st0 = getST(0);
        switch (reg) {
            case 0: setST(0, st0 + val); break;
            case 1: setST(0, st0 * val); break;
            case 2: compare(st0, val); break;
            case 3: comparePop(st0, val); break;
            case 4: setST(0, st0 - val); break;
            case 5: setST(0, val - st0); break;
            case 6: if (val == 0) { status |= 0x0200; checkException(0x0004); setST(0, st0 / val); } else { setST(0, st0 / val); } break;
            case 7: if (st0 == 0) { status |= 0x0200; checkException(0x0004); setST(0, val / st0); } else { setST(0, val / st0); } break;
            default: break;
        }
    }

    private void d9_mem(int op, int reg, SegOfs addr) {
        switch (reg) {
            case 0: push(readReal32(addr)); break;
            case 1: writeReal32(addr, getST(0)); break;
            case 2: writeReal32(addr, getST(0)); pop(); break;
            case 3: writeReal64(addr, getST(0)); pop(); break;
            case 4: storeEnv(addr); break;
            case 5: control = cpu.getMemory().readWord(addr); break;
            case 6: loadEnv(addr); break;
            case 7: cpu.getMemory().writeWord(addr, control); break;
            default: break;
        }
    }

    private void da_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger32(addr)); break;
            case 1: writeInteger32(addr, (int) Math.rint(st0)); break;
            case 2: writeInteger32(addr, (int) Math.rint(st0)); pop(); break;
            default: break;
        }
    }

    private void db_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger16(addr)); break;
            case 1: writeInteger16(addr, (short) Math.rint(st0)); break;
            case 2: writeInteger16(addr, (short) Math.rint(st0)); pop(); break;
            case 3: push(readInteger32(addr)); break;
            case 4: writeInteger32(addr, (int) Math.rint(st0)); break;
            case 5: writeInteger32(addr, (int) Math.rint(st0)); pop(); break;
            case 6: storeEnv(addr); break;
            case 7: loadEnv(addr); break;
            default: break;
        }
    }

    private void dc_mem(int op, int reg, SegOfs addr) {
        double val = readReal64(addr);
        double st0 = getST(0);
        switch (reg) {
            case 0: setST(0, st0 + val); break;
            case 1: setST(0, st0 - val); break;
            case 2: setST(0, st0 * val); break;
            case 3: if (val == 0) { status |= 0x0200; checkException(0x0004); setST(0, st0 / val); } else { setST(0, st0 / val); } break;
            default: break;
        }
    }

    private void dd_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger64(addr)); break;
            case 1: writeInteger64(addr, Math.round(st0)); break;
            case 2: writeInteger64(addr, Math.round(st0)); pop(); break;
            case 3: push(readReal64(addr)); break;
            case 4: writeReal64(addr, getST(0)); pop(); break;
            default: break;
        }
    }

    private void de_mem(int op, int reg, SegOfs addr) {
        int intVal = readInteger16(addr);
        double st0 = getST(0);
        switch (reg) {
            case 0: setST(0, st0 + intVal); break;
            case 1: setST(0, st0 * intVal); break;
            case 2: setST(0, st0 - intVal); break;
            case 3: compare(st0, intVal); break;
            case 4: comparePop(st0, intVal); break;
            case 5: setST(0, intVal - st0); break;
            case 6: if (intVal == 0) { status |= 0x0200; checkException(0x0004); setST(0, st0 / intVal); } else { setST(0, st0 / intVal); } break;
            case 7: if (st0 == 0) { status |= 0x0200; checkException(0x0004); setST(0, intVal / st0); } else { setST(0, intVal / st0); } break;
            default: break;
        }
    }

    private void df_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger64(addr)); break;
            case 1: writeInteger64(addr, Math.round(st0)); break;
            case 2: writeInteger64(addr, Math.round(st0)); pop(); break;
            case 7: cpu.getMemory().writeWord(addr, status); break;
            default: break;
        }
    }

    private void d8_reg(int op, int reg, int rm) {
        int i = rm;
        switch (reg) {
            case 0: addST(i); break;
            case 1: subST(i); break;
            case 2: compare(getST(0), getST(i)); break;
            case 3: comparePop(getST(0), getST(i)); break;
            case 4: mulST(i); break;
            case 5: divST(i); break;
            default: break;
        }
    }

    private void d9_reg(int op, int reg, int rm) {
        if (reg == 0) {
            switch (rm) {
                case 0: fchs(); break;
                case 1: fabs(); break;
                case 2: fld1(); break;
                case 3: fldl2t(); break;
                case 4: fldl2e(); break;
                case 5: fldpi(); break;
                case 6: fldlg2(); break;
                case 7: fldln2(); break;
                default: break;
            }
        } else if (reg == 1) {
            fxch(rm);
        } else if (reg == 2 && rm == 0) {
            fptan();
        } else if (reg == 3 && rm == 0) {
            fpatan();
        } else if (reg == 4 && rm == 0) {
            fyl2x();
        } else if (reg == 5 && rm == 0) {
            fyl2xp1();
        } else if (reg == 6 && rm == 0) {
            f2xm1();
        } else if (reg == 7 && rm == 0) {
            fxam();
        } else if (reg == 7 && rm == 1) {
            fprem();
        } else if (reg == 7 && rm == 4) {
            frndint();
        } else if (reg == 7 && rm == 5) {
            fxch(1);
        }
    }

    private void da_reg(int op, int reg, int rm) {
    }

    private void db_reg(int op, int reg, int rm) {
        if (reg == 7 && rm == 3) {
            reset();
        }
    }

    private void dc_reg(int op, int reg, int rm) {
        int i = rm;
        switch (reg) {
            case 0: addSTi(i); break;
            case 1: subSTi(i); break;
            case 2: compare(getST(0), getST(i)); break;
            case 3: comparePop(getST(0), getST(i)); break;
            case 4: mulSTi(i); break;
            case 5: divSTi(i); break;
            default: break;
        }
    }

    private void dd_reg(int op, int reg, int rm) {
        if (reg == 7 && rm == 0) {
            frndint();
        } else if (reg == 7 && rm == 4) {
            fxch(1);
        } else if (reg == 7 && rm == 5) {
            fxch(1);
        }
    }

    private void de_reg(int op, int reg, int rm, int modRM) {
        if (reg == 3 && rm == 1) {
            if ((modRM & 0xFF) == 0xF9) {
                divPop(1);
            } else if ((modRM & 0xFF) == 0xD9) {
                comparePop(getST(0), getST(1));
            } else {
                divPop(1);
            }
        } else if (reg >= 0 && reg <= 7) {
            int i = rm;
            switch (reg) {
                case 0: addPop(i); break;
                case 1: subPop(i); break;
                case 2: mulPop(i); break;
                case 3: divPop(i); break;
                default: break;
            }
        }
    }

    private void df_reg(int op, int reg, int rm) {
        if (reg == 7 && rm == 0) {
            cpu.getReg().AX.setValue(status);
        } else if (reg == 0 && rm == 0) {
            fscale();
        }
    }
}
