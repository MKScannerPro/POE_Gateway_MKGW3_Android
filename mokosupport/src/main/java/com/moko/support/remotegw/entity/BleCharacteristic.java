package com.moko.support.remotegw.entity;

import java.io.Serializable;

public class BleCharacteristic implements Serializable {
    public String char_uuid;
    public int properties;
    public int notify_status;
}
