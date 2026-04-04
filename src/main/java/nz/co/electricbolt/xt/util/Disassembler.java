package nz.co.electricbolt.xt.util;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.cpu.SegOfs;

public class Disassembler {

    private final CPU cpu;
    private SegOfs addr;
    private int startOffset;

    public Disassembler(CPU cpu) {
        this.cpu = cpu;
    }

    public String disassemble() {
        startOffset = cpu.getReg().IP.getValue();
        addr = new SegOfs(cpu.getReg().CS.getValue(), (short) startOffset);
        return disassembleWithPrefixes();
    }

    private String disassembleWithPrefixes() {
        StringBuilder prefixStr = new StringBuilder();
        while (true) {
            byte b = fetch8();
            int op = b & 0xFF;
            if (op == 0x26) prefixStr.append("es:");
            else if (op == 0x2E) prefixStr.append("cs:");
            else if (op == 0x36) prefixStr.append("ss:");
            else if (op == 0x3E) prefixStr.append("ds:");
            else if (op == 0xF0) prefixStr.append("lock ");
            else if (op == 0xF2) prefixStr.append("repnz ");
            else if (op == 0xF3) prefixStr.append("repz ");
            else {
                startOffset = addr.getOffset();
                String inner = disassembleOpcode((byte) op);
                if (inner.startsWith(prefixStr.toString()))
                    return inner;
                else
                    return prefixStr.toString() + inner;
            }
        }
    }

    private String hexByte(int b) {
        return String.format("0x%02X", b & 0xFF);
    }

    private String hexWord(int w) {
        return String.format("0x%04X", w & 0xFFFF);
    }

