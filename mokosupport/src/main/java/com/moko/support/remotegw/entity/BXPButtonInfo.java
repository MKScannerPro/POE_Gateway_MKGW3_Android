package com.moko.support.remotegw.entity;


import java.io.Serializable;

public class BXPButtonInfo implements Serializable {

    public String mac;
    public int result_code;
    public String result_msg;
    public String product_model;
    public String company_name;
    public String hardware_version;
    public String software_version;
    public String firmware_version;
    public int battery_v;
    public int single_alarm_num;
    public int double_alarm_num;
    public int long_alarm_num;
    public int alarm_status;
}
