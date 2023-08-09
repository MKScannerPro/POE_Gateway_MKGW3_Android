package com.moko.support.remotegw.entity;

import java.util.List;

public class BleConnectedList {
    public List<BleDevice> ble_conn_list;

    public static class BleDevice {
        // 0:通用连接
        // 1:bxp_b1连接
        public int type;
        public String mac;
    }
}
