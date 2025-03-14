package com.moko.mkgw3.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class AdvParamInfo implements Serializable {
    public String mac;
    public int result_code;
    public ArrayList<SlotAdvParams> adv_param;
}
