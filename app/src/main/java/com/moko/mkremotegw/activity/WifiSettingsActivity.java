package com.moko.mkremotegw.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityWifiSettingsBinding;
import com.moko.mkremotegw.dialog.BottomDialog;
import com.moko.mkremotegw.utils.FileUtils;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.support.remotegw.MokoSupport;
import com.moko.support.remotegw.OrderTaskAssembler;
import com.moko.support.remotegw.entity.OrderCHAR;
import com.moko.support.remotegw.entity.ParamsKeyEnum;
import com.moko.support.remotegw.entity.ParamsLongKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

public class WifiSettingsActivity extends BaseActivity<ActivityWifiSettingsBinding> {

    private final String FILTER_ASCII = "[ -~]*";
    private InputFilter filter;

    private ArrayList<String> mSecurityValues;
    private int mSecuritySelected;
    private ArrayList<String> mEAPTypeValues;
    private int mEAPTypeSelected;
    private boolean mSavedParamsError;
    private boolean mIsSaved;
    private String mCaPath;
    private String mCertPath;
    private String mKeyPath;

    @Override
    protected void onCreate() {
        mBind.cbVerifyServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mSecuritySelected != 0 && mEAPTypeSelected != 2)
                mBind.llCa.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        mSecurityValues = new ArrayList<>();
        mSecurityValues.add("Personal");
        mSecurityValues.add("Enterprise");
        mEAPTypeValues = new ArrayList<>();
        mEAPTypeValues.add("PEAP-MSCHAPV2");
        mEAPTypeValues.add("TTLS-MSCHAPV2");
        mEAPTypeValues.add("TLS");
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        mBind.etUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etEapPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etSsid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etDomainId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        showLoadingProgressDialog();
        mBind.tvTitle.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getWifiSecurityType());
            orderTasks.add(OrderTaskAssembler.getWifiSSID());
            orderTasks.add(OrderTaskAssembler.getWifiPassword());
            orderTasks.add(OrderTaskAssembler.getWifiEapType());
            orderTasks.add(OrderTaskAssembler.getWifiEapUsername());
            orderTasks.add(OrderTaskAssembler.getWifiEapPassword());
            orderTasks.add(OrderTaskAssembler.getWifiEapDomainId());
            orderTasks.add(OrderTaskAssembler.getWifiEapVerifyServiceEnable());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }


    @Override
    protected ActivityWifiSettingsBinding getViewBinding() {
        return ActivityWifiSettingsBinding.inflate(getLayoutInflater());
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
                        if (header == 0xEE) {
                            ParamsLongKeyEnum configKeyEnum = ParamsLongKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_WIFI_CLIENT_KEY:
                                    case KEY_WIFI_CLIENT_CERT:
                                    case KEY_WIFI_CA:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                }
                            }
                        }
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
                                    case KEY_WIFI_SECURITY_TYPE:
                                    case KEY_WIFI_SSID:
                                    case KEY_WIFI_EAP_USERNAME:
                                    case KEY_WIFI_EAP_PASSWORD:
                                    case KEY_WIFI_EAP_DOMAIN_ID:
                                    case KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE:
                                    case KEY_WIFI_PASSWORD:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                    case KEY_WIFI_EAP_TYPE:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        if (mSavedParamsError) {
                                            ToastUtils.showToast(this, "Setup failed！");
                                        } else {
                                            mIsSaved = true;
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
                                    case KEY_WIFI_SECURITY_TYPE:
                                        mSecuritySelected = value[4];
                                        mBind.tvSecurity.setText(mSecurityValues.get(mSecuritySelected));
                                        mBind.clEapType.setVisibility(mSecuritySelected != 0 ? View.VISIBLE : View.GONE);
                                        mBind.clPassword.setVisibility(mSecuritySelected != 0 ? View.GONE : View.VISIBLE);
                                        if (mSecuritySelected == 0) {
                                            mBind.llCa.setVisibility(View.GONE);
                                        } else {
                                            if (mEAPTypeSelected != 2) {
                                                mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                                            } else {
                                                mBind.llCa.setVisibility(View.VISIBLE);
                                            }
                                        }
                                        break;
                                    case KEY_WIFI_SSID:
                                        mBind.etSsid.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_PASSWORD:
                                        mBind.etPassword.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_EAP_PASSWORD:
                                        mBind.etEapPassword.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_EAP_TYPE:
                                        mEAPTypeSelected = value[4];
                                        mBind.tvEapType.setText(mEAPTypeValues.get(mEAPTypeSelected));
                                        if (mSecuritySelected == 0) {
                                            mBind.llCa.setVisibility(View.GONE);
                                            mBind.clUsername.setVisibility(View.GONE);
                                            mBind.clEapPassword.setVisibility(View.GONE);
                                            mBind.cbVerifyServer.setVisibility(View.GONE);
                                            mBind.clDomainId.setVisibility(View.GONE);
                                            mBind.llCert.setVisibility(View.GONE);
                                            mBind.llKey.setVisibility(View.GONE);
                                            mBind.tvCertTips.setVisibility(View.GONE);
                                        } else {
                                            if (mEAPTypeSelected != 2)
                                                mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                                            else
                                                mBind.llCa.setVisibility(View.VISIBLE);
                                            mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                                            mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                                            mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
                                            mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                            mBind.llCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                            mBind.llKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                            mBind.tvCertTips.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                        }
                                        break;
                                    case KEY_WIFI_EAP_USERNAME:
                                        mBind.etUsername.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_EAP_DOMAIN_ID:
                                        mBind.etDomainId.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE:
                                        mBind.cbVerifyServer.setChecked(value[4] == 1);
                                        if (mSecuritySelected != 0 && mEAPTypeSelected != 2)
                                            mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                                        break;

                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void onSelectSecurity(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mSecurityValues, mSecuritySelected);
        dialog.setListener(value -> {
            mSecuritySelected = value;
            mBind.tvSecurity.setText(mSecurityValues.get(value));
            mBind.clEapType.setVisibility(mSecuritySelected != 0 ? View.VISIBLE : View.GONE);
            mBind.clPassword.setVisibility(mSecuritySelected != 0 ? View.GONE : View.VISIBLE);
            if (mSecuritySelected == 0) {
                mBind.llCa.setVisibility(View.GONE);
                mBind.clUsername.setVisibility(View.GONE);
                mBind.clEapPassword.setVisibility(View.GONE);
                mBind.cbVerifyServer.setVisibility(View.GONE);
                mBind.clDomainId.setVisibility(View.GONE);
                mBind.llCert.setVisibility(View.GONE);
                mBind.llKey.setVisibility(View.GONE);
                mBind.tvCertTips.setVisibility(View.GONE);
            } else {
                if (mEAPTypeSelected != 2)
                    mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                else
                    mBind.llCa.setVisibility(View.VISIBLE);
                mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
                mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.llCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.llKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.tvCertTips.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSelectEAPType(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mEAPTypeValues, mEAPTypeSelected);
        dialog.setListener(value -> {
            mEAPTypeSelected = value;
            mBind.tvEapType.setText(mEAPTypeValues.get(value));
            mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
            mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
            mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
            mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            if (mEAPTypeSelected != 2)
                mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
            else
                mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            mBind.llKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            mBind.tvCertTips.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
        });
        dialog.show(getSupportFragmentManager());
    }


    public void selectCAFile(View view) {
        if (isWindowLocked()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"),
                    AppConstants.REQUEST_CODE_SELECT_CA);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    public void selectCertFile(View view) {
        if (isWindowLocked()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"),
                    AppConstants.REQUEST_CODE_SELECT_CLIENT_CERT);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"),
                    AppConstants.REQUEST_CODE_SELECT_CLIENT_KEY);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
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
        String ssid = mBind.etSsid.getText().toString();
        if (TextUtils.isEmpty(ssid))
            return true;
        if (mSecuritySelected != 0) {
            if (mEAPTypeSelected != 2 && !mBind.cbVerifyServer.isChecked()) {
                return false;
            }
            return TextUtils.isEmpty(mCaPath);
        }
        return false;
    }

    private void saveParams() {
        try {
            String ssid = mBind.etSsid.getText().toString();
            String username = mBind.etUsername.getText().toString();
            String password = mBind.etPassword.getText().toString();
            String eapPassword = mBind.etEapPassword.getText().toString();
            String domainId = mBind.etDomainId.getText().toString();
            showLoadingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setWifiSecurityType(mSecuritySelected));
            if (mSecuritySelected == 0) {
                orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
                orderTasks.add(OrderTaskAssembler.setWifiPassword(password));
            } else {
                if (mEAPTypeSelected != 2) {
                    orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
                    orderTasks.add(OrderTaskAssembler.setWifiEapUsername(username));
                    orderTasks.add(OrderTaskAssembler.setWifiEapPassword(eapPassword));
                    orderTasks.add(OrderTaskAssembler.setWifiEapVerifyServiceEnable(mBind.cbVerifyServer.isChecked() ? 1 : 0));
                    if (mBind.cbVerifyServer.isChecked())
                        orderTasks.add(OrderTaskAssembler.setWifiCA(new File(mCaPath)));
                } else {
                    orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
                    orderTasks.add(OrderTaskAssembler.setWifiEapDomainId(domainId));
                    orderTasks.add(OrderTaskAssembler.getWifiEapVerifyServiceEnable());
                    orderTasks.add(OrderTaskAssembler.setWifiCA(new File(mCaPath)));
                    if (!TextUtils.isEmpty(mCertPath))
                        orderTasks.add(OrderTaskAssembler.setWifiClientCert(new File(mCertPath)));
                    if (!TextUtils.isEmpty(mKeyPath))
                        orderTasks.add(OrderTaskAssembler.setWifiClientKey(new File(mKeyPath)));

                }
            }
            orderTasks.add(OrderTaskAssembler.setWifiEapType(mEAPTypeSelected));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        if (mIsSaved)
            setResult(RESULT_OK);
        finish();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
        //得到uri，后面就是将uri转化成file的过程。
        Uri uri = data.getData();
        String filePath = FileUtils.getPath(this, uri);
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(this, "file path error!");
            return;
        }
        final File file = new File(filePath);
        if (file.exists()) {
            if (requestCode == AppConstants.REQUEST_CODE_SELECT_CA) {
                mCaPath = filePath;
                mBind.tvCaFile.setText(filePath);
            }
            if (requestCode == AppConstants.REQUEST_CODE_SELECT_CLIENT_CERT) {
                mCertPath = filePath;
                mBind.tvCertFile.setText(filePath);
            }
            if (requestCode == AppConstants.REQUEST_CODE_SELECT_CLIENT_KEY) {
                mKeyPath = filePath;
                mBind.tvKeyFile.setText(filePath);
            }
        } else {
            ToastUtils.showToast(this, "file is not exists!");
        }
    }
}