    private String disassembleOpcode(byte opcode) {
        int b = opcode & 0xFF;
        switch (b) {
            case 0x27: return "daa";
            case 0x2F: return "das";
            case 0x37: return "aaa";
            case 0x3F: return "aas";
            case 0xD4: { byte imm = fetch8(); return "aam " + hexByte(imm); }
            case 0xD5: { byte imm = fetch8(); return "aad " + hexByte(imm); }

            case 0xE4: { byte imm = fetch8(); return "in al, " + hexByte(imm); }
            case 0xE5: { byte imm = fetch8(); return "in ax, " + hexByte(imm); }
            case 0xE6: { byte imm = fetch8(); return "out " + hexByte(imm) + ", al"; }
            case 0xE7: { byte imm = fetch8(); return "out " + hexByte(imm) + ", ax"; }
            case 0xEC: return "in al, dx";
            case 0xED: return "in ax, dx";
            case 0xEE: return "out dx, al";
            case 0xEF: return "out dx, ax";

            case 0x88: return mov_r_m8();
            case 0x89: return mov_r_m16();
            case 0x8A: return mov_r8_r_m();
            case 0x8B: return mov_r16_r_m();
            case 0x8C: return mov_r_m_sreg();
            case 0x8E: return mov_sreg_r_m();
            case 0xA0: return mov_al_moffs();
            case 0xA1: return mov_ax_moffs();
            case 0xA2: return mov_moffs_al();
            case 0xA3: return mov_moffs_ax();
            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
                return mov_imm8(b);
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                return mov_imm16(b);
            case 0xC6: return mov_r_m8_imm8();
            case 0xC7: return mov_r_m16_imm16();

            case 0x00: return add_r_m8();
            case 0x01: return add_r_m16();
            case 0x02: return add_r8_r_m();
            case 0x03: return add_r16_r_m();
            case 0x04: return add_al_imm8();
            case 0x05: return add_ax_imm16();

            case 0x10: return adc_r_m8();
            case 0x11: return adc_r_m16();
            case 0x12: return adc_r8_r_m();
            case 0x13: return adc_r16_r_m();
            case 0x14: return adc_al_imm8();
            case 0x15: return adc_ax_imm16();

            case 0x28: return sub_r_m8();
            case 0x29: return sub_r_m16();
            case 0x2A: return sub_r8_r_m();
            case 0x2B: return sub_r16_r_m();
            case 0x2C: return sub_al_imm8();
            case 0x2D: return sub_ax_imm16();

            case 0x18: return sbb_r_m8();
            case 0x19: return sbb_r_m16();
            case 0x1A: return sbb_r8_r_m();
            case 0x1B: return sbb_r16_r_m();
            case 0x1C: return sbb_al_imm8();
            case 0x1D: return sbb_ax_imm16();

            case 0x38: return cmp_r_m8();
            case 0x39: return cmp_r_m16();
            case 0x3A: return cmp_r8_r_m();
            case 0x3B: return cmp_r16_r_m();
            case 0x3C: return cmp_al_imm8();
            case 0x3D: return cmp_ax_imm16();

            case 0x20: return and_r_m8();
            case 0x21: return and_r_m16();
            case 0x22: return and_r8_r_m();
            case 0x23: return and_r16_r_m();
            case 0x24: return and_al_imm8();
            case 0x25: return and_ax_imm16();

            case 0x08: return or_r_m8();
            case 0x09: return or_r_m16();
            case 0x0A: return or_r8_r_m();
            case 0x0B: return or_r16_r_m();
            case 0x0C: return or_al_imm8();
            case 0x0D: return or_ax_imm16();

            case 0x30: return xor_r_m8();
            case 0x31: return xor_r_m16();
            case 0x32: return xor_r8_r_m();
            case 0x33: return xor_r16_r_m();
            case 0x34: return xor_al_imm8();
            case 0x35: return xor_ax_imm16();

            case 0x84: return test_r_m8();
            case 0x85: return test_r_m16();
            case 0xA8: return test_al_imm8();
            case 0xA9: return test_ax_imm16();

            case 0x86: return xchg_r_m8();
            case 0x87: return xchg_r_m16();
            case 0x90: case 0x91: case 0x92: case 0x93:
            case 0x94: case 0x95: case 0x96: case 0x97:
                return xchg_ax_reg(b);

            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
                return inc_reg16(b);
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
                return dec_reg16(b);
            case 0xFE:
                return inc_dec_r_m8();

            case 0x06: return "push es";
            case 0x07: return "pop es";
            case 0x0E: return "push cs";
            case 0x16: return "push ss";
            case 0x17: return "pop ss";
            case 0x1E: return "push ds";
            case 0x1F: return "pop ds";
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
                return push_reg16(b);
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
                return pop_reg16(b);
            case 0x8F: return pop_r_m16();

            case 0x70: return jcc_rel8("jo");
            case 0x71: return jcc_rel8("jno");
            case 0x72: return jcc_rel8("jb");
            case 0x73: return jcc_rel8("jnb");
            case 0x74: return jcc_rel8("je");
            case 0x75: return jcc_rel8("jne");
            case 0x76: return jcc_rel8("jbe");
            case 0x77: return jcc_rel8("jnbe");
            case 0x78: return jcc_rel8("js");
            case 0x79: return jcc_rel8("jns");
            case 0x7A: return jcc_rel8("jp");
            case 0x7B: return jcc_rel8("jnp");
            case 0x7C: return jcc_rel8("jl");
            case 0x7D: return jcc_rel8("jnl");
            case 0x7E: return jcc_rel8("jle");
            case 0x7F: return jcc_rel8("jnle");
            case 0xE3: return jcxz_rel8();

            case 0xE0: return loopnz();
            case 0xE1: return loopz();
            case 0xE2: return loop();

            case 0xE8: return call_rel16();
            case 0xE9: return jmp_rel16();
            case 0xEA: return jmp_ptr16_16();
            case 0xEB: return jmp_rel8();
            case 0x9A: return call_ptr16_16();

            case 0xC2: return ret_imm16();
            case 0xC3: return "ret";
            case 0xCA: return retf_imm16();
            case 0xCB: return "retf";

            case 0xCC: return "int3";
            case 0xCD: return int_imm8();
            case 0xCE: return "into";
            case 0xCF: return "iret";

            case 0x9C: return "pushf";
            case 0x9D: return "popf";
            case 0x9E: return "sahf";
            case 0x9F: return "lahf";
            case 0xF5: return "cmc";
            case 0xF8: return "clc";
            case 0xF9: return "stc";
            case 0xFA: return "cli";
            case 0xFB: return "sti";
            case 0xFC: return "cld";
            case 0xFD: return "std";

            case 0x8D: return lea();
            case 0xC4: return les();
            case 0xC5: return lds();

            case 0xD7: return "xlat";

            case 0x98: return "cbw";
            case 0x99: return "cwd";

            case 0xF4: return "hlt";
            case 0x9B: return "wait";

            case 0x80: return group1_imm8();
            case 0x81: return group1_imm16();
            case 0x83: return group1_imm8_sign();

            case 0xD0: return shift_rotate8_1();
            case 0xD1: return shift_rotate16_1();
            case 0xD2: return shift_rotate8_cl();
            case 0xD3: return shift_rotate16_cl();

            case 0xF6: return group3A();
            case 0xF7: return group3B();

            case (byte)0xFF: return group5();

            case 0xA4: return "movsb";
            case 0xA5: return "movsw";
            case 0xA6: return "cmpsb";
            case 0xA7: return "cmpsw";
            case 0xAA: return "stosb";
            case 0xAB: return "stosw";
            case 0xAC: return "lodsb";
            case 0xAD: return "lodsw";
            case 0xAE: return "scasb";
            case 0xAF: return "scasw";

            case 0xD8: case 0xD9: case 0xDA: case 0xDB:
            case 0xDC: case 0xDD: case 0xDE: case 0xDF:
                return disassemble8087(b);

            default:
                return "db " + hexByte(b);
        }
    }

