package com.example.travelapp;

import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import fragments.HomeFragment;
import fragments.SplashFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show SplashFragment initially
        replaceFragment(new SplashFragment());

        // Delay for 2 seconds and then replace SplashFragment with HomeFragment
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                replaceFragment(new HomeFragment());
            }
        }, 2000);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

}
