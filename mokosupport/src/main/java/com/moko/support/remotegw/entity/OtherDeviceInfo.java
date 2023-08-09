package com.moko.support.remotegw.entity;


import java.io.Serializable;
import java.util.List;

public class OtherDeviceInfo implements Serializable {

    public String mac;
    public int result_code;
    public String result_msg;
    public int mtu;
    public List<BleService> service_array;
}
