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
        byte opcode = fetch8();
        if (opcode == 0x26 || opcode == 0x2E || opcode == 0x36 || opcode == 0x3E) {
            return disassembleWithPrefix(opcode);
        } else if (opcode == (byte) 0xF2 || opcode == (byte) 0xF3) {
            return disassembleWithPrefix(opcode);
        }
        return disassembleOpcode(opcode);
    }

    private String disassembleWithPrefix(byte prefix) {
        String prefixStr = "";
        switch (prefix) {
            case 0x26: prefixStr = "es:"; break;
            case 0x2E: prefixStr = "cs:"; break;
            case 0x36: prefixStr = "ss:"; break;
            case 0x3E: prefixStr = "ds:"; break;
            case (byte)0xF2: prefixStr = "repnz "; break;
            case (byte)0xF3: prefixStr = "repz "; break;
        }
        byte opcode = fetch8();
        String inner = disassembleOpcode(opcode);
        if (inner.startsWith(prefixStr)) {
            return inner;
        }
        return prefixStr + inner;
    }

    private String disassembleOpcode(byte opcode) {
        int b = opcode & 0xFF;
        switch (b) {
            // MOV
            case 0x88: return mov_r_m8();    // mov r/m8, r8
            case 0x89: return mov_r_m16();   // mov r/m16, r16
            case 0x8A: return mov_r8_r_m();  // mov r8, r/m8
            case 0x8B: return mov_r16_r_m(); // mov r16, r/m16
            case 0x8C: return mov_r_m_sreg();// mov r/m16, sreg
            case 0x8E: return mov_sreg_r_m();// mov sreg, r/m16
            case 0xA0: return mov_al_moffs();// mov al, [seg:offset]
            case 0xA1: return mov_ax_moffs();// mov ax, [seg:offset]
            case 0xA2: return mov_moffs_al();// mov [seg:offset], al
            case 0xA3: return mov_moffs_ax();// mov [seg:offset], ax
            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
                return mov_imm8(b);
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                return mov_imm16(b);
            case 0xC6: return mov_r_m8_imm8(); // mov r/m8, imm8
            case 0xC7: return mov_r_m16_imm16();// mov r/m16, imm16
            // ADD
            case 0x00: return add_r_m8();      // add r/m8, r8
            case 0x01: return add_r_m16();     // add r/m16, r16
            case 0x02: return add_r8_r_m();    // add r8, r/m8
            case 0x03: return add_r16_r_m();   // add r16, r/m16
            case 0x04: return add_al_imm8();   // add al, imm8
            case 0x05: return add_ax_imm16();  // add ax, imm16
            // ADC
            case 0x10: return adc_r_m8();
            case 0x11: return adc_r_m16();
            case 0x12: return adc_r8_r_m();
            case 0x13: return adc_r16_r_m();
            case 0x14: return adc_al_imm8();
            case 0x15: return adc_ax_imm16();
            // SUB
            case 0x28: return sub_r_m8();
            case 0x29: return sub_r_m16();
            case 0x2A: return sub_r8_r_m();
            case 0x2B: return sub_r16_r_m();
            case 0x2C: return sub_al_imm8();
            case 0x2D: return sub_ax_imm16();
            // SBB
            case 0x18: return sbb_r_m8();
            case 0x19: return sbb_r_m16();
            case 0x1A: return sbb_r8_r_m();
            case 0x1B: return sbb_r16_r_m();
            case 0x1C: return sbb_al_imm8();
            case 0x1D: return sbb_ax_imm16();
            // CMP
            case 0x38: return cmp_r_m8();
            case 0x39: return cmp_r_m16();
            case 0x3A: return cmp_r8_r_m();
            case 0x3B: return cmp_r16_r_m();
            case 0x3C: return cmp_al_imm8();
            case 0x3D: return cmp_ax_imm16();
            // AND
            case 0x20: return and_r_m8();
            case 0x21: return and_r_m16();
            case 0x22: return and_r8_r_m();
            case 0x23: return and_r16_r_m();
            case 0x24: return and_al_imm8();
            case 0x25: return and_ax_imm16();
            // OR
            case 0x08: return or_r_m8();
            case 0x09: return or_r_m16();
            case 0x0A: return or_r8_r_m();
            case 0x0B: return or_r16_r_m();
            case 0x0C: return or_al_imm8();
            case 0x0D: return or_ax_imm16();
            // XOR
            case 0x30: return xor_r_m8();
            case 0x31: return xor_r_m16();
            case 0x32: return xor_r8_r_m();
            case 0x33: return xor_r16_r_m();
            case 0x34: return xor_al_imm8();
            case 0x35: return xor_ax_imm16();
            // TEST
            case 0x84: return test_r_m8();
            case 0x85: return test_r_m16();
            case 0xA8: return test_al_imm8();
            case 0xA9: return test_ax_imm16();
            // XCHG
            case 0x86: return xchg_r_m8();
            case 0x87: return xchg_r_m16();
            case 0x90: case 0x91: case 0x92: case 0x93:
            case 0x94: case 0x95: case 0x96: case 0x97:
                return xchg_ax_reg(b);
            // INC/DEC
            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
                return inc_reg16(b);
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
                return dec_reg16(b);
            case 0xFE:
                return inc_dec_r_m8();
            // PUSH/POP
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
            // Jcc
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
            // LOOP
            case 0xE0: return loopnz();
            case 0xE1: return loopz();
            case 0xE2: return loop();
            // JMP/CALL
            case 0xE8: return call_rel16();
            case 0xE9: return jmp_rel16();
            case 0xEA: return jmp_ptr16_16();
            case 0xEB: return jmp_rel8();
            case 0x9A: return call_ptr16_16();
            // RET
            case 0xC2: return ret_imm16();
            case 0xC3: return "ret";
            case 0xCA: return retf_imm16();
            case 0xCB: return "retf";
            // INT
            case 0xCC: return "int3";
            case 0xCD: return int_imm8();
            case 0xCE: return "into";
            case 0xCF: return "iret";
            // FLAGS
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
            // LEA, LDS, LES
            case 0x8D: return lea();
            case 0xC4: return les();
            case 0xC5: return lds();
            // XLAT
            case 0xD7: return "xlat";
            // CBW/CWD
            case 0x98: return "cbw";
            case 0x99: return "cwd";
            // HLT, WAIT, LOCK
            case 0xF4: return "hlt";
            case 0x9B: return "wait";
            case 0xF0: return "lock";
            // Group 1 (imm8/imm16)
            case 0x80: return group1_imm8();
            case 0x81: return group1_imm16();
            case 0x83: return group1_imm8_sign();
            // Group 2 (shift/rotate)
            case 0xD0: return shift_rotate8_1();
            case 0xD1: return shift_rotate16_1();
            case 0xD2: return shift_rotate8_cl();
            case 0xD3: return shift_rotate16_cl();
            // Group 3A (test/not/neg/mul/imul/div/idiv)
            case 0xF6: return group3A();
            case 0xF7: return group3B();
            // Group 5 (inc/dec/call/jmp/push r/m16)
            case (byte)0xFF: return group5();
            // String instructions
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
            // NOP handled in xchg_ax_reg (case 0x90) as "nop"
            default:
                return "db 0x" + Integer.toHexString(b);
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
                // disp16 only
                disp = fetch16() & 0xFFFF;
                sb.append("0x").append(Integer.toHexString(disp));
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
                sb.append("0x").append(Integer.toHexString(disp & 0xFF));
            }
        } else if (mod == 2) {
            disp = fetch16();
            if (disp != 0) {
                if ((short) disp > 0) sb.append("+");
                sb.append("0x").append(Integer.toHexString(disp & 0xFFFF));
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
    private String mov_al_moffs() { short offset = fetch16(); return "mov al, [0x" + Integer.toHexString(offset & 0xFFFF) + "]"; }
    private String mov_ax_moffs() { short offset = fetch16(); return "mov ax, [0x" + Integer.toHexString(offset & 0xFFFF) + "]"; }
    private String mov_moffs_al() { short offset = fetch16(); return "mov [0x" + Integer.toHexString(offset & 0xFFFF) + "], al"; }
    private String mov_moffs_ax() { short offset = fetch16(); return "mov [0x" + Integer.toHexString(offset & 0xFFFF) + "], ax"; }
    private String mov_imm8(int op) { int reg = op & 0x7; byte imm = fetch8(); return "mov " + reg8Name(reg) + ", 0x" + Integer.toHexString(imm & 0xFF); }
    private String mov_imm16(int op) { int reg = op & 0x7; short imm = fetch16(); return "mov " + reg16Name(reg) + ", 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String mov_r_m8_imm8() { int modrm = getModRM(); byte imm = fetch8(); return "mov " + modRM8Operand(modrm) + ", 0x" + Integer.toHexString(imm & 0xFF); }
    private String mov_r_m16_imm16() { int modrm = getModRM(); short imm = fetch16(); return "mov " + modRM16Operand(modrm) + ", 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String add_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String add_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String add_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String add_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "add " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String add_al_imm8() { byte imm = fetch8(); return "add al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String add_ax_imm16() { short imm = fetch16(); return "add ax, 0x" + Integer.toHexString(imm & 0xFFFF); }

    private String adc_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String adc_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String adc_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String adc_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "adc " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String adc_al_imm8() { byte imm = fetch8(); return "adc al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String adc_ax_imm16() { short imm = fetch16(); return "adc ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String sub_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String sub_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String sub_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String sub_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sub " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String sub_al_imm8() { byte imm = fetch8(); return "sub al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String sub_ax_imm16() { short imm = fetch16(); return "sub ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String sbb_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String sbb_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String sbb_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String sbb_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "sbb " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String sbb_al_imm8() { byte imm = fetch8(); return "sbb al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String sbb_ax_imm16() { short imm = fetch16(); return "sbb ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String cmp_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String cmp_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String cmp_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String cmp_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "cmp " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String cmp_al_imm8() { byte imm = fetch8(); return "cmp al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String cmp_ax_imm16() { short imm = fetch16(); return "cmp ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String and_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String and_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String and_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String and_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "and " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String and_al_imm8() { byte imm = fetch8(); return "and al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String and_ax_imm16() { short imm = fetch16(); return "and ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String or_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String or_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String or_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String or_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "or " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String or_al_imm8() { byte imm = fetch8(); return "or al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String or_ax_imm16() { short imm = fetch16(); return "or ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String xor_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String xor_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String xor_r8_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String xor_r16_r_m() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xor " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String xor_al_imm8() { byte imm = fetch8(); return "xor al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String xor_ax_imm16() { short imm = fetch16(); return "xor ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String test_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "test " + modRM8Operand(modrm) + ", " + reg8Name(reg); }
    private String test_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "test " + modRM16Operand(modrm) + ", " + reg16Name(reg); }
    private String test_al_imm8() { byte imm = fetch8(); return "test al, 0x" + Integer.toHexString(imm & 0xFF); }
    private String test_ax_imm16() { short imm = fetch16(); return "test ax, 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String xchg_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xchg " + reg8Name(reg) + ", " + modRM8Operand(modrm); }
    private String xchg_r_m16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "xchg " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String xchg_ax_reg(int op) { int reg = op & 0x7; return reg == 0 ? "nop" : "xchg ax, " + reg16Name(reg); }
    private String inc_reg16(int op) { int reg = op & 0x7; return "inc " + reg16Name(reg); }
    private String dec_reg16(int op) { int reg = op & 0x7; return "dec " + reg16Name(reg); }
    private String inc_dec_r_m8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; if (reg == 0) return "inc " + modRM8Operand(modrm); if (reg == 1) return "dec " + modRM8Operand(modrm); return "db 0xfe"; }
    private String push_reg16(int op) { int reg = op & 0x7; return "push " + reg16Name(reg); }
    private String pop_reg16(int op) { int reg = op & 0x7; return "pop " + reg16Name(reg); }
    private String pop_r_m16() { int modrm = getModRM(); return "pop " + modRM16Operand(modrm); }
    private String jcc_rel8(String mnem) { byte disp = fetch8(); int target = startOffset + 2 + disp; return mnem + " 0x" + Integer.toHexString(target & 0xFFFF); }
    private String jcxz_rel8() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "jcxz 0x" + Integer.toHexString(target & 0xFFFF); }
    private String loopnz() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "loopnz 0x" + Integer.toHexString(target & 0xFFFF); }
    private String loopz() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "loopz 0x" + Integer.toHexString(target & 0xFFFF); }
    private String loop() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "loop 0x" + Integer.toHexString(target & 0xFFFF); }
    private String call_rel16() { short disp = fetch16(); int target = startOffset + 3 + disp; return "call 0x" + Integer.toHexString(target & 0xFFFF); }
    private String jmp_rel16() { short disp = fetch16(); int target = startOffset + 3 + disp; return "jmp 0x" + Integer.toHexString(target & 0xFFFF); }
    private String jmp_ptr16_16() { short offset = fetch16(); short segment = fetch16(); return "jmp 0x" + Integer.toHexString(segment & 0xFFFF) + ":0x" + Integer.toHexString(offset & 0xFFFF); }
    private String jmp_rel8() { byte disp = fetch8(); int target = startOffset + 2 + disp; return "jmp 0x" + Integer.toHexString(target & 0xFFFF); }
    private String call_ptr16_16() { short offset = fetch16(); short segment = fetch16(); return "call 0x" + Integer.toHexString(segment & 0xFFFF) + ":0x" + Integer.toHexString(offset & 0xFFFF); }
    private String ret_imm16() { short imm = fetch16(); return "ret 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String retf_imm16() { short imm = fetch16(); return "retf 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String int_imm8() { byte imm = fetch8(); return "int 0x" + Integer.toHexString(imm & 0xFF); }
    private String lea() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "lea " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String les() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "les " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String lds() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return "lds " + reg16Name(reg) + ", " + modRM16Operand(modrm); }
    private String group1_imm8() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; byte imm = fetch8(); String op = switch (reg) { case 0 -> "add"; case 1 -> "or"; case 2 -> "adc"; case 3 -> "sbb"; case 4 -> "and"; case 5 -> "sub"; case 6 -> "xor"; case 7 -> "cmp"; default -> "db"; }; return op + " " + modRM8Operand(modrm) + ", 0x" + Integer.toHexString(imm & 0xFF); }
    private String group1_imm16() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; short imm = fetch16(); String op = switch (reg) { case 0 -> "add"; case 1 -> "or"; case 2 -> "adc"; case 3 -> "sbb"; case 4 -> "and"; case 5 -> "sub"; case 6 -> "xor"; case 7 -> "cmp"; default -> "db"; }; return op + " " + modRM16Operand(modrm) + ", 0x" + Integer.toHexString(imm & 0xFFFF); }
    private String group1_imm8_sign() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; byte imm = fetch8(); String op = switch (reg) { case 0 -> "add"; case 1 -> "or"; case 2 -> "adc"; case 3 -> "sbb"; case 4 -> "and"; case 5 -> "sub"; case 6 -> "xor"; case 7 -> "cmp"; default -> "db"; }; return op + " " + modRM16Operand(modrm) + ", 0x" + Integer.toHexString(imm & 0xFF); }
    private String shift_rotate8_1() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; String op = switch (reg) { case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr"; case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar"; default -> "db"; }; return op + " " + modRM8Operand(modrm) + ", 1"; }
    private String shift_rotate16_1() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; String op = switch (reg) { case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr"; case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar"; default -> "db"; }; return op + " " + modRM16Operand(modrm) + ", 1"; }
    private String shift_rotate8_cl() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; String op = switch (reg) { case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr"; case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar"; default -> "db"; }; return op + " " + modRM8Operand(modrm) + ", cl"; }
    private String shift_rotate16_cl() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; String op = switch (reg) { case 0 -> "rol"; case 1 -> "ror"; case 2 -> "rcl"; case 3 -> "rcr"; case 4 -> "shl"; case 5 -> "shr"; case 7 -> "sar"; default -> "db"; }; return op + " " + modRM16Operand(modrm) + ", cl"; }
    private String group3A() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return switch (reg) { case 0 -> "test " + modRM8Operand(modrm) + ", ?"; case 2 -> "not " + modRM8Operand(modrm); case 3 -> "neg " + modRM8Operand(modrm); case 4 -> "mul " + modRM8Operand(modrm); case 5 -> "imul " + modRM8Operand(modrm); case 6 -> "div " + modRM8Operand(modrm); case 7 -> "idiv " + modRM8Operand(modrm); default -> "db 0xf6"; }; }
    private String group3B() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return switch (reg) { case 0 -> "test " + modRM16Operand(modrm) + ", ?"; case 2 -> "not " + modRM16Operand(modrm); case 3 -> "neg " + modRM16Operand(modrm); case 4 -> "mul " + modRM16Operand(modrm); case 5 -> "imul " + modRM16Operand(modrm); case 6 -> "div " + modRM16Operand(modrm); case 7 -> "idiv " + modRM16Operand(modrm); default -> "db 0xf7"; }; }
    private String group5() { int modrm = getModRM(); int reg = (modrm >> 3) & 7; return switch (reg) { case 0 -> "inc " + modRM16Operand(modrm); case 1 -> "dec " + modRM16Operand(modrm); case 2 -> "call " + modRM16Operand(modrm); case 3 -> "call far " + modRM16Operand(modrm); case 4 -> "jmp " + modRM16Operand(modrm); case 5 -> "jmp far " + modRM16Operand(modrm); case 6 -> "push " + modRM16Operand(modrm); default -> "db 0xff"; }; }
}
