package com.moko.support.remotegw.entity;


import java.io.Serializable;
import java.util.List;

public class BleCharResponse implements Serializable {

    public String mac;
    public int result_code;
    public String service_uuid;
    public String char_uuid;
    public String payload;
}
