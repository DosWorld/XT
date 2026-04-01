package nz.co.electricbolt.xt.usermode.interrupts.dos;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.cpu.SegOfs;
import nz.co.electricbolt.xt.usermode.interrupts.annotations.*;
import nz.co.electricbolt.xt.usermode.util.MemoryUtil;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.charset.Charset;

public class ClipboardDOS {

    private static final int CLIPBOARD_INT = 0x1700;
    private static final Charset CP437 = Charset.forName("Cp437");

    @Interrupt(interrupt = 0x2F, function = 0x17, subfunction = 0x00, description = "Clipboard: Installation check")
    public void installCheck(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x1701);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x17, subfunction = 0x01, description = "Clipboard: Get text")
    public void getClipboard(CPU cpu, final @ES @DI SegOfs buffer, final @CX short maxLength) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            String text = (String) clipboard.getData(DataFlavor.stringFlavor);
            byte[] bytes = text.getBytes(CP437);
            int copyLen = Math.min(bytes.length, maxLength & 0xFFFF);
            for (int i = 0; i < copyLen; i++) {
                cpu.getMemory().writeByte(buffer, bytes[i]);
                buffer.increment();
            }
            cpu.getMemory().writeByte(buffer, (byte) 0);
            cpu.getReg().AX.setValue((short) copyLen);
            cpu.getReg().flags.setCarry(false);
        } catch (UnsupportedFlavorException | IOException e) {
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((short) 0x0000);
        }
    }

    @Interrupt(interrupt = 0x2F, function = 0x17, subfunction = 0x02, description = "Clipboard: Set text")
    public void setClipboard(CPU cpu, final @ASCIZ @DS @SI String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
        cpu.getReg().AX.setValue((short) text.length());
        cpu.getReg().flags.setCarry(false);
    }

    @Interrupt(interrupt = 0x2F, function = 0x17, subfunction = 0x03, description = "Clipboard: Clear")
    public void clearClipboard(CPU cpu) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(""), null);
        cpu.getReg().flags.setCarry(false);
    }
}
