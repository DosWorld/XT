package nz.co.electricbolt.xt.usermode.filedevice;

import nz.co.electricbolt.xt.usermode.AccessMode;
import nz.co.electricbolt.xt.usermode.SharingMode;
import nz.co.electricbolt.xt.usermode.interrupts.dos.FileDateTime;

import java.io.IOException;
import java.nio.charset.Charset;

public class CONFile extends BaseFile {

    private final boolean isError;

    public CONFile(final AccessMode accessMode, final SharingMode sharingMode, final boolean inheritenceFlag) {
        this(accessMode, sharingMode, inheritenceFlag, false);
    }

    public CONFile(final AccessMode accessMode, final SharingMode sharingMode, final boolean inheritenceFlag, final boolean isError) {
        super("CON", accessMode, sharingMode, inheritenceFlag);
        this.isError = isError;
    }

    @Override
    public boolean open() {
        return true;
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public byte[] read(int bytesToRead) {
        if (accessMode != AccessMode.readOnly) {
            return new byte[0];
        }
        byte[] buf = new byte[bytesToRead];
        try {
            int read = 0;
            while (read < bytesToRead) {
                int ch = System.in.read();
                if (ch == -1) break;
                buf[read++] = (byte) ch;
                if (ch == '\r' || ch == '\n') break;
            }
            byte[] result = new byte[read];
            System.arraycopy(buf, 0, result, 0, read);
            return result;
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public void write(byte[] data) {
        String str = new String(data, Charset.forName("Cp437"));
        if (isError) {
            System.err.print(str);
        } else {
            System.out.print(str);
        }
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void seek(int position) {
    }

    @Override
    public int currentPos() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public FileDateTime getDateTime() {
        return null;
    }

    @Override
    public short getDeviceInformationWord() {
        return (short) 0x80D3;
    }
}
