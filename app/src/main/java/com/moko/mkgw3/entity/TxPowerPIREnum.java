package com.moko.mkgw3.entity;


import java.io.Serializable;

public enum TxPowerPIREnum implements Serializable {
    NEGATIVE_40(-40),
    NEGATIVE_20(-20),
    NEGATIVE_16(-16),
    NEGATIVE_12(-12),
    NEGATIVE_8(-8),
    NEGATIVE_4(-4),
    NEGATIVE_0(0),
    POSITIVE_4(4);

    private int txPower;

    TxPowerPIREnum(int txPower) {
        this.txPower = txPower;
    }

    public static TxPowerPIREnum fromOrdinal(int ordinal) {
        for (TxPowerPIREnum txPowerEnum : TxPowerPIREnum.values()) {
            if (txPowerEnum.ordinal() == ordinal) {
                return txPowerEnum;
            }
        }
        return null;
    }
    public static TxPowerPIREnum fromTxPower(int txPower) {
        for (TxPowerPIREnum txPowerEnum : TxPowerPIREnum.values()) {
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
