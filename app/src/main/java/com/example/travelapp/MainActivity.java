
package com.example.travelapp;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.model.LatLng;

import fragments.HomeFragment;
import fragments.PopUpFragment;
import fragments.SplashFragment;
import fragments.LoginFragment;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for Google Play services availability
        if (isServicesOK()) {
            // Google Play services is available, proceed with fragment transactions

            // Show SplashFragment initially
            replaceFragment(new SplashFragment());

            // Delay for 2 seconds and then replace SplashFragment with HomeFragment
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    replaceFragment(new LoginFragment());
                }
            }, 2000);
        } else {
            // Google Play services is not available, handle accordingly (e.g., show error message)
            // You may choose to finish the activity or display an error message
            Toast.makeText(this, "Google Play services are not available", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    // Method to handle navigation from LoginFragment to HomeFragment
    public void navigateToHomeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new HomeFragment())
                .commit();
    }


    // Method to handle navigation from HomeFragment to PopUpFragment
    public void navigateToPopUpFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new PopUpFragment())
                .addToBackStack(null)
                .commit();
    }




}
