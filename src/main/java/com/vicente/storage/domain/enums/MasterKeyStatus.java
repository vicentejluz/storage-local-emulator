package com.vicente.storage.domain.enums;

public enum MasterKeyStatus {
    ACTIVE(true),
    ROTATING(false),
    INACTIVE(false);

    private final boolean usable;

    MasterKeyStatus(boolean usable) {
        this.usable = usable;
    }

    public boolean isUsable() {
        return usable;
    }
}
