package com.moko.support.remotegw.entity;

import java.io.Serializable;
import java.util.List;

public class BleService implements Serializable {
    //0:Primary Service
    //1:Secondary Service
    public int type;
    public String service_uuid;
    public List<BleCharacteristic> char_array;
}
