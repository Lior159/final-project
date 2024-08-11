package com.example.final_project;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMNotificationService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("NEW_TOKEN",token);

        FireBaseUtil.saveTokenToDatabase(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String action = remoteMessage.getData().get("action");

        Log.d("MESSAGE",remoteMessage.getData().toString());


        if (RecordService.START_RECORDING.equalsIgnoreCase(action)) {
            sendActionToService(RecordService.START_RECORDING);
        } else if (RecordService.STOP_RECORDING.equalsIgnoreCase(action)) {
            sendActionToService(RecordService.STOP_RECORDING);
        }
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
}
