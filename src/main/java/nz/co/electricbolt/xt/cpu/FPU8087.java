package nz.co.electricbolt.xt.cpu;

public class FPU8087 {

    private final CPU cpu;
    private final double[] st = new double[8];
    private int top;
    private short control;
    private short status;
    private final short[] tag = new short[8];

    public FPU8087(CPU cpu) {
        this.cpu = cpu;
        reset();
    }

    private void updateStatus() {
        status = (short) ((status & ~0x3800) | ((top & 7) << 11));
    }

    private void reset() {
        top = 0;
        control = 0x037F;
        status = 0;
        for (int i = 0; i < 8; i++) { st[i] = 0.0; tag[i] = 0; }
        updateStatus();
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

    private int sti(int i) {
        return (top + i) & 7;
    }

    private double getST(int i) {
        return st[sti(i)];
    }

    private void setST(int i, double val) {
        st[sti(i)] = val;
        tag[sti(i)] = 1;
        status = (short)(status & 0xFF00);
    }

    private void push(double val) {
        top = (top - 1) & 7;
        setST(0, val);
        tag[sti(0)] = 1;
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
            status |= 0x0100; // C0=1, C3=1 unordered
            status |= 0x4000;
            return;
        }
        if (a < b) {
            status |= 0x0100; // C0=1 (less)
        } else if (a > b) {
            status |= 0x4000; // C3=1 (greater)
        } else {
            status |= 0x4000; // equal: C3=1
            status |= 0x0100; // C0=1? Actually equal should have C3=1, C0=0.
            // Clear C0 if needed
            status &= ~0x0100;
        }
    }

    private void comparePop(double a, double b) {
        compare(a, b);
        pop();
    }

    private void d8_mem(int op, int reg, SegOfs addr) {
        double val = readReal32(addr);
        double st0 = getST(0);
        double res = 0;
        boolean pop = false;
        switch (reg) {
            case 0: // fadd
                res = st0 + val;
                setST(0, res);
                break;
            case 1: // fmul
                res = st0 * val;
                setST(0, res);
                break;
            case 2: // fcom
                compare(st0, val);
                break;
            case 3: // fcomp
                comparePop(st0, val);
                break;
            case 4: // fsub
                res = st0 - val;
                setST(0, res);
                break;
            case 5: // fsubr
                res = val - st0;
                setST(0, res);
                break;
            case 6: // fdiv
                res = st0 / val;
                setST(0, res);
                break;
            case 7: // fdivr
                res = val / st0;
                setST(0, res);
                break;
            default:
                return;
        }
        status = (short)(status & 0xFF00);
    }

    private void d9_mem(int op, int reg, SegOfs addr) {
        switch (reg) {
            case 0: // FLD m32real
                push(readReal32(addr));
                break;
            case 1: // FST m32real
                writeReal32(addr, getST(0));
                break;
            case 2: // FSTP m32real
                writeReal32(addr, getST(0));
                pop();
                break;
            case 3:
                writeReal64(addr, getST(0));
                pop();
                break;
            case 4: // FLDENV
                break;
            case 5: // FLDCW
                control = cpu.getMemory().readWord(addr);
                break;
            case 6: // FSTENV
                break;
            case 7: // FSTCW
                cpu.getMemory().writeWord(addr, control);
                break;
            default:
                break;
        }
    }


    private void da_mem(int op, int reg, SegOfs addr) {
    }

    private void db_mem(int op, int reg, SegOfs addr) {
    }

    private void dc_mem(int op, int reg, SegOfs addr) {
        double val = readReal64(addr);
        double st0 = getST(0);
        switch (reg) {
            case 0: setST(0, st0 + val); break;
            case 1: setST(0, st0 - val); break;
            case 2: setST(0, st0 * val); break;
            case 3: setST(0, st0 / val); break;
            default: return;
        }
        status = (short)(status & 0xFF00);
    }

    private void dd_mem(int op, int reg, SegOfs addr) {
    }

    private void de_mem(int op, int reg, SegOfs addr) {
        double val = readReal64(addr);
        double st0 = getST(0);
        switch (reg) {
            case 0: setST(0, st0 + val); break;
            case 1: setST(0, st0 - val); break;
            case 2: setST(0, st0 * val); break;
            case 3: setST(0, st0 / val); break;
            default: return;
        }
        status = (short)(status & 0xFF00);
    }

    private void df_mem(int op, int reg, SegOfs addr) {
        if (reg == 7) {
            cpu.getMemory().writeWord(addr, status);
        }
    }

    private void d8_reg(int op, int reg, int rm) {
        int i = rm;
        double st0 = getST(0);
        double sti = getST(i);
        double res = 0;
        if (reg == 0) {
            res = st0 + sti;
        } else if (reg == 1) {
            res = st0 - sti;
        } else if (reg == 2) {
            res = st0 * sti;
        } else if (reg == 3) {
            res = st0 / sti;
        } else {
            return;
        }
        setST(0, res);
        status = (short)(status & 0xFF00);
    }

    private void d9_reg(int op, int reg, int rm) {
        if (reg == 0) {
            switch (rm) {
                case 0: // fchs
                    setST(0, -getST(0));
                    break;
                case 1: // fabs
                    setST(0, Math.abs(getST(0)));
                    break;
                case 4: // ftst
                    compare(getST(0), 0.0);
                    break;
                case 5: // fld1
                    push(1.0);
                    break;
                case 6: // fldz
                    push(0.0);
                    break;
                default:
                    break;
            }
        } else if (reg == 5 && rm == 0) {
            cpu.getReg().AX.setValue(control);
        } else if (reg == 7 && rm == 0) {
            cpu.getReg().AX.setValue(status);
        }
        status = (short)(status & 0xFF00);
    }

    private void da_reg(int op, int reg, int rm) {
    }

    private void db_reg(int op, int reg, int rm) {
        if (reg == 7 && rm == 3) {
            reset();
        } else if (reg == 7 && rm == 0) {
            // fnop
        }
    }

    private void dc_reg(int op, int reg, int rm) {
        int i = rm;
        double st0 = getST(0);
        double sti = getST(i);
        double res = 0;
        if (reg == 0) {
            res = sti + st0;
        } else if (reg == 1) {
            res = sti - st0;
        } else if (reg == 2) {
            res = sti * st0;
        } else if (reg == 3) {
            res = sti / st0;
        } else {
            return;
        }
        setST(i, res);
        status = (short)(status & 0xFF00);
    }

    private void dd_reg(int op, int reg, int rm) {
    }

    private void de_reg(int op, int reg, int rm) {
        if (reg == 0) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 + sti);
            pop();
        } else if (reg == 1) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 - sti);
            pop();
        } else if (reg == 2) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 * sti);
            pop();
        } else if (reg == 3) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 / sti);
            pop();
        } else if (reg == 4) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 + sti);
            pop();
        } else if (reg == 5) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 - sti);
            pop();
        } else if (reg == 6) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 * sti);
            pop();
        } else if (reg == 7) {
            double st0 = getST(0);
            double sti = getST(rm);
            setST(0, st0 / sti);
            pop();
        }
        status = (short)(status & 0xFF00);
    }

    private void df_reg(int op, int reg, int rm) {
        if (reg == 7 && rm == 0) {
            cpu.getReg().AX.setValue(status);
        }
    }
}
