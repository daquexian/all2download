package com.daquexian.all2download;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
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
    private boolean[] isObserving = new boolean[Constants.DIRECTORIES.length];
    private FileObserverService mService;
    private boolean bound;
    private final int[] observerInt = new int[1];
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);

        /**
         * show welcome message on first time
         */
        if (sharedPreferences.getBoolean(Constants.IS_FIRST_TIME, true)) {
            new AlertDialog.Builder(this).setMessage(R.string.welcome_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            sharedPreferences.edit().putBoolean(Constants.IS_FIRST_TIME, false).apply();
                            init();
                        }
                    })
                    .show();
        } else {
            init();
        }
    }

    private void init() {
        /**
         * request permission if needed
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                /**
                 * show explanation if user rejected our request ago.
                 */
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    new AlertDialog.Builder(this).setMessage(R.string.external_storage_permission_rationable)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                                }
                            }).show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            } else {
                startWorking();
            }
        } else {
            startWorking();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWorking();
        } else {
            new AlertDialog.Builder(this).setMessage(R.string.external_storage_permission_rationable)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    }
                }).show();
        }
    }

    private void startWorking() {
        /**
         * load data from sharedPreferences, the boolean values are stored in the form of bits in integer
         */
        observerInt[0] = sharedPreferences.getInt(Constants.SP_NAME, Integer.MAX_VALUE);
        LogUtil.d(TAG, "startWorking: " + observerInt[0]);
        for (int i = 0; i < isObserving.length; i++) {
            isObserving[i] = (observerInt[0] & (1 << i)) > 0;
        }

        /**
         * initialize UI
         */
        LinearLayout layout = (LinearLayout) findViewById(R.id.activity_main);
        for (int i = 0; i < Constants.DIR_NAMES.length; i++) {
            Switch mySwitch = new Switch(this);
            mySwitch.setText(Constants.DIR_NAMES[i]);
            mySwitch.setChecked(isObserving[i]);
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

        /**
         * try to start service
         */
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
            for (int i = 0; i < isObserving.length; i++) {
                mService.setEnabled(i, isObserving[i]);
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
