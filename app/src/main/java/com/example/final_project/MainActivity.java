package com.example.final_project;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    MaterialButton main_BTN_start_service;
    MaterialButton main_BTN_stop_service;
    MaterialButton main_BTN_stop_alert;
    MaterialTextView main_LBL_status;
    private boolean isAproved = false;
    private static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final String POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS;
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
        if (isAproved) {
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

    private void requestRunTimePermissions() {
        
        if (ActivityCompat.checkSelfPermission(this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "requestRunTimePermissions: granted");
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
            isAproved = true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Permission to access the microphone is required to use this app")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isAproved = true;
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Please allow Microphone permission from settings menu")
                        .setTitle("Permission Required")
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

    private String checkPermission(){
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return RECORD_AUDIO;
        else if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return POST_NOTIFICATIONS;
        }
        return null;
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

//    private void checkRecordStatus() {
//        String permissionStatus = checkRecordPermissionStatus(this);
//        if (permissionStatus != null)
//            askForRecordPermissions(checkRecordPermissionStatus(this));
//    }

//    private String checkRecordPermissionStatus(Context context) {
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) != PackageManager.PERMISSION_GRANTED)
//            return Manifest.permission.FOREGROUND_SERVICE_MICROPHONE;
//        return null;
//    }
//
//    private void askForRecordPermissions(String permission) {
//        if (permission.equals(Manifest.permission.RECORD_AUDIO) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
//            buildAlertMessageManuallyPermission(permission);
//        else
//            recordPermissionRequest.launch(permission);
//    }
//
//    private void buildAlertMessageManuallyPermission(String permission) {
//        if (permission == null) return;
//
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        String allow_message_type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "Allow all the time" : "Allow";
//
//        builder.setMessage("You need to enable location permission manually." +
//                        "\nOn the page that opens - click on PERMISSIONS, then on LOCATION and then check '" + allow_message_type + "'")
//                .setCancelable(false)
//                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
//                .setNegativeButton("Exit", (dialog, which) -> finish());
//        builder.create().show();
//    }
//
//    private void openAppSettings() {
//        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                Uri.fromParts("package", getPackageName(), null));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        appSettingsResultLauncher.launch(intent);
//    }
}