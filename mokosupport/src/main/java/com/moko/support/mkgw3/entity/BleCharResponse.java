package com.moko.support.mkgw3.entity;


import java.io.Serializable;

public class BleCharResponse implements Serializable {

    public String mac;
    public int result_code;
    public String service_uuid;
    public String char_uuid;
    public String payload;
}
