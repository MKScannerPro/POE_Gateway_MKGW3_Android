package com.moko.support.mkgw3.entity;


import java.io.Serializable;

public class BeaconInfo implements Serializable {

    public String mac;
    public int result_code;
    public String result_msg;
    public String product_model;
    public String company_name;
    public String hardware_version;
    public String software_version;
    public String firmware_version;
    public int sensor_status;
    public int battery_v;
    public int battery_level;
    public int type;
    // MK-PIR
    public int run_time;
    // BXP-B-D/BXP-B-CR
    public int single_alarm_num;
    public int double_alarm_num;
    public int long_alarm_num;
    public int alarm_status;
    // BXP-S
    public int axis_type;
    public int th_type;
    public int light_type;
    public int pir_type;
    public int tof_type;
}
