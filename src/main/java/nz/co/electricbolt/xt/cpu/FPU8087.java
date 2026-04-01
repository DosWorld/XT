package nz.co.electricbolt.xt.cpu;

public class FPU8087 {

    private final CPU cpu;
    private final double[] st = new double[8];
    private final int[] tag = new int[8];
    private int top;
    private short control;
    private short status;

    public FPU8087(CPU cpu) {
        this.cpu = cpu;
        reset();
    }

    private void reset() {
        top = 0;
        control = 0x037F;
        status = 0;
        for (int i = 0; i < 8; i++) {
            st[i] = 0.0;
            tag[i] = 0;
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
        return st[sti(i)];
    }

    private void setST(int i, double val) {
        int idx = sti(i);
        st[idx] = val;
        tag[idx] = 1;
        status &= 0xFF00;
    }

    private void push(double val) {
        top = (top - 1) & 7;
        setST(0, val);
        updateStatus();
    }

    private void pop() {
        tag[sti(0)] = 0;
        top = (top + 1) & 7;
        updateStatus();
    }

    private void compare(double a, double b) {
        status &= 0xFF00;
        if (Double.isNaN(a) || Double.isNaN(b)) {
            status |= 0x0100;
            status |= 0x4000;
            status |= 0x0400;
            return;
        }
        if (a < b) {
            status |= 0x0100;
        } else if (a > b) {
            status |= 0x4000;
        } else {
            status |= 0x4000;
        }
    }

    private void comparePop(double a, double b) {
        compare(a, b);
        pop();
    }

    private void addST(int i) {
        setST(0, getST(0) + getST(i));
    }

    private void subST(int i) {
        setST(0, getST(0) - getST(i));
    }

    private void mulST(int i) {
        setST(0, getST(0) * getST(i));
    }

    private void divST(int i) {
        setST(0, getST(0) / getST(i));
    }

    private void addSTi(int i) {
        setST(i, getST(i) + getST(0));
    }

    private void subSTi(int i) {
        setST(i, getST(i) - getST(0));
    }

    private void mulSTi(int i) {
        setST(i, getST(i) * getST(0));
    }

    private void divSTi(int i) {
        setST(i, getST(i) / getST(0));
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
        top = (status >> 11) & 7;
    }

    public void execute(int opcode, int modRM) {
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
                case 6: de_reg(opcode, reg, rm); break;
                case 7: df_reg(opcode, reg, rm); break;
            }
        }
    }

    private SegOfs computeAddress(int mod, int rm) {
        Reg16 seg = cpu.getSegmentOverride() == null ? cpu.getReg().DS : cpu.getSegmentOverride();
        int offset;
        if (rm == 0) offset = cpu.getReg().BX.getValue() + cpu.getReg().SI.getValue();
        else if (rm == 1) offset = cpu.getReg().BX.getValue() + cpu.getReg().DI.getValue();
        else if (rm == 2) offset = cpu.getReg().BP.getValue() + cpu.getReg().SI.getValue();
        else if (rm == 3) offset = cpu.getReg().BP.getValue() + cpu.getReg().DI.getValue();
        else if (rm == 4) offset = cpu.getReg().SI.getValue();
        else if (rm == 5) offset = cpu.getReg().DI.getValue();
        else if (rm == 6) offset = cpu.getReg().BP.getValue();
        else offset = cpu.getReg().BX.getValue();
        if (mod == 1) {
            byte disp = cpu.getMemory().fetchByte(new SegOfs(cpu.getReg().CS, cpu.getReg().IP));
            offset += disp;
            cpu.getReg().IP.add((short) 1);
        } else if (mod == 2) {
            short disp = cpu.getMemory().fetchWord(new SegOfs(cpu.getReg().CS, cpu.getReg().IP));
            offset += disp;
            cpu.getReg().IP.add((short) 2);
        }
        return new SegOfs(seg.getValue(), (short) offset);
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
            case 6: setST(0, st0 / val); break;
            case 7: setST(0, val / st0); break;
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
            case 1: writeInteger32(addr, (int) Math.round(st0)); break;
            case 2: writeInteger32(addr, (int) Math.round(st0)); pop(); break;
            default: break;
        }
    }

    private void db_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger16(addr)); break;
            case 1: writeInteger16(addr, (short) Math.round(st0)); break;
            case 2: writeInteger16(addr, (short) Math.round(st0)); pop(); break;
            case 3: push(readInteger32(addr)); break;
            case 4: writeInteger32(addr, (int) Math.round(st0)); break;
            case 5: writeInteger32(addr, (int) Math.round(st0)); pop(); break;
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
            case 3: setST(0, st0 / val); break;
            default: break;
        }
    }

    private void dd_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger64(addr)); break;
            case 1: writeInteger64(addr, Math.round(st0)); break;
            case 2: writeInteger64(addr, Math.round(st0)); pop(); break;
            case 3: break;
            case 4: writeReal64(addr, getST(0)); pop(); break;
            default: break;
        }
    }

    private void de_mem(int op, int reg, SegOfs addr) {
        double st0 = getST(0);
        switch (reg) {
            case 0: push(readInteger16(addr)); break;
            case 1: writeInteger16(addr, (short) Math.round(st0)); break;
            case 2: writeInteger16(addr, (short) Math.round(st0)); pop(); break;
            case 3: compare(st0, readInteger16(addr)); break;
            case 4: comparePop(st0, readInteger16(addr)); break;
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
        double st0 = getST(0);
        double sti = getST(i);
        switch (reg) {
            case 0: setST(0, st0 + sti); break;
            case 1: setST(0, st0 - sti); break;
            case 2: setST(0, st0 * sti); break;
            case 3: setST(0, st0 / sti); break;
            default: break;
        }
    }

    private void d9_reg(int op, int reg, int rm) {
        if (reg == 0) {
            switch (rm) {
                case 0: setST(0, -getST(0)); break;
                case 1: setST(0, Math.abs(getST(0))); break;
                case 4: compare(getST(0), 0.0); break;
                case 5: push(1.0); break;
                case 6: push(0.0); break;
                default: break;
            }
        } else if (reg == 5 && rm == 0) {
            cpu.getReg().AX.setValue(control);
        } else if (reg == 7 && rm == 0) {
            cpu.getReg().AX.setValue(status);
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
        double st0 = getST(0);
        double sti = getST(i);
        switch (reg) {
            case 0: setST(i, sti + st0); break;
            case 1: setST(i, sti - st0); break;
            case 2: setST(i, sti * st0); break;
            case 3: setST(i, sti / st0); break;
            default: break;
        }
    }

    private void dd_reg(int op, int reg, int rm) {
    }

    private void de_reg(int op, int reg, int rm) {
        if (reg >= 0 && reg <= 7) {
            int i = rm;
            double st0 = getST(0);
            double sti = getST(i);
            switch (reg) {
                case 0: setST(0, st0 + sti); pop(); break;
                case 1: setST(0, st0 - sti); pop(); break;
                case 2: setST(0, st0 * sti); pop(); break;
                case 3: setST(0, st0 / sti); pop(); break;
                case 4: setST(0, st0 + sti); pop(); break;
                case 5: setST(0, st0 - sti); pop(); break;
                case 6: setST(0, st0 * sti); pop(); break;
                case 7: setST(0, st0 / sti); pop(); break;
                default: break;
            }
        }
    }

    private void df_reg(int op, int reg, int rm) {
        if (reg == 7 && rm == 0) {
            cpu.getReg().AX.setValue(status);
        }
    }
}
