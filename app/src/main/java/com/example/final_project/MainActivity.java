package com.example.final_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class MainActivity extends AppCompatActivity {

    MaterialButton main_BTN_start_service;
    MaterialButton main_BTN_stop_service;
    MaterialButton main_BTN_stop_alert;
    MaterialTextView main_LBL_status;
    public static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    public static final String POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS;
    public static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final String ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String ACCESS_BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
    private static final int PERMISSION_REQUEST_CODE = 952;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initViews();

        requestRunTimePermissions();
    }

    private void findViews() {
        main_BTN_start_service = findViewById(R.id.main_BTN_start_service);
        main_BTN_stop_service = findViewById(R.id.main_BTN_stop_service);
        main_BTN_stop_alert = findViewById(R.id.main_BTN_stop_alert);

        main_LBL_status = findViewById(R.id.main_LBL_status);
    }

    private void initViews() {
        main_BTN_start_service.setOnClickListener(v -> startService());
        main_BTN_stop_service.setOnClickListener(v -> stopService());
        main_BTN_stop_alert.setOnClickListener(v -> stopAlert());
    }

    private void stopAlert() {
        sendActionToService(RecordService.STOP_ALERT);
    }

    private void startService() {
        String permission = checkPermission();
//        if (isApproved) {
        if (isLocationEnabled() && permission == null) {
            main_LBL_status.setText("Service ON");
            sendActionToService(RecordService.START_FOREGROUND_SERVICE);
        } else {
            requestRunTimePermissions();
        }
    }

    private void stopService() {
        main_LBL_status.setText("Service OFF");
        sendActionToService(RecordService.STOP_FOREGROUND_SERVICE);
    }

    private void sendActionToService(String action) {
        Intent intent = new Intent(this, RecordService.class);
        intent.setAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            // or
            //ContextCompat.startForegroundService(this, startIntent);
        } else {
            startService(intent);
        }
    }

    private String checkPermission(){
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return RECORD_AUDIO;
        else if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return POST_NOTIFICATIONS;
        else if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return ACCESS_COARSE_LOCATION;
        else if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return ACCESS_FINE_LOCATION;
        else if (ContextCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return ACCESS_BACKGROUND_LOCATION;

//        isApproved = true;
        return null;
    }

    private void requestRunTimePermissions() {
        String permission = checkPermission();
        if (!isLocationEnabled())
            Toast.makeText(this, "Location Disabled", Toast.LENGTH_LONG).show();
        else if (permission == null) {
            Log.d("TAG", "requestRunTimePermissions: granted");
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(permission + " Permission required to use this app")
                    .setTitle(permission + " Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, PERMISSION_REQUEST_CODE);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            builder.show();
        } else {
            Log.d("TAG", "requestRunTimePermissions: not granted");
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("TAG", "onRequestPermissionsResult: " + permissions[0]);
        if (requestCode == PERMISSION_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
                requestRunTimePermissions();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Please allow " + permissions[0] + " permission from settings menu")
                        .setTitle(permissions[0] + " Permission Required")
                        .setCancelable(false)
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);

                                dialog.dismiss();
                            }
                        });

                builder.show();
            } else {
                requestRunTimePermissions();
            }
        }
    }


    private boolean isLocationEnabled() {
        boolean isEnabled;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            isEnabled = lm.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            isEnabled =  mode != Settings.Secure.LOCATION_MODE_OFF;
        }

        if (!isEnabled) {
            showLocationDisabledDialog();
        }

        return isEnabled;
    }

    private void showLocationDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please enable Location Services ")
                .setTitle("Location Services Disabled")
                .setCancelable(false)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);

                        dialog.dismiss();
                    }
                });

        builder.show();
    }
}