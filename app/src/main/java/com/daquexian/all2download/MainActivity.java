package com.daquexian.all2download;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private boolean[] isObservering = new boolean[Constants.DIRECTORIES.length];
    private FileObserverService mService;
    private boolean bound;
    private final int[] observerInt = new int[1];

    // TODO: 16-12-7 增加开关按钮决定不同目录的监听与否
    // TODO: 16-12-7 权限申请被拒绝时增加重复申请和说明
    // TODO: 16-12-7 换一个图标
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            } else {
                startWorking();
            }
        } else {
            startWorking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            startWorking();
        }
    }

    private void startWorking() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.activity_main);
        final SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        observerInt[0] = sharedPreferences.getInt(Constants.SP_NAME, Integer.MIN_VALUE);
        LogUtil.d(TAG, "startWorking: " + observerInt[0]);
        for (int i = 0; i < isObservering.length; i++) {
            isObservering[i] = (observerInt[0] & (1 << i)) > 0;
        }

        for (int i = 0; i < Constants.DIR_NAMES.length; i++) {
            Switch mySwitch = new Switch(this);
            mySwitch.setText(Constants.DIR_NAMES[i]);
            mySwitch.setChecked(isObservering[i]);
            final int finalI = i;
            mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (mService != null) {
                        if (checked) {
                            observerInt[0] |= (1 << finalI);
                        } else {
                            observerInt[0] &= ~(1 << finalI);
                        }
                        sharedPreferences.edit().putInt(Constants.SP_NAME, observerInt[0]).apply();
                        mService.setEnabled(finalI, checked);
                    } else {
                        compoundButton.setChecked(!checked);
                    }
                }
            });
            layout.addView(mySwitch);
        }

        LogUtil.d(TAG, "startWorking: try to start service");
        startService(new Intent(this, FileObserverService.class));
        bindService(new Intent(this, FileObserverService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtil.d(TAG, "onServiceConnected: success");
            bound = true;
            mService = ((FileObserverService.FileObserverBinder) iBinder).getService();
            for (int i = 0; i < isObservering.length; i++) {
                mService.setEnabled(i, isObservering[i]);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
            LogUtil.d(TAG, "onServiceDisconnected: failure");
            new AlertDialog.Builder(MainActivity.this).setMessage(R.string.fatal_error).show();
            finish();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
}
