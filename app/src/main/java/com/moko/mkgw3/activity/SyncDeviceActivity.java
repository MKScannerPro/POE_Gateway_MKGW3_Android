package com.moko.mkgw3.activity;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.moko.mkgw3.R;
import com.moko.mkgw3.adapter.SyncDeviceAdapter;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityDevicesBinding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.dialog.LogoutDialog;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.net.Urls;
import com.moko.mkgw3.net.entity.CommonResp;
import com.moko.mkgw3.net.entity.SyncDevice;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.event.MQTTConnectionCompleteEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import okhttp3.RequestBody;

public class SyncDeviceActivity extends BaseActivity<ActivityDevicesBinding> implements BaseQuickAdapter.OnItemClickListener {

    private ArrayList<MokoDeviceKgw3> devices;
    private SyncDeviceAdapter adapter;
    public Handler mHandler;


    @Override
    protected void onCreate() {
        devices = MKgw3DBTools.getInstance(this).selectAllDevice();
        adapter = new SyncDeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        mBind.rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDeviceList.setAdapter(adapter);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected ActivityDevicesBinding getViewBinding() {
        return ActivityDevicesBinding.inflate(getLayoutInflater());
    }


    public void onSyncDevices(View view) {
        if (isWindowLocked()) return;
        if (devices.isEmpty()) {
            ToastUtils.showToast(this, "Add devices first");
            return;
        }
        List<SyncDevice> syncDevices = new ArrayList();
        List<MokoDeviceKgw3> deviceList = adapter.getData();
        for (MokoDeviceKgw3 device : deviceList) {
            if (device.isSelected) {
                SyncDevice syncDevice = new SyncDevice();
                syncDevice.mac = device.mac;
                syncDevice.macName = device.name;
                syncDevice.publishTopic = device.topicPublish;
                syncDevice.subscribeTopic = device.topicSubscribe;
                syncDevice.lastWill = device.lwtTopic;
                syncDevice.model = "70";
                syncDevices.add(syncDevice);
            }
        }
        syncDevices(syncDevices);
    }

    private void syncDevices(List<SyncDevice> syncDevices) {
        RequestBody body = RequestBody.create(Urls.JSON, new Gson().toJson(syncDevices));
        OkGo.<String>post(Urls.syncGatewayApi(getApplicationContext()))
                .upRequestBody(body)
                .headers("Authorization", MKGW3MainActivity.mAccessToken)
                .execute(new StringCallback() {

                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        showLoadingProgressDialog();
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        Type type = new TypeToken<CommonResp<JsonObject>>() {
                        }.getType();
                        CommonResp<JsonObject> commonResp = new Gson().fromJson(response.body(), type);
                        if (commonResp.code != 200) {
                            ToastUtils.showToast(SyncDeviceActivity.this, commonResp.msg);
                            return;
                        }
                        ToastUtils.showToast(SyncDeviceActivity.this, "Sync Success");
                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showToast(SyncDeviceActivity.this, R.string.request_error);
                    }

                    @Override
                    public void onFinish() {
                        dismissLoadingProgressDialog();
                    }
                });
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDeviceKgw3 mokoDevice = (MokoDeviceKgw3) adapter.getItem(position);
        if (mokoDevice == null)
            return;
        boolean isSelected = mokoDevice.isSelected;
        mokoDevice.isSelected = !isSelected;
        adapter.notifyItemChanged(position);
    }


    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        finish();
    }

    public void onAccount(View view) {
        if (isWindowLocked()) return;
        LogoutDialog dialog = new LogoutDialog();
        dialog.setOnLogoutClicked(() -> {
            back();
        });
        dialog.show(getSupportFragmentManager());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }
}
