package com.moko.support.mkgw3.entity;

import java.io.Serializable;

public class BleCharacteristic implements Serializable {
    public String char_uuid;
    public int properties;
    public int notify_status;
}
