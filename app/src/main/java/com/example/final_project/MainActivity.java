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
    ActivityResultLauncher<Intent> appSettingsResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                checkRecordStatus();
            });
    ActivityResultLauncher<String> recordPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestPermission(), result -> {
                        if (result) {
                            // location access granted.
                            checkRecordStatus();
                        } else {
                            // No location access granted.
                            String permission = checkRecordPermissionStatus(this);
                            if (permission != null && ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                                buildAlertMessageManuallyPermission(permission);
                            } else {
                                buildAlertMessageManuallyPermission(
                                        checkRecordPermissionStatus(this)
                                );
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
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
        main_LBL_status.setText("Service ON");
        sendActionToService(RecordService.START_FOREGROUND_SERVICE);
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

    private void checkRecordStatus() {
        String permissionStatus = checkRecordPermissionStatus(this);
        if (permissionStatus != null)
            askForRecordPermissions(checkRecordPermissionStatus(this));
    }

    private String checkRecordPermissionStatus(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.FOREGROUND_SERVICE_MICROPHONE;
        return null;
    }

    private void askForRecordPermissions(String permission) {
        if (permission.equals(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            buildAlertMessageManuallyPermission(permission);
        else
            recordPermissionRequest.launch(permission);
    }

    private void buildAlertMessageManuallyPermission(String permission) {
        if (permission == null) return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String allow_message_type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "Allow all the time" : "Allow";

        builder.setMessage("You need to enable location permission manually." +
                        "\nOn the page that opens - click on PERMISSIONS, then on LOCATION and then check '" + allow_message_type + "'")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
                .setNegativeButton("Exit", (dialog, which) -> finish());
        builder.create().show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appSettingsResultLauncher.launch(intent);
    }
}