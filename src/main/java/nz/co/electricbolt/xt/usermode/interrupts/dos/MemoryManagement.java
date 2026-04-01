package nz.co.electricbolt.xt.usermode.interrupts.dos;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.usermode.interrupts.annotations.Interrupt;

public class MemoryManagement {

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x00, description = "XMS: Installation check")
    public void xmsInstallCheck(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x10, description = "XMS: Query free extended memory")
    public void xmsQueryFree(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x11, description = "XMS: Allocate extended memory block")
    public void xmsAllocate(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x12, description = "XMS: Free extended memory block")
    public void xmsFree(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x13, description = "XMS: Move extended memory block")
    public void xmsMove(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x14, description = "XMS: Lock extended memory block")
    public void xmsLock(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x15, description = "XMS: Unlock extended memory block")
    public void xmsUnlock(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x16, description = "XMS: Get handle information")
    public void xmsHandleInfo(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
        cpu.getReg().CX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x17, description = "XMS: Reallocate extended memory block")
    public void xmsReallocate(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x18, description = "XMS: Enable A20 line")
    public void xmsEnableA20(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x19, description = "XMS: Disable A20 line")
    public void xmsDisableA20(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x1A, description = "XMS: Query A20 line status")
    public void xmsQueryA20(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x1B, description = "XMS: Register UMB handler")
    public void xmsRegisterUMB(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x1C, description = "XMS: Get UMB size")
    public void xmsGetUMBSize(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x1D, description = "XMS: Allocate UMB")
    public void xmsAllocateUMB(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x2F, function = 0x43, subfunction = 0x1E, description = "XMS: Free UMB")
    public void xmsFreeUMB(CPU cpu) {
        cpu.getReg().AX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x67, function = 0x00, description = "EMS: Installation check")
    public void emsInstallCheck(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().AL.setValue((byte) 0x00);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x67, function = 0x01, description = "EMS: Get EMS status")
    public void emsGetStatus(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().AL.setValue((byte) 0x00);
    }

    @Interrupt(interrupt = 0x67, function = 0x02, description = "EMS: Get page frame segment")
    public void emsGetPageFrame(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x03, description = "EMS: Get unallocated page count")
    public void emsGetUnallocatedPages(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().BX.setValue((short) 0x0000);
        cpu.getReg().DX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x67, function = 0x04, description = "EMS: Allocate pages")
    public void emsAllocatePages(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x05, description = "EMS: Map page")
    public void emsMapPage(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x06, description = "EMS: Free pages")
    public void emsFreePages(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x07, description = "EMS: Get version")
    public void emsGetVersion(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().AL.setValue((byte) 0x00);
    }

    @Interrupt(interrupt = 0x67, function = 0x08, description = "EMS: Save page map")
    public void emsSavePageMap(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x09, description = "EMS: Restore page map")
    public void emsRestorePageMap(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x0A, description = "EMS: Get handle pages")
    public void emsGetHandlePages(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x67, function = 0x0B, description = "EMS: Get handle count")
    public void emsGetHandleCount(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x0C, description = "EMS: Get handle pages (alternative)")
    public void emsGetHandlePagesAlt(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().BX.setValue((short) 0x0000);
    }

    @Interrupt(interrupt = 0x67, function = 0x0D, description = "EMS: Get handle count (alternative)")
    public void emsGetHandleCountAlt(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
    }

    @Interrupt(interrupt = 0x67, function = 0x0E, description = "EMS: Get page frame address")
    public void emsGetPageFrameAddr(CPU cpu) {
        cpu.getReg().AH.setValue((byte) 0x80);
        cpu.getReg().BX.setValue((short) 0x0000);
    }
}
