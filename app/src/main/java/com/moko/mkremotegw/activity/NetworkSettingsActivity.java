package com.moko.mkremotegw.activity;

import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityNetworkSettingsBinding;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.support.remotegw.MokoSupport;
import com.moko.support.remotegw.OrderTaskAssembler;
import com.moko.support.remotegw.entity.OrderCHAR;
import com.moko.support.remotegw.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkSettingsActivity extends BaseActivity<ActivityNetworkSettingsBinding> {

    private final String IP_REGEX = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))*";

    private boolean mSavedParamsError;

    private Pattern pattern;

    @Override
    protected ActivityNetworkSettingsBinding getViewBinding() {
        return ActivityNetworkSettingsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        pattern = Pattern.compile(IP_REGEX);
        mBind.cbDhcp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mBind.clIp.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
        showLoadingProgressDialog();
        mBind.tvTitle.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getNetworkDHCP());
            orderTasks.add(OrderTaskAssembler.getNetworkIPInfo());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header == 0xED) {
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_NETWORK_IP_INFO:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                    case KEY_NETWORK_DHCP:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        if (mSavedParamsError) {
                                            ToastUtils.showToast(this, "Setup failed！");
                                        } else {
                                            ToastUtils.showToast(this, "Setup succeed！");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                if (length == 0)
                                    return;
                                // read
                                switch (configKeyEnum) {
                                    case KEY_NETWORK_DHCP:
                                        int enable = value[4];
                                        mBind.cbDhcp.setChecked(enable == 1);
                                        mBind.clIp.setVisibility(enable == 1 ? View.GONE : View.VISIBLE);
                                        break;
                                    case KEY_NETWORK_IP_INFO:
                                        if (length == 16) {
                                            String ip = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                    value[4] & 0xFF, value[5] & 0xFF, value[6] & 0xFF, value[7] & 0xFF);
                                            String mask = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                    value[8] & 0xFF, value[9] & 0xFF, value[10] & 0xFF, value[11] & 0xFF);
                                            String gateway = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                    value[12] & 0xFF, value[13] & 0xFF, value[14] & 0xFF, value[15] & 0xFF);
                                            String dns = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                    value[16] & 0xFF, value[17] & 0xFF, value[18] & 0xFF, value[19] & 0xFF);
                                            mBind.etIp.setText(ip);
                                            mBind.etMask.setText(mask);
                                            mBind.etGateway.setText(gateway);
                                            mBind.etDns.setText(dns);
                                        }
                                        break;

                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!isParaError()) {
            saveParams();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isParaError() {
        if (!mBind.cbDhcp.isChecked()) {
            String ip = mBind.etIp.getText().toString();
            String mask = mBind.etMask.getText().toString();
            String gateway = mBind.etGateway.getText().toString();
            String dns = mBind.etDns.getText().toString();
            Matcher matcherIp = pattern.matcher(ip);
            Matcher matcherMask = pattern.matcher(mask);
            Matcher matcherGateway = pattern.matcher(gateway);
            Matcher matcherDns = pattern.matcher(dns);
            if (!matcherIp.matches()
                    || !matcherMask.matches()
                    || !matcherGateway.matches()
                    || !matcherDns.matches())
                return true;
        }
        return false;
    }

    private void saveParams() {
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        if (!mBind.cbDhcp.isChecked()) {
            String ip = mBind.etIp.getText().toString();
            String mask = mBind.etMask.getText().toString();
            String gateway = mBind.etGateway.getText().toString();
            String dns = mBind.etDns.getText().toString();
            String[] ipArray = ip.split("\\.");
            String ipHex = String.format("%s%s%s%s",
                    MokoUtils.int2HexString(Integer.parseInt(ipArray[0])),
                    MokoUtils.int2HexString(Integer.parseInt(ipArray[1])),
                    MokoUtils.int2HexString(Integer.parseInt(ipArray[2])),
                    MokoUtils.int2HexString(Integer.parseInt(ipArray[3])));
            String[] maskArray = mask.split("\\.");
            String maskHex = String.format("%s%s%s%s",
                    MokoUtils.int2HexString(Integer.parseInt(maskArray[0])),
                    MokoUtils.int2HexString(Integer.parseInt(maskArray[1])),
                    MokoUtils.int2HexString(Integer.parseInt(maskArray[2])),
                    MokoUtils.int2HexString(Integer.parseInt(maskArray[3])));
            String[] gatewayArray = gateway.split("\\.");
            String gatewayHex = String.format("%s%s%s%s",
                    MokoUtils.int2HexString(Integer.parseInt(gatewayArray[0])),
                    MokoUtils.int2HexString(Integer.parseInt(gatewayArray[1])),
                    MokoUtils.int2HexString(Integer.parseInt(gatewayArray[2])),
                    MokoUtils.int2HexString(Integer.parseInt(gatewayArray[3])));
            String[] dnsArray = dns.split("\\.");
            String dnsHex = String.format("%s%s%s%s",
                    MokoUtils.int2HexString(Integer.parseInt(dnsArray[0])),
                    MokoUtils.int2HexString(Integer.parseInt(dnsArray[1])),
                    MokoUtils.int2HexString(Integer.parseInt(dnsArray[2])),
                    MokoUtils.int2HexString(Integer.parseInt(dnsArray[3])));
            orderTasks.add(OrderTaskAssembler.setNetworkIPInfo(ipHex, maskHex, gatewayHex, dnsHex));
        }
        orderTasks.add(OrderTaskAssembler.setNetworkDHCP(mBind.cbDhcp.isChecked() ? 1 : 0));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));

    }

    public void onBack(View view) {
        finish();
    }
}
