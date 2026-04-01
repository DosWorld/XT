package nz.co.electricbolt.xt.usermode.interrupts.dos;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.usermode.interrupts.annotations.*;
import java.io.IOException;

public class ConsoleIO {

    @Interrupt(function = 0x02, description = "Write character to standard output")
    public void writeCharacter(final CPU cpu, final @DL char c) {
        System.out.print(c);
    }

    @Interrupt(function = 0x09, description = "Write string to standard output")
    public void writeString(final CPU cpu, final @ASCIZ(terminationChar = '$') @DS @DX String s) {
        System.out.print(s);
        cpu.getReg().AL.setValue((byte) '$');
    }

    @Interrupt(function = 0x06, description = "Direct console input/output")
    public void directConsoleIO(final CPU cpu, final @DL byte dl) {
        if (dl == (byte) 0xFF) {
            try {
                if (System.in.available() > 0) {
                    int ch = System.in.read();
                    cpu.getReg().AL.setValue((byte) ch);
                    cpu.getReg().flags.setZero(false);
                } else {
                    cpu.getReg().AL.setValue((byte) 0);
                    cpu.getReg().flags.setZero(true);
                }
            } catch (IOException e) {
                cpu.getReg().AL.setValue((byte) 0);
                cpu.getReg().flags.setZero(true);
            }
        } else {
            System.out.print((char) (dl & 0xFF));
            cpu.getReg().flags.setZero(false);
        }
    }
}
