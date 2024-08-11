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

    MaterialButton main_BTN_start_record;
    MaterialButton main_BTN_stop_record;
    MaterialButton main_BTN_start_audio;
    MaterialButton main_BTN_stop_audio;
    MaterialTextView main_LBL_status;
    String audioFilePath = null;
    boolean isRecording = false;
    boolean isPlaying = false;
    MediaPlayer player = null;
    MediaRecorder recorder = null;
    private FirebaseStorage storage;
    private StorageReference storageRef;

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

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        audioFilePath = getFilesDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".3gp";
    }

    private void findViews() {
        main_BTN_start_record = findViewById(R.id.main_BTN_start_record);
        main_BTN_stop_record = findViewById(R.id.main_BTN_stop_record);
        main_BTN_start_audio = findViewById(R.id.main_BTN_start_audio);
        main_BTN_stop_audio = findViewById(R.id.main_BTN_stop_audio);
        main_LBL_status = findViewById(R.id.main_LBL_status);
    }

    private void initViews() {
        main_BTN_start_record.setOnClickListener(v -> startService());
        main_BTN_stop_record.setOnClickListener(v -> stopService());
        main_BTN_start_audio.setOnClickListener(v -> startAudio());
        main_BTN_stop_audio.setOnClickListener(v -> stopAudio());
    }

    private void startService() {
        sendActionToService(RecordService.START_FOREGROUND_SERVICE);
    }

    private void stopService() {
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

    private void uploadAudioToFirebase() {
        Uri fileUri = Uri.fromFile(new File(audioFilePath));
        StorageReference audioRef = storageRef.child("audios/" + fileUri.getLastPathSegment());
        UploadTask uploadTask = audioRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(this, "Upload successful!", Toast.LENGTH_SHORT).show();
            audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                // Handle the download URL
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void stopAudio() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.release();
            player = null;
            main_LBL_status.setText("Audio stopped");
        }
    }

    private void startAudio() {
        if (player == null) {
            player = new MediaPlayer();


            try {
                player.setDataSource(audioFilePath);
                player.prepare();
                player.start();
            } catch (IOException e) {
                Log.e("Audio", "prepare() failed");
            }

            main_LBL_status.setText("Audio started");
        }
    }

    private void stopRecord() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            FireBaseUtil.uploadAudioToFirebase(this, audioFilePath);

            main_LBL_status.setText("Recording stopped");
        }
    }

    private void startRecord() {
        if (recorder == null) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(audioFilePath);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e("Audio", "prepare() failed");
            }

            recorder.start();

            main_LBL_status.setText("Recording started");
        }
    }

    private void checkRecordStatus() {
        String permissionStatus = checkRecordPermissionStatus(this);
        if (permissionStatus != null)
            askForRecordPermissions(checkRecordPermissionStatus(this));
    }

    private String checkRecordPermissionStatus(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.RECORD_AUDIO;
        return null;
    }

    private void askForRecordPermissions(String permission) {
        if (permission.equals(Manifest.permission.RECORD_AUDIO) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
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

//    @Override
//    protected void onStop() {
//        super.onStop();
//        stopAudio();
//        stopRecord();
//    }

//    private void getFCMToken() {
//        // Get updated InstanceID token.
//        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
////                FireBaseUtil.saveTokenToDatabase(task.getResult());
//                Log.i("Firebase Messaging Token", task.getResult());
//            }
//        });
//    }
}