package fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.travelapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PopUpFragment extends Fragment {

    private static final String TAG = "PopUpFragment";

    private View rootView;
    private TextView emailTextView;
    // Define variables for views and Firebase
    private DatabaseReference tripsRef;
    private ValueEventListener valueEventListener;
    private LinearLayout tripContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_popup, container, false);

        // Button Click Listener
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopUpFragment();
            }
        });

        // Find the logout_button
        Button logoutButton = rootView.findViewById(R.id.logout_button);

        // Set OnClickListener for the logout_button
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the logout method
                logoutUser();
            }
        });

        emailTextView = rootView.findViewById(R.id.email_text);
        setEmailText(); // Set email text when the fragment is created

        // Initialize tripContainer
        tripContainer = rootView.findViewById(R.id.trip_container);

        // Initialize Firebase reference
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tripsRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(user.getUid())
                    .child("trips");
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchTripsData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (tripsRef != null && valueEventListener != null) {
            tripsRef.removeEventListener(valueEventListener);
        }
    }

    private void fetchTripsData() {
        if (tripsRef != null) {
            if (valueEventListener != null) {
                tripsRef.removeEventListener(valueEventListener);
            }

            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    tripContainer.removeAllViews();
                    long totalTrips = dataSnapshot.getChildrenCount();
                    long start = Math.max(0, totalTrips - 5);
                    long index = 0;
                    for (DataSnapshot tripSnapshot : dataSnapshot.getChildren()) {
                        if (index >= start) {
                            Trip trip = tripSnapshot.getValue(Trip.class);
                            if (trip != null) {
                                addTripView(trip);
                            }
                        }
                        index++;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error fetching trips: " + databaseError.getMessage());
                }
            };

            tripsRef.addValueEventListener(valueEventListener);
        }
    }

    private void addTripView(Trip trip) {
        // Create a TextView to display trip information
        TextView tripTextView = new TextView(requireContext());
        tripTextView.setText(trip.getCity() + ": " + trip.getStartDate() + " - " + trip.getEndDate());
        tripTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 10, 0, 0);
        tripTextView.setLayoutParams(layoutParams);

        // Add the TextView to tripContainer
        tripContainer.addView(tripTextView);
    }

    private void closePopUpFragment() {
        // Go back to the previous fragment (HomeFragment)
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void logoutUser() {
        // Build and show a confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked Yes, proceed with logout
                FirebaseAuth.getInstance().signOut();
                // Navigate to LoginFragment after logout
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new LoginFragment())
                        .commit();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked No, dismiss the dialog
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setEmailText() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email != null) {
                emailTextView.setText(email);
            }
        }
    }


}