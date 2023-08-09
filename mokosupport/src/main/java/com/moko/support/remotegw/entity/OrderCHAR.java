package com.moko.support.remotegw.entity;

import java.io.Serializable;
import java.util.UUID;

public enum OrderCHAR implements Serializable {
    // 180A
    CHAR_MODEL_NUMBER(UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB")),
    CHAR_FIRMWARE_REVISION(UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB")),
    CHAR_HARDWARE_REVISION(UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB")),
    CHAR_SOFTWARE_REVISION(UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB")),
    CHAR_MANUFACTURER_NAME(UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB")),
    // AA00
    CHAR_PASSWORD(UUID.fromString("0000AA00-0000-1000-8000-00805F9B34FB")),
    CHAR_DISCONNECTED_NOTIFY(UUID.fromString("0000AA01-0000-1000-8000-00805F9B34FB")),
    CHAR_PARAMS(UUID.fromString("0000AA03-0000-1000-8000-00805F9B34FB")),
    ;

    private UUID uuid;

    OrderCHAR(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
