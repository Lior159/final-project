package com.example.final_project;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class RecordService extends Service {

    public static final String START_FOREGROUND_SERVICE = "START_FOREGROUND_SERVICE";
    public static final String STOP_FOREGROUND_SERVICE = "STOP_FOREGROUND_SERVICE";
    public static final String START_ALERT = "START_ALERT";
    public static final String STOP_ALERT = "STOP_ALERT";
    public static final String START_RECORDING = "START_RECORDING";
    public static final String STOP_RECORDING = "STOP_RECORDING";
    public static final String GET_LOCATION = "GET_LOCATION";
    private static final int NOTIFICATION_ID = 195;
    public static final int FOREGROUND_SERVICE_TYPE_MICROPHONE = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
    public static final int FOREGROUND_SERVICE_TYPE_LOCATION = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
    private static final int MICROPHONE_NOTIFICATION_ID = 213;
    private static final int LOCATION_NOTIFICATION_ID = 211;
    public static String MICROPHONE_CHANNEL_ID = "com.example.fina_project.MICROPHONE_CHANNEL_ID";
    public static String LOCATION_CHANNEL_ID = "com.example.fina_project.LOCATION_CHANNEL_ID";
    private int lastShownNotificationId = -1;
    public static String CHANNEL_ID = "com.example.fina_project.CHANNEL_ID_FOREGROUND";
    public static String MAIN_ACTION = "com.example.fina_project.recordService.action.main";
    private NotificationCompat.Builder notificationBuilder;
    private MediaRecorder recorder = null;
    private MediaPlayer mediaPlayer = null;
    String audioFilePath = null;
    private boolean isServiceRunning = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis() + ".3gp";

        audioFilePath = getFilesDir().getAbsolutePath() + "/audio_file.3gp";
        File file = new File(audioFilePath);

        if (intent == null || intent.getAction() == null || !checkPermission() || !isLocationEnabled()) {
            return START_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case START_FOREGROUND_SERVICE:
                if (isServiceRunning) {
                    return START_STICKY;
                }
                notifyToUserForForegroundService(FOREGROUND_SERVICE_TYPE_MICROPHONE, MICROPHONE_NOTIFICATION_ID, MICROPHONE_CHANNEL_ID);
                isServiceRunning = true;
                Log.d("Service Status", "ON");
                break;
            case STOP_FOREGROUND_SERVICE:
                if (!isServiceRunning) {
                    return START_STICKY;
                }
                isServiceRunning = false;
                Log.d("Service Status", "OFF");
                stopSelf();
                break;
            case START_RECORDING:
                notifyToUserForForegroundService(FOREGROUND_SERVICE_TYPE_MICROPHONE, MICROPHONE_NOTIFICATION_ID, MICROPHONE_CHANNEL_ID);
                if (!isServiceRunning) {
                    return START_STICKY;
                }
                startRecording();
                break;
            case STOP_RECORDING:
                if (!isServiceRunning) {
                    return START_STICKY;
                }
                stopRecording();
                break;
            case START_ALERT:
                if (!isServiceRunning) {
                    return START_STICKY;
                }
                startAlert();
                break;
            case STOP_ALERT:
                if (!isServiceRunning) {
                    return START_STICKY;
                }
                stopAlert();
                break;
            case GET_LOCATION:
                notifyToUserForForegroundService(FOREGROUND_SERVICE_TYPE_LOCATION, LOCATION_NOTIFICATION_ID, LOCATION_CHANNEL_ID);
                if (!isServiceRunning) {
                    Log.d("Service Status", "OFF");
                    return START_STICKY;
                } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("Location Status", "No permission");
                    return START_STICKY;
                } else {
                    Log.d("Location Status", "Permission granted");
                    FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        Log.d("Location Status", "[" + location.getLatitude() + ", " + location.getLongitude());
                                      FireBaseUtil.saveLocationToDatabase(location.getLatitude(), location.getLongitude());
                                    }
                                }
                            });
                }
                break;
        }

        return START_STICKY;
    }

    private void startAlert() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound); // Replace with your audio file name
            mediaPlayer.setLooping(true); // Set looping if you want the alert to repeat
        }
        mediaPlayer.start();
        Log.d("RecordService", "Alert started");
    }

    // Method to stop playing alert sound
    private void stopAlert() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d("RecordService", "Alert stopped");
        }
    }

    private void startRecording() {
        Log.d("Record Action", "Recording Started");
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
        }
    }

    private void stopRecording() {
        Log.d("Record Action", "Recording Stopped");
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            FireBaseUtil.uploadAudioToFirebase(this, audioFilePath);
        }
    }

    private void notifyToUserForForegroundService(int foregroundServiceType, int notificationId, String chanelId) {
        // On notification click
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder = getNotificationBuilder(this,
                chanelId,
                NotificationManagerCompat.IMPORTANCE_LOW); //Low importance prevent visual appearance for this notification channel on top

        notificationBuilder
                .setContentIntent(pendingIntent) // Open activity
                .setOngoing(true)
//                .setSmallIcon(R.drawable.ic_cycling)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
                .setContentTitle("App in progress")
                .setContentText("Content")
        ;

        Notification notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, foregroundServiceType);
        }

        if (notificationId != lastShownNotificationId) {
            // Cancel previous notification
            final NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            notificationManager.cancel(lastShownNotificationId);
        }
        lastShownNotificationId = notificationId;
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context, String channelId, int importance) {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, channelId, importance);
            builder = new NotificationCompat.Builder(context, channelId);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    @TargetApi(26)
    private static void prepareChannel(Context context, String id, int importance) {
        final String appName = context.getString(R.string.app_name);
        String notifications_channel_description = "recordings channel";
        final NotificationManager nm = (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);

        if (nm != null) {
            NotificationChannel nChannel = nm.getNotificationChannel(id);

            if (nChannel == null) {
                nChannel = new NotificationChannel(id, appName, importance);
                nChannel.setDescription(notifications_channel_description);

                // from another answer
                nm.createNotificationChannel(nChannel);
            }
        }
    }

    private void updateNotification(String content) {
        notificationBuilder.setContentText(content);
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private boolean checkPermission(){
        if (ContextCompat.checkSelfPermission(this, MainActivity.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return false;
        else if (ContextCompat.checkSelfPermission(this, MainActivity.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return false;
        else if (ContextCompat.checkSelfPermission(this, MainActivity.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return false;
        else if (ContextCompat.checkSelfPermission(this, MainActivity.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return false;
        else if (ContextCompat.checkSelfPermission(this, MainActivity.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return false;

//        isApproved = true;
        return true;
    }

    private boolean isLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            return mode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
