package com.moko.mkgw3.net.entity;


import java.io.Serializable;

public class SyncDevice implements Serializable {

    public String macName;
    public String mac;
    public String lastWill;
    public String publishTopic;
    public String subscribeTopic;
    // MKGW-mini 01 10
    // MK107 20
    // MK107D Pro 30
    // MK110 Plus 01 40
    // MK110 Plus 02 50
    // MK110 Plus 03 60
    // MKGW3 70
    // MKGW1 80
    // LW003-B 90
    // SGWP-B 100
    // JP 200
    public String model;
}
