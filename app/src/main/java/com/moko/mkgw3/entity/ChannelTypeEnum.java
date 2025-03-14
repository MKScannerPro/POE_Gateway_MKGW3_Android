package com.moko.mkgw3.entity;


import java.io.Serializable;

public enum ChannelTypeEnum implements Serializable {
    UID(-40),
    URL(-20),
    TLM(-16),
    EID(-12),
    DEVICE_INFO(-8),
    iBeacon(-4),
    AXIS(0),
    TH(3),
    NO_DATA(4);

    private int txPower;

    ChannelTypeEnum(int txPower) {
        this.txPower = txPower;
    }

    public static ChannelTypeEnum fromOrdinal(int ordinal) {
        for (ChannelTypeEnum txPowerEnum : ChannelTypeEnum.values()) {
            if (txPowerEnum.ordinal() == ordinal) {
                return txPowerEnum;
            }
        }
        return null;
    }
    public static ChannelTypeEnum fromTxPower(int txPower) {
        for (ChannelTypeEnum txPowerEnum : ChannelTypeEnum.values()) {
            if (txPowerEnum.getTxPower() == txPower) {
                return txPowerEnum;
            }
        }
        return null;
    }

    public int getTxPower() {
        return txPower;
    }
}
