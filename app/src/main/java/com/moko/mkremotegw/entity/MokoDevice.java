package com.moko.mkremotegw.entity;


import java.io.Serializable;

public class MokoDevice implements Serializable {

    public int id;
    public String name;
    public String mac;
    public String mqttInfo;
    public int lwtEnable;
    public String lwtTopic;
    public String topicPublish;
    public String topicSubscribe;
    public boolean isOnline;
    public int deviceType;
    public int netStatus;
}
