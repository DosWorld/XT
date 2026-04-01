package nz.co.electricbolt.xt.usermode.interrupts.dos;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.cpu.SegOfs;
import nz.co.electricbolt.xt.usermode.ProgramLoader;
import nz.co.electricbolt.xt.usermode.interrupts.annotations.*;
import nz.co.electricbolt.xt.usermode.util.DirectoryTranslation;
import nz.co.electricbolt.xt.usermode.util.Trace;

import java.io.File;
import java.util.Stack;

public class Exec {

    @Interrupt(interrupt = 0x21, function = 0x4B, subfunction = 0x00, description = "EXEC: Load and execute program")
    public void exec(CPU cpu, Trace trace, DirectoryTranslation dirTrans,
                     @ASCIZ @DS @DX String filename, @ES @BX SegOfs parameterBlock) {
        filename = dirTrans.emulatedPathToHostPath(filename);
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((short) 0x0002);
            return;
        }

        short envSegment = cpu.getMemory().readWord(parameterBlock);
        SegOfs cmdLinePtr = new SegOfs(
            cpu.getMemory().readWord(new SegOfs(parameterBlock.getSegment(), (short)(parameterBlock.getOffset() + 2))),
            cpu.getMemory().readWord(new SegOfs(parameterBlock.getSegment(), (short)(parameterBlock.getOffset() + 4)))
        );
        byte cmdLen = cpu.getMemory().readByte(cmdLinePtr);
        byte[] cmdBuf = new byte[cmdLen];
        for (int i = 0; i < cmdLen; i++) {
            cmdBuf[i] = cpu.getMemory().readByte(new SegOfs(cmdLinePtr.getSegment(), (short)(cmdLinePtr.getOffset() + 1 + i)));
        }
        String commandLine = new String(cmdBuf).trim();

        TerminateProgram.pushContext(cpu);

        short parentPSP = cpu.getReg().DS.getValue();
        short parentEnv = cpu.getMemory().readWord(new SegOfs(parentPSP, (short) 0x002C));

        long fileSize = file.length();
        short paragraphsNeeded = (short) (((fileSize + 256 + 15) / 16) + 1);
        short[] allocatedSegment = new short[1];
        allocateMemory(cpu, paragraphsNeeded, allocatedSegment);
        if (cpu.getReg().flags.isCarry()) {
            TerminateProgram.popContext(cpu);
            return;
        }

        short childPSP = allocatedSegment[0];

        for (int i = 0; i < 256; i++) {
            cpu.getMemory().writeByte(new SegOfs(childPSP, (short) i), (byte) 0);
        }
        cpu.getMemory().writeWord(new SegOfs(childPSP, (short) 0x002C), envSegment != 0 ? envSegment : parentEnv);
        cpu.getMemory().writeWord(new SegOfs(childPSP, (short) 0x0002), (short) 0x0090);
        cpu.getMemory().writeWord(new SegOfs(childPSP, (short) 0x0004), (short) 0x0090);
        cpu.getMemory().writeWord(new SegOfs(childPSP, (short) 0x0006), (short) 0xFFFF);
        cpu.getMemory().writeWord(new SegOfs(childPSP, (short) 0x0008), (short) 0xFFFF);

        SegOfs cmdLine = new SegOfs(childPSP, (short) 0x0080);
        cpu.getMemory().writeByte(cmdLine, (byte) commandLine.length());
        for (int i = 0; i < commandLine.length(); i++) {
            cpu.getMemory().writeByte(new SegOfs(cmdLine.getSegment(), (short)(cmdLine.getOffset() + 1 + i)), (byte) commandLine.charAt(i));
        }

        ProgramLoader loader = new ProgramLoader(cpu);
        loader.load(filename, childPSP);

        TerminateProgram.setChildSegment(childPSP);

        cpu.getReg().flags.setCarry(false);
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    private void allocateMemory(CPU cpu, short paragraphs, short[] result) {
        cpu.getReg().BX.setValue(paragraphs);
        Memory.allocateMemoryBlockStatic(cpu, cpu.getReg().BX.getValue());
        if (!cpu.getReg().flags.isCarry()) {
            result[0] = cpu.getReg().AX.getValue();
        }
    }
}
