package com.moko.mkgw3.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class AdvChannelInfo implements Serializable {
    public String mac;
    public int result_code;
    public ArrayList<AdvChannel> adv_param;
}
