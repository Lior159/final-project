
# Final Project

This project involves a Node.js server and an Android application that integrates with Firebase services, including Firebase Realtime Database, Firebase Cloud Messaging, and Firebase Cloud Storage. The project allows you to track current location, trigger alerts, and record audio, with recordings stored in Firebase Cloud Storage.

## Prerequisites

### Node.js Server Requirements
- **Node.js and npm installed:** Ensure that Node.js and npm are installed on your machine.
- **Install dependencies:** In the project folder, run the following command in the terminal to install the necessary packages:

```
npm install
```
- **Firebase Realtime Database:** Set up Firebase Realtime Database for your project.
- **Admin SDK JSON File:** Download the *firebase-admin-sdk* JSON file from your Firebase project and store it in the server folder.
- **Environment Variables:**
    - In the **.env** file, add the following variables:
        - **FIREBASE_JSON_FILE_NAME**: Set this to the name of the admin SDK JSON file (e.g., final-project-2656-firebase-adminsdk-sss444aaa.json).
        - **DB_URL**: Set this to your Firebase Realtime Database URL.

### Running the Node.js Server
- Start the server by running:
```
npm start
```
- Open a browser and navigate to `http://localhost:3000/`
- You can perform the following actions:
    - **Show current location on the map**
    - **Trigger/Stop an alert**
    - **Start/Stop audio recording**
    The recorded file will be stored in Firebase Cloud Storage after recording stops. Note that currently, each new recording will overwrite the previous file.   
- **Firebase Configuration:** 
    - Set the Firebase Realtime Database URL in the ***FireBaseUtil*** class by updating the ***FIREBASE_URL*** constant.
    - Set up Firebase Cloud Storage and Firebase Cloud Messaging in your Firebase project.
    - Download the ***google-services.json*** file from Firebase and move it to your Android project.
- **Running the Android App:**
    - after runing the app you can start or stop the service, and stop alert that was triger by the browser.
    - The service will continue running even if the app is closed (it will run in the foreground). However, you will only be able to stop the alert from the browser after triggering it there, not from the Android app.
