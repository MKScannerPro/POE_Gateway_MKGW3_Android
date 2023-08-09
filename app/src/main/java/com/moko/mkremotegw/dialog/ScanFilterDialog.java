package com.moko.mkremotegw.dialog;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.moko.mkremotegw.R;
import com.moko.mkremotegw.databinding.DialogScanFilterBinding;


public class ScanFilterDialog extends MokoBaseDialog<DialogScanFilterBinding> {
    public static final String TAG = ScanFilterDialog.class.getSimpleName();

    private int filterRssi;
    private String filterName;
    private String filterMac;

    @Override
    protected DialogScanFilterBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogScanFilterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        mBind.tvRssi.setText(String.format("%sdBm", filterRssi + ""));
        mBind.sbRssi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int rssi = (progress * -1);
                mBind.tvRssi.setText(String.format("%sdBm", rssi + ""));
                filterRssi = rssi;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBind.sbRssi.setProgress(Math.abs(filterRssi));
        if (!TextUtils.isEmpty(filterName)) {
            mBind.etFilterName.setText(filterName);
            mBind.etFilterName.setSelection(filterName.length());
        }
        if (!TextUtils.isEmpty(filterMac)) {
            mBind.etFilterMac.setText(filterMac);
            mBind.etFilterMac.setSelection(filterMac.length());
        }
        mBind.ivFilterNameDelete.setOnClickListener(v -> mBind.etFilterName.setText(""));
        mBind.ivFilterMacDelete.setOnClickListener(v -> mBind.etFilterMac.setText(""));
        mBind.tvDone.setOnClickListener(v -> {
            listener.onDone(mBind.etFilterName.getText().toString(),
                    mBind.etFilterMac.getText().toString(),
                    filterRssi);
            dismiss();
        });
    }

    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return true;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    private OnScanFilterListener listener;

    public void setOnScanFilterListener(OnScanFilterListener listener) {
        this.listener = listener;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void setFilterMac(String filterMac) {
        this.filterMac = filterMac;
    }

    public void setFilterRssi(int filterRssi) {
        this.filterRssi = filterRssi;
    }

    public interface OnScanFilterListener {
        void onDone(String filterName, String filterMac, int filterRssi);
    }
}
