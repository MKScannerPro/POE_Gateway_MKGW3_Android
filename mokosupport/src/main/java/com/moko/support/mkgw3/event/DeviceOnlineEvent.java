package com.moko.support.mkgw3.event;

public class DeviceOnlineEvent {

    private String mac;
    private boolean online;

    public DeviceOnlineEvent(String mac, boolean online) {
        this.mac = mac;
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public String getMac() {
        return mac;
    }
}
