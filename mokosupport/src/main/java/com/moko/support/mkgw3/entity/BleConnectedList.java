package com.moko.support.mkgw3.entity;

import java.util.List;

public class BleConnectedList {
    public List<BleDevice> ble_conn_list;

    public static class BleDevice {
        // 0:通用连接
        // 1:bxp_b_d
        // 2:bxp_b_cr
        // 3:bxp_c
        // 4:bxp_d
        // 5:bxp_tag
        // 6:bxp_s
        // 7:pir
        // 8:tof
        public int type;
        public String mac;
    }
}
