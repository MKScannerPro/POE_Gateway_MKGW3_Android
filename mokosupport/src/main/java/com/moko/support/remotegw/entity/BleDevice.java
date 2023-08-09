package com.moko.support.remotegw.entity;


import java.io.Serializable;

public class BleDevice implements Serializable {

    public String adv_name;
    public String mac;
    public int rssi;
    public int index;
    //0：ibeacon
    //1：eddystone-uid
    //2：eddystone-url
    //3：eddystone-tlm
    //4：bxp-devinfo
    //5：bxp-acc
    //6：bxp-th
    //7：bxp-button
    //8：bxp-tag
    //9：pir
    //10：other
    public int type_code;
    //值类型：number
    //0/1
    public int connectable;
}
