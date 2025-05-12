package com.moko.mkgw3.entity;


import java.io.Serializable;

public enum TxPowerTagEnum implements Serializable {
    NEGATIVE_20(-20),
    NEGATIVE_16(-16),
    NEGATIVE_12(-12),
    NEGATIVE_8(-8),
    NEGATIVE_4(-4),
    NEGATIVE_0(0),
    POSITIVE_3(3),
    POSITIVE_4(4),
    POSITIVE_6(6);

    private int txPower;

    TxPowerTagEnum(int txPower) {
        this.txPower = txPower;
    }

    public static TxPowerTagEnum fromOrdinal(int ordinal) {
        for (TxPowerTagEnum txPowerEnum : TxPowerTagEnum.values()) {
            if (txPowerEnum.ordinal() == ordinal) {
                return txPowerEnum;
            }
        }
        return null;
    }
    public static TxPowerTagEnum fromTxPower(int txPower) {
        for (TxPowerTagEnum txPowerEnum : TxPowerTagEnum.values()) {
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
