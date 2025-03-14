package com.moko.support.mkgw3;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoBleLib;
import com.moko.ble.lib.MokoBleManager;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.mkgw3.entity.OrderCHAR;
import com.moko.support.mkgw3.handler.MokoCharacteristicHandler;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class MokoSupport extends MokoBleLib {
    private HashMap<OrderCHAR, BluetoothGattCharacteristic> mCharacteristicMap;

    private static volatile MokoSupport INSTANCE;

    private Context mContext;

    private MokoSupport() {
        //no instance
    }

    public static MokoSupport getInstance() {
        if (INSTANCE == null) {
            synchronized (MokoSupport.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MokoSupport();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        super.init(context);
    }

    @Override
    public MokoBleManager getMokoBleManager() {
        MokoBleConfig mokoSupportBleManager = new MokoBleConfig(mContext, this);
        return mokoSupportBleManager;
    }

    ///////////////////////////////////////////////////////////////////////////
    // connect
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onDeviceConnected(BluetoothGatt gatt) {
        mCharacteristicMap = new MokoCharacteristicHandler().getCharacteristics(gatt);
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCONNECTED);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public BluetoothGattCharacteristic getCharacteristic(Enum orderCHAR) {
        return mCharacteristicMap.get(orderCHAR);
    }

    ///////////////////////////////////////////////////////////////////////////
    // order
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isCHARNull() {
        if (mCharacteristicMap == null || mCharacteristicMap.isEmpty()) {
            disConnectBle();
            return true;
        }
        return false;
    }

    @Override
    public void orderFinish() {
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_FINISH);
        EventBus.getDefault().post(event);
    }

    @Override
    public void orderTimeout(OrderTaskResponse response) {
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_TIMEOUT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
    }

    @Override
    public void orderResult(OrderTaskResponse response) {
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_RESULT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
    }

    @Override
    public boolean orderResponseValid(BluetoothGattCharacteristic characteristic, OrderTask orderTask) {
        final UUID responseUUID = characteristic.getUuid();
        final OrderCHAR orderCHAR = (OrderCHAR) orderTask.orderCHAR;
        return responseUUID.equals(orderCHAR.getUuid());
    }

    private String dataBytesStr = "";

    @Override
    public boolean orderNotify(BluetoothGattCharacteristic characteristic, byte[] value) {
        final UUID responseUUID = characteristic.getUuid();
        OrderCHAR orderCHAR = null;
        if (responseUUID.equals(OrderCHAR.CHAR_DISCONNECTED_NOTIFY.getUuid())) {
            orderCHAR = OrderCHAR.CHAR_DISCONNECTED_NOTIFY;
        }
        if (responseUUID.equals(OrderCHAR.CHAR_PARAMS.getUuid())) {
            if (value != null && value.length > 2 && (value[2] & 0Xff) == 0x51) {
                orderCHAR = OrderCHAR.CHAR_PARAMS;
                final int cmd = value[2] & 0xFF;
                final int packetCount = value[3] & 0xFF;
                final int indexPack = value[4] & 0xFF;
                final int length = value[5] & 0xFF;
                if (indexPack < (packetCount - 1)) {
                    byte[] remainBytes = Arrays.copyOfRange(value, 6, 6 + length);
                    dataBytesStr += MokoUtils.bytesToHexString(remainBytes);
                } else {
                    if (length == 0) {
                        byte[] data = new byte[5];
                        data[0] = (byte) 0xEE;
                        data[1] = (byte) 0x02;
                        data[2] = (byte) cmd;
                        data[3] = 0;
                        data[4] = 0;
                        value = data;
                    } else {
                        byte[] remainBytes = Arrays.copyOfRange(value, 6, 6 + length);
                        dataBytesStr += MokoUtils.bytesToHexString(remainBytes);
                        byte[] dataBytes = MokoUtils.hex2bytes(dataBytesStr);
                        int dataLength = dataBytes.length;
                        byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
                        byte[] data = new byte[dataLength + 5];
                        data[0] = (byte) 0xEE;
                        data[1] = (byte) 0x02;
                        data[2] = (byte) cmd;
                        data[3] = dataLengthBytes[0];
                        data[4] = dataLengthBytes[1];
                        for (int i = 0; i < dataLength; i++) {
                            data[i + 5] = dataBytes[i];
                        }
                        dataBytesStr = "";
                        value = data;
                    }
                }
            }
        }
        if (orderCHAR == null)
            return false;
        XLog.i(orderCHAR.name());
        OrderTaskResponse response = new OrderTaskResponse();
        response.orderCHAR = orderCHAR;
        response.responseValue = value;
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_CURRENT_DATA);
        event.setResponse(response);
        EventBus.getDefault().post(event);
        return true;
    }
}