    private byte fetch8() {
        byte v = cpu.getMemory().fetchByte(addr);
        addr.increment();
        return v;
    }

    private short fetch16() {
        short v = cpu.getMemory().fetchWord(addr);
        addr.addOffset((short) 2);
        return v;
    }

    private int getModRM() {
        return fetch8() & 0xFF;
    }

    private String reg8Name(int reg) {
        String[] names = {"al", "cl", "dl", "bl", "ah", "ch", "dh", "bh"};
        return names[reg];
    }

    private String reg16Name(int reg) {
        String[] names = {"ax", "cx", "dx", "bx", "sp", "bp", "si", "di"};
        return names[reg];
    }

    private String segRegName(int reg) {
        String[] names = {"es", "cs", "ss", "ds"};
        return names[reg];
    }

    private String modRM8Operand(int modrm) {
        int mod = (modrm >> 6) & 3;
        int rm = modrm & 7;
        if (mod == 3) {
            return reg8Name(rm);
        } else {
            return memoryOperand(mod, rm);
        }
    }

    private String modRM16Operand(int modrm) {
        int mod = (modrm >> 6) & 3;
        int rm = modrm & 7;
        if (mod == 3) {
            return reg16Name(rm);
        } else {
            return memoryOperand(mod, rm);
        }
    }

