package com.jushi.library.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jushi.library.utils.PermissionUtil;
import com.jushi.library.utils.ToastUtil;

import java.util.List;

/**
 * 权限申请基类activity
 */
abstract class BasePermissionActivity extends AppCompatActivity {
    private final int REQUEST_CODE_PERMISSIONS_CAMERA = 0x01;
    private final int REQUEST_CODE_PERMISSIONS_EXTERNAL_STORAGE = 0x02;
    private final int REQUEST_CODE_PERMISSIONS_LOCATION = 0x03;
    private final int REQUEST_CODE_PERMISSIONS_RECORD_AUDIO = 0x04;
    private final int SYSTEM_ALERT_WINDOW_CODE = 0x05;
    private final int REQUEST_OPEN_BLUETOOTH = 0x06;
    private final int REQUEST_OPEN_NOTICE = 0x07;
    private final int REQUEST_CODE_PERMISSIONS_CALL_PHONE = 0x08;

    protected BluetoothAdapter bluetoothAdapter;

    /**
     * 检查相机权限
     *
     * @return
     */
    protected boolean checkCameraPermission() {
        return PermissionUtil.request(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS_CAMERA);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS_CAMERA);
//            return false;
//        }
//        return true;
    }

    /**
     * 检查SD卡读写权限
     *
     * @return
     */
    protected boolean checkExternalStoragePermission() {
        return PermissionUtil.request(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_PERMISSIONS_EXTERNAL_STORAGE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_CODE_PERMISSIONS_EXTERNAL_STORAGE);
//            return false;
//        }
//        return true;
    }

    /**
     * 检查位置权限
     *
     * @return
     */
    protected boolean checkLocationPermission() {
        return PermissionUtil.request(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_PERMISSIONS_LOCATION);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                    REQUEST_CODE_PERMISSIONS_LOCATION);
//            return false;
//        }
//        return true;
    }

    /**
     * 检查录音权限
     *
     * @return
     */
    protected boolean checkRecordAudioPermission() {
        return PermissionUtil.request(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSIONS_RECORD_AUDIO);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
//                    REQUEST_CODE_PERMISSIONS_RECORD_AUDIO);
//            return false;
//        }
//        return true;
    }

    /**
     * 检查悬浮窗权限
     *
     * @return
     */
    protected boolean checkAlertWindowPermission() {
        if (canDrawOverlays()) return true;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
        return false;
    }

    /**
     * 是否有悬浮窗权限
     *
     * @return
     */
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    /**
     * 请求开启蓝牙
     */
    protected void requestOpenBluetooth() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter == null) {
            showToast("该设备不支持蓝牙");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            //请求打开并可见
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_OPEN_BLUETOOTH);
        }
    }

    /**
     * 蓝牙是否开启
     *
     * @return
     */
    protected boolean bluetoothIsEnabled() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter == null) {
            showToast("该设备不支持蓝牙");
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * 跳转系统蓝牙设置页面
     */
    protected void openBluetoothSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * 是否开启了系统通知权限
     *
     * @return true-已开启通知权限  false-未开启通知权限
     */
    protected boolean isNotificationEnabled() {
        boolean isOpened = false;
        try {
            isOpened = NotificationManagerCompat.from(this).areNotificationsEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOpened;
    }

    /**
     * 跳转系统通知设置页面
     */
    protected void toSystemNoticeSettings(String pkgName){
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
//            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        }
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){//这种方案适用于 API 26, 即8.0（含8.0）以上可以用
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, pkgName);
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, getApplicationInfo().uid);
        }else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP&&Build.VERSION.SDK_INT<=Build.VERSION_CODES.N_MR1){
            //这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
            intent.putExtra("app_package", getPackageName());
            intent.putExtra("app_uid", getApplicationInfo().uid);
        }else { //跳转到应用设置界面
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
        }
        startActivityForResult(intent,REQUEST_OPEN_NOTICE);
    }

    /**
     * 检查拨打电话权限
     * @return
     */
    protected boolean checkCallPhonePermission(){
        return PermissionUtil.request(this,new String[]{Manifest.permission.CALL_PHONE},REQUEST_CODE_PERMISSIONS_CALL_PHONE);
    }

    private void showToast(String msg) {
        ToastUtil.showToast(this, msg);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        onFragmentPermissionResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS_CAMERA:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onCameraPermissionOpened();
                } else {
                    showToast("相机权限已被禁止");
                }
                break;
            case REQUEST_CODE_PERMISSIONS_EXTERNAL_STORAGE:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onExternalStoragePermissionOpened();
                } else {
                    showToast("存储权限已被禁止");
                }
                break;
            case REQUEST_CODE_PERMISSIONS_LOCATION:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onLocationPermissionOpened();
                } else {
                    showToast("定位权限已被禁止");
                }
                break;
            case REQUEST_CODE_PERMISSIONS_RECORD_AUDIO:
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onRecordAudioPermissionOpened();
                } else {
                    showToast("录音权限已被禁止");
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SYSTEM_ALERT_WINDOW_CODE:
                if (canDrawOverlays()) {
                    onAlertWindowPermissionOpened();
                } else {
                    showToast("悬浮窗权限已被禁止");
                }
                break;
            case REQUEST_OPEN_BLUETOOTH:
                if (bluetoothAdapter.isEnabled()) {
                    onBluetoothOpened();
                } else {
                    showToast("请求打开蓝牙被拒");
                }
                break;
            case REQUEST_OPEN_NOTICE:
                if (isNotificationEnabled()) {
                    onNotificationPermissionOpened();
                } else {
                    showToast("通知权限被禁止");
                }
                break;
            case REQUEST_CODE_PERMISSIONS_CALL_PHONE:
                if (isNotificationEnabled()) {
                    onCallPhonePermissionOpened();
                } else {
                    showToast("拨打电话权限已被禁止");
                }
                break;
        }
    }

    /**
     * 拨打电话权限开启
     */
    protected abstract void onCallPhonePermissionOpened();

    /**
     * 权限事件传递到fragment
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    private void onFragmentPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment == null) continue;
            handleChildPermissionResult(fragment, requestCode, permissions, grantResults);
        }
    }

    private void handleChildPermissionResult(Fragment fragment, int requestCode, String[] permissions, int[] grantResults) {
        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);//onRequestPermissionsResult
        List<Fragment> childFragment = fragment.getChildFragmentManager().getFragments(); //找到第二层Fragment
        for (Fragment f : childFragment) {
            if (f == null) continue;
            handleChildPermissionResult(f, requestCode, permissions, grantResults);
        }
    }

    /**
     * 相机权限打开
     */
    protected abstract void onCameraPermissionOpened();

    /**
     * 存储权限打开
     */
    protected abstract void onExternalStoragePermissionOpened();

    /**
     * 定位权限打开
     */
    protected abstract void onLocationPermissionOpened();

    /**
     * 录音权限打开
     */
    protected abstract void onRecordAudioPermissionOpened();

    /**
     * 悬浮窗权限打开
     */
    protected abstract void onAlertWindowPermissionOpened();

    /**
     * 蓝牙已打开
     */
    protected abstract void onBluetoothOpened();

    /**
     * 通知权限已打开
     */
    protected abstract void onNotificationPermissionOpened();
}
