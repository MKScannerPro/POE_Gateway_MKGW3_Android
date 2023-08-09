package com.moko.support.mkgw3.event;

public class DeviceDeletedEvent {

    private int id;

    public DeviceDeletedEvent(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
