# Explore Mate

# Utilization Instructions
This project is a travel planning mobile application developed using Android Studio, Firebase for authentication, and a real-time database.

# Requirements
- **Android Studio**: Ensure the latest version is installed.
- **Firebase Project**: Set up your own Firebase project.
- **Google Places API**: Enable the Places API and get an API key.
- **Android Device or Emulator**: Use either an Android emulator or a physical device.

# Setup and Installation
Clone the Repository:
```bash
git clone https://github.com/immonica/TravelApp.git
cd TravelApp
```

# Open the Project in Android Studio:
- Launch Android Studio.
- Select File > Open and navigate to the cloned repository directory.
- Click OK to open the project.

# Configure Firebase:
- Create a new project in the Firebase Console.
- Add an Android app to your Firebase project and download the google-services.json file.
- Place the google-services.json file in the app directory.

# Enable Firebase Services:
Enable Email/Password Authentication and Realtime Database in your Firebase Console.

# Configure Google Places API:
- Enable the Places API in the Google Cloud Console.
- Generate an API key and restrict its usage to your app's package name.
- Add the API key to your AndroidManifest.xml file:
```bash
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE"/>
```

# Sync Project with Gradle Files:
In Android Studio, click on Sync Now when prompted to sync your project with the Gradle files.

# Running the Application
# Using an Emulator:
- Open AVD Manager in Android Studio and create a new virtual device or select an existing one.
- Click the Play button to launch the emulator.

# Using a Physical Device:
- Connect your Android device to your computer via USB.
- Ensure USB debugging is enabled on your device.
- In Android Studio, select your device from the target device dropdown menu.

# Run the App:
- Click the Run button (green play arrow) in Android Studio.
- Select the emulator or your connected device as the deployment target.
- The app should build and launch on the selected device.

# Using the App

# Sign Up / Log In:
- Sign up with an email and password.
- Log in with the created credentials.

# Trip Planning:
- Create new trips, add destinations, and plan your itinerary.
- The app syncs data in real-time using Firebase.
