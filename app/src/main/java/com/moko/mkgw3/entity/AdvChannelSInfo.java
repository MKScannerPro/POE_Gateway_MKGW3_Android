package com.moko.mkgw3.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class AdvChannelSInfo implements Serializable {
    public String mac;
    public int result_code;
    public ArrayList<AdvChannelS> adv_param;
}
