package com.example.final_project;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class FireBaseUtil {

    public static void saveTokenToDatabase(String token) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://final-project-23698-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference myRef = database.getReference("tokens");

        myRef.child("token").setValue(token)
                .addOnSuccessListener(aVoid -> Log.d("Token", "Token saved successfully: " + token))
                .addOnFailureListener(e -> Log.e("Token Error", "Error saving token to database: " + e.getMessage()));
    }

    public static void uploadAudioToFirebase(Context context, String audioFilePath) {
        Uri fileUri = Uri.fromFile(new File(audioFilePath));
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = fileUri.getLastPathSegment();
        StorageReference audioRef = storageRef.child("audios/" + fileName);
        UploadTask uploadTask = audioRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(context, "Upload successful!", Toast.LENGTH_SHORT).show();
            audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                // Handle the download URL
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }





}
