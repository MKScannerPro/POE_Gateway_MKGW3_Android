package com.moko.support.mkgw3.event;

public class MQTTMessageArrivedEvent {

    private String topic;
    private String message;

    public MQTTMessageArrivedEvent(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }


    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }
}
