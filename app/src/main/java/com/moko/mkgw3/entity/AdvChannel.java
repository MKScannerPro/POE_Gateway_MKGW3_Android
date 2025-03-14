package com.moko.mkgw3.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class AdvChannel implements Serializable {
    public int channel;
    public int enable;
    public int channel_type;
    public int adv_type;
    public AdvParam normal_adv;
    public AdvParam trigger_before_adv;
    public AdvParam trigger_after_adv;
}
