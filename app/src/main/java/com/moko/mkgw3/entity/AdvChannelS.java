package com.moko.mkgw3.entity;

import java.io.Serializable;

public class AdvChannelS implements Serializable {
    public int channel;
    public int enable;
    public int channel_type;
    public AdvChannelSParam normal_adv;
    public AdvChannelSParam trigger_before_adv;
    public AdvChannelSParam trigger_after_adv;
}