    private String memoryOperand(int mod, int rm) {
        StringBuilder sb = new StringBuilder("[");
        String base = "";
        String index = "";
        int disp = 0;
        if (rm == 0) { base = "bx"; index = "si"; }
        else if (rm == 1) { base = "bx"; index = "di"; }
        else if (rm == 2) { base = "bp"; index = "si"; }
        else if (rm == 3) { base = "bp"; index = "di"; }
        else if (rm == 4) { base = "si"; }
        else if (rm == 5) { base = "di"; }
        else if (rm == 6) {
            if (mod == 0) {
                disp = fetch16() & 0xFFFF;
                sb.append(hexWord(disp));
                sb.append("]");
                return sb.toString();
            } else {
                base = "bp";
            }
        } else if (rm == 7) { base = "bx"; }

        if (!base.isEmpty()) sb.append(base);
        if (!index.isEmpty()) {
            if (sb.length() > 1) sb.append("+");
            sb.append(index);
        }
        if (mod == 1) {
            disp = fetch8();
            if (disp != 0) {
                if (disp > 0) sb.append("+");
                else sb.append("-");
                sb.append(hexByte(Math.abs(disp)));
            }
        } else if (mod == 2) {
            disp = fetch16();
            if (disp != 0) {
                if ((short) disp > 0) sb.append("+");
                else sb.append("-");
                sb.append(hexWord(Math.abs(disp)));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String mov_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "mov " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String mov_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "mov " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String mov_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "mov " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String mov_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "mov " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String mov_r_m_sreg() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "mov " + modRM16Operand(modrm) + ", " + segRegName(reg); }
    private String mov_sreg_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "mov " + segRegName(reg) + ", " + modRM16Operand(modrm); }
    private String mov_al_moffs() { short offset = fetch16(); return "mov al, [" + hexWord(offset) + "]"; }
    private String mov_ax_moffs() { short offset = fetch16(); return "mov ax, [" + hexWord(offset) + "]"; }
    private String mov_moffs_al() { short offset = fetch16(); return "mov [" + hexWord(offset) + "], al"; }
    private String mov_moffs_ax() { short offset = fetch16(); return "mov [" + hexWord(offset) + "], ax"; }
    private String mov_imm8(int op) { int reg = op & 0x7; byte imm = fetch8(); return "mov " + reg8Name(reg) + ", " + hexByte(imm); }
    private String mov_imm16(int op) { int reg = op & 0x7; short imm = fetch16(); return "mov " + reg16Name(reg) + ", " + hexWord(imm); }
    private String mov_r_m8_imm8() { int modrm = getModRM(); byte imm = fetch8(); return "mov " + modRM8Operand(modrm) + ", " + hexByte(imm); }
    private String mov_r_m16_imm16() { int modrm = getModRM(); short imm = fetch16(); return "mov " + modRM16Operand(modrm) + ", " + hexWord(imm); }

    private String add_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String add_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String add_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String add_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String add_al_imm8() { byte imm = fetch8(); return "add al, " + hexByte(imm); }
    private String add_ax_imm16() { short imm = fetch16(); return "add ax, " + hexWord(imm); }

    private String adc_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String adc_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String adc_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String adc_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String adc_al_imm8() { byte imm = fetch8(); return "adc al, " + hexByte(imm); }
    private String adc_ax_imm16() { short imm = fetch16(); return "adc ax, " + hexWord(imm); }

    private String sub_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String sub_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String sub_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String sub_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String sub_al_imm8() { byte imm = fetch8(); return "sub al, " + hexByte(imm); }
    private String sub_ax_imm16() { short imm = fetch16(); return "sub ax, " + hexWord(imm); }

    private String sbb_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String sbb_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String sbb_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String sbb_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String sbb_al_imm8() { byte imm = fetch8(); return "sbb al, " + hexByte(imm); }
    private String sbb_ax_imm16() { short imm = fetch16(); return "sbb ax, " + hexWord(imm); }

    private String cmp_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String cmp_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String cmp_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String cmp_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String cmp_al_imm8() { byte imm = fetch8(); return "cmp al, " + hexByte(imm); }
    private String cmp_ax_imm16() { short imm = fetch16(); return "cmp ax, " + hexWord(imm); }

    private String and_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String and_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String and_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String and_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String and_al_imm8() { byte imm = fetch8(); return "and al, " + hexByte(imm); }
    private String and_ax_imm16() { short imm = fetch16(); return "and ax, " + hexWord(imm); }

    private String or_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String or_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String or_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String or_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String or_al_imm8() { byte imm = fetch8(); return "or al, " + hexByte(imm); }
    private String or_ax_imm16() { short imm = fetch16(); return "or ax, " + hexWord(imm); }

    private String xor_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String xor_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String xor_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String xor_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String xor_al_imm8() { byte imm = fetch8(); return "xor al, " + hexByte(imm); }
    private String xor_ax_imm16() { short imm = fetch16(); return "xor ax, " + hexWord(imm); }

    private String test_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "test " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String test_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "test " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String test_al_imm8() { byte imm = fetch8(); return "test al, " + hexByte(imm); }
    private String test_ax_imm16() { short imm = fetch16(); return "test ax, " + hexWord(imm); }

    private String xchg_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xchg " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String xchg_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xchg " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String xchg_ax_reg(int op) { int reg = op & 0x7; return reg == 0 ? "nop" : "xchg ax, " + reg16Name(reg); }

    private String inc_reg16(int op) { int reg = op & 0x7; return "inc " + reg16Name(reg); }
    private String dec_reg16(int op) { int reg = op & 0x7; return "dec " + reg16Name(reg); }
    private String inc_dec_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; if (reg == 0) return "inc " + modRM8Operand(modrm); if (reg == 1) return "dec " + modRM8Operand(modrm); return "db 0xFE"; }

