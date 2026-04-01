package nz.co.electricbolt.xt.usermode;

public enum SharingMode {
    compatibilityMode(0),
    denyAll(1),
    denyWrite(2),
    denyRead(3),
    denyNone(4);

    int sharingMode;

    SharingMode(int sharingMode) {
        this.sharingMode = sharingMode;
    }
}