    private String push_reg16(int op) { int reg = op & 0x7; return "push " + reg16Name(reg); }
    private String pop_reg16(int op) { int reg = op & 0x7; return "pop " + reg16Name(reg); }
    private String pop_r_m16() { int modrm = getModRM(); return "pop " + modRM16Operand(modrm); }

    private String jcc_rel8(String mnem) { byte disp = fetch8(); int target = startOffset + 2 + disp; return mnem + " " + hexWord(target); }
    private String jcxz_rel8() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "jcxz " + hexWord(target); }
    private String loopnz() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "loopnz " + hexWord(target); }
    private String loopz() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "loopz " + hexWord(target); }
    private String loop() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "loop " + hexWord(target); }
    private String call_rel16() { short disp = fetch16(); int target = startOffset + 3 + disp; return "call " + hexWord(target); }
    private String jmp_rel16() { short disp = fetch16(); int target = startOffset + 3 + disp; return "jmp " + hexWord(target); }
    private String jmp_ptr16_16() { short offset = fetch16(); short segment = fetch16(); return "jmp " + hexWord(segment) + ":" + hexWord(offset); }
    private String jmp_rel8() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "jmp " + hexWord(target); }
    private String call_ptr16_16() { short offset = fetch16(); short segment = fetch16(); return "call " + hexWord(segment) + ":" + hexWord(offset); }

    private String ret_imm16() { short imm = fetch16(); return "ret " + hexWord(imm); }
    private String retf_imm16() { short imm = fetch16(); return "retf " + hexWord(imm); }
    private String int_imm8() { byte imm = fetch8(); return "int " + hexByte(imm); }

    private String lea() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "lea " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String les() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "les " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String lds() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "lds " + reg16Name(reg) + ", " + modRM16Operand(modrm); }

    private String group1_imm8() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        byte imm = fetch8();
        String op = switch (reg) {
            case 0 -> "add"; case 1 -> "or"; case 2 -> "adc"; case 3 -> "sbb";
            case 4 -> "and"; case 5 -> "sub"; case 6 -> "xor"; case 7 -> "cmp";
            default -> "db";
        };
        return op + " " + modRM8Operand(modrm) + ", " + hexByte(imm);
    }

    private String group1_imm16() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        short imm = fetch16();
        String op = switch (reg) {
            case 0 -> "add"; case 1 -> "or"; case 2 -> "adc"; case 3 -> "sbb";
            case 4 -> "and"; case 5 -> "sub"; case 6 -> "xor"; case 7 -> "cmp";
            default -> "db";
        };
        return op + " " + modRM16Operand(modrm) + ", " + hexWord(imm);
    }

    private String group1_imm8_sign() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        byte imm = fetch8();
        String op = switch (reg) {
            case 0 -> "add"; case 1 -> "or"; case 2 -> "adc"; case 3 -> "sbb";
            case 4 -> "and"; case 5 -> "sub"; case 6 -> "xor"; case 7 -> "cmp";
            default -> "db";
        };
        return op + " " + modRM16Operand(modrm) + ", " + hexByte(imm);
    }

    private String shift_rotate8_1() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        String op = switch (reg) {
            case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr";
            case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar";
            default -> "db";
        };
        return op + " " + modRM8Operand(modrm) + ", 1";
    }

    private String shift_rotate16_1() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        String op = switch (reg) {
            case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr";
            case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar";
            default -> "db";
        };
        return op + " " + modRM16Operand(modrm) + ", 1";
    }

    private String shift_rotate8_cl() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        String op = switch (reg) {
            case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr";
            case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar";
            default -> "db";
        };
        return op + " " + modRM8Operand(modrm) + ", cl";
    }

    private String shift_rotate16_cl() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        String op = switch (reg) {
            case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr";
            case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar";
            default -> "db";
        };
        return op + " " + modRM16Operand(modrm) + ", cl";
    }

    private String group3A() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        if (reg == 0) {
            byte imm = fetch8();
            return "test " + modRM8Operand(modrm) + ", " + hexByte(imm);
        }
        return switch (reg) {
            case 2 -> "not " + modRM8Operand(modrm);
            case 3 -> "neg " + modRM8Operand(modrm);
            case 4 -> "mul " + modRM8Operand(modrm);
            case 5 -> "imul " + modRM8Operand(modrm);
            case 6 -> "div " + modRM8Operand(modrm);
            case 7 -> "idiv " + modRM8Operand(modrm);
            default -> "db 0xF6";
        };
    }

    private String group3B() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        if (reg == 0) {
            short imm = fetch16();
            return "test " + modRM16Operand(modrm) + ", " + hexWord(imm);
        }
        return switch (reg) {
            case 2 -> "not " + modRM16Operand(modrm);
            case 3 -> "neg " + modRM16Operand(modrm);
            case 4 -> "mul " + modRM16Operand(modrm);
            case 5 -> "imul " + modRM16Operand(modrm);
            case 6 -> "div " + modRM16Operand(modrm);
            case 7 -> "idiv " + modRM16Operand(modrm);
            default -> "db 0xF7";
        };
    }

    private String group5() {
        int modrm = getModRM();
        int reg = (modrm >> 3) & 7;
        int mod = (modrm >> 6) & 3;
        if ((reg == 2 || reg == 3 || reg == 4 || reg == 5) && mod == 3) {
            return "db 0xFF";
        }
        return switch (reg) {
            case 0 -> "inc " + modRM16Operand(modrm);
            case 1 -> "dec " + modRM16Operand(modrm);
            case 2 -> "call " + modRM16Operand(modrm);
            case 3 -> "call far " + modRM16Operand(modrm);
            case 4 -> "jmp " + modRM16Operand(modrm);
            case 5 -> "jmp far " + modRM16Operand(modrm);
            case 6 -> "push " + modRM16Operand(modrm);
            default -> "db 0xFF";
        };
    }

    private String disassemble8087(int esc) {
        int modrm = getModRM();
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;

        if (mod == 3) {
            switch (esc) {
                case 0xD8:
                    switch (reg) {
                        case 0: return "fadd st, st(" + rm + ")";
                        case 1: return "fmul st, st(" + rm + ")";
                        case 2: return "fcom st(" + rm + ")";
                        case 3: return "fcomp st(" + rm + ")";
                        case 4: return "fsub st, st(" + rm + ")";
                        case 5: return "fsubr st, st(" + rm + ")";
                        case 6: return "fdiv st, st(" + rm + ")";
                        case 7: return "fdivr st, st(" + rm + ")";
                    }
                    break;
                case 0xD9:
                    if (reg == 0) {
                        if (rm == 0) return "fld st(" + rm + ")";
                        if (rm == 1) return "fxch st(" + rm + ")";
                        if (rm == 2) return "fnop";
                    } else if (reg == 1) {
                        if (rm == 0) return "fchs";
                        if (rm == 1) return "fabs";
                    } else if (reg == 2) return "fst st(" + rm + ")";
                    else if (reg == 3) return "fstp st(" + rm + ")";
                    else if (reg == 4) {
                        if (rm == 0) return "fldenv";
                        if (rm == 1) return "fldcw";
                    } else if (reg == 5) {
                        if (rm == 0) return "fstenv";
                        if (rm == 1) return "fstcw";
                    } else if (reg == 6) {
                        if (rm == 0) return "fclex";
                        if (rm == 1) return "fninit";
                    } else if (reg == 7 && rm == 0) return "fstsw ax";
                    break;
                case 0xDA:
                    if (reg == 0) return "fiadd st, st(" + rm + ")";
                    if (reg == 1) return "fimul st, st(" + rm + ")";
                    if (reg == 2) return "ficom st(" + rm + ")";
                    if (reg == 3) return "ficomp st(" + rm + ")";
                    if (reg == 4) return "fisub st, st(" + rm + ")";
                    if (reg == 5) return "fisubr st, st(" + rm + ")";
                    if (reg == 6) return "fidiv st, st(" + rm + ")";
                    if (reg == 7) return "fidivr st, st(" + rm + ")";
                    break;
                case 0xDB:
                    if (reg == 0) return "fild st(" + rm + ")";
                    if (reg == 1) return "fist st(" + rm + ")";
                    if (reg == 2) return "fistp st(" + rm + ")";
                    if (reg == 5) return "fld st(" + rm + ")";
                    if (reg == 7) return "fstp st(" + rm + ")";
                    break;
                case 0xDC:
                    switch (reg) {
                        case 0: return "fadd st, st(" + rm + ")";
                        case 1: return "fmul st, st(" + rm + ")";
                        case 2: return "fcom st(" + rm + ")";
                        case 3: return "fcomp st(" + rm + ")";
                        case 4: return "fsub st, st(" + rm + ")";
                        case 5: return "fsubr st, st(" + rm + ")";
                        case 6: return "fdiv st, st(" + rm + ")";
                        case 7: return "fdivr st, st(" + rm + ")";
                    }
                    break;
                case 0xDD:
                    if (reg == 0) return "fld st(" + rm + ")";
                    if (reg == 1) return "fxch st(" + rm + ")";
                    if (reg == 2) return "fst st(" + rm + ")";
                    if (reg == 3) return "fstp st(" + rm + ")";
                    if (reg == 4) return "frstor";
                    if (reg == 5) return "fsave";
                    if (reg == 6) return "fstsw";
                    if (reg == 7) return "ffree st(" + rm + ")";
                    break;
                case 0xDE:
                    if (reg == 0) return "fiadd st, st(" + rm + ")";
                    if (reg == 1) return "fimul st, st(" + rm + ")";
                    if (reg == 2) return "ficom st(" + rm + ")";
                    if (reg == 3) return "ficomp st(" + rm + ")";
                    if (reg == 4) return "fisub st, st(" + rm + ")";
                    if (reg == 5) return "fisubr st, st(" + rm + ")";
                    if (reg == 6) return "fidiv st, st(" + rm + ")";
                    if (reg == 7) return "fidivr st, st(" + rm + ")";
                    break;
                case 0xDF:
                    if (reg == 0) return "fild st(" + rm + ")";
                    if (reg == 1) return "fist st(" + rm + ")";
                    if (reg == 2) return "fistp st(" + rm + ")";
                    if (reg == 3) return "fbld st(" + rm + ")";
                    if (reg == 4) return "fild st(" + rm + ")";
                    if (reg == 5) return "fistp st(" + rm + ")";
                    if (reg == 6) return "fbstp st(" + rm + ")";
                    if (reg == 7) return "fistp st(" + rm + ")";
                    break;
            }
            return "db " + hexByte(esc);
        }

        String mem = memoryOperand(mod, rm);
        switch (esc) {
            case 0xD8:
                switch (reg) {
                    case 0: return "fadd dword ptr " + mem;
                    case 1: return "fmul dword ptr " + mem;
                    case 2: return "fcom dword ptr " + mem;
                    case 3: return "fcomp dword ptr " + mem;
                    case 4: return "fsub dword ptr " + mem;
                    case 5: return "fsubr dword ptr " + mem;
                    case 6: return "fdiv dword ptr " + mem;
                    case 7: return "fdivr dword ptr " + mem;
                }
                break;
            case 0xD9:
                switch (reg) {
                    case 0: return "fld dword ptr " + mem;
                    case 2: return "fst dword ptr " + mem;
                    case 3: return "fstp dword ptr " + mem;
                    case 4: return "fldenv " + mem;
                    case 5: return "fldcw word ptr " + mem;
                    case 6: return "fstenv " + mem;
                    case 7: return "fstcw word ptr " + mem;
                }
                break;
            case 0xDA:
                switch (reg) {
                    case 0: return "fiadd dword ptr " + mem;
                    case 1: return "fimul dword ptr " + mem;
                    case 2: return "ficom dword ptr " + mem;
                    case 3: return "ficomp dword ptr " + mem;
                    case 4: return "fisub dword ptr " + mem;
                    case 5: return "fisubr dword ptr " + mem;
                    case 6: return "fidiv dword ptr " + mem;
                    case 7: return "fidivr dword ptr " + mem;
                }
                break;
            case 0xDB:
                switch (reg) {
                    case 0: return "fild dword ptr " + mem;
                    case 1: return "fist dword ptr " + mem;
                    case 2: return "fistp dword ptr " + mem;
                    case 3: return "fld tbyte ptr " + mem;
                    case 5: return "fld tbyte ptr " + mem;
                    case 7: return "fstp tbyte ptr " + mem;
                }
                break;
            case 0xDC:
                switch (reg) {
                    case 0: return "fadd qword ptr " + mem;
                    case 1: return "fmul qword ptr " + mem;
                    case 2: return "fcom qword ptr " + mem;
                    case 3: return "fcomp qword ptr " + mem;
                    case 4: return "fsub qword ptr " + mem;
                    case 5: return "fsubr qword ptr " + mem;
                    case 6: return "fdiv qword ptr " + mem;
                    case 7: return "fdivr qword ptr " + mem;
                }
                break;
            case 0xDD:
                switch (reg) {
                    case 0: return "fld qword ptr " + mem;
                    case 1: return "fistp qword ptr " + mem;
                    case 2: return "fst qword ptr " + mem;
                    case 3: return "fstp qword ptr " + mem;
                    case 4: return "frstor " + mem;
                    case 5: return "fsave " + mem;
                    case 6: return "fstsw word ptr " + mem;
                    case 7: return "ffree qword ptr " + mem;
                }
                break;
            case 0xDE:
                switch (reg) {
                    case 0: return "fiadd word ptr " + mem;
                    case 1: return "fimul word ptr " + mem;
                    case 2: return "ficom word ptr " + mem;
                    case 3: return "ficomp word ptr " + mem;
                    case 4: return "fisub word ptr " + mem;
                    case 5: return "fisubr word ptr " + mem;
                    case 6: return "fidiv word ptr " + mem;
                    case 7: return "fidivr word ptr " + mem;
                }
                break;
            case 0xDF:
                switch (reg) {
                    case 0: return "fild word ptr " + mem;
                    case 1: return "fist word ptr " + mem;
                    case 2: return "fistp word ptr " + mem;
                    case 3: return "fbld tbyte ptr " + mem;
                    case 4: return "fild dword ptr " + mem;
                    case 5: return "fistp dword ptr " + mem;
                    case 6: return "fbstp tbyte ptr " + mem;
                    case 7: return "fistp qword ptr " + mem;
                }
                break;
        }
        return "db " + hexByte(esc);
    }
}
