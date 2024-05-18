package fragments;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.travelapp.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize Firebase database reference
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map_container, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // Set OnClickListener for back_icon
        ImageView backIcon = view.findViewById(R.id.back_icon);
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replace the current fragment with HomeFragment
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                HomeFragment homeFragment = new HomeFragment();
                fragmentTransaction.replace(R.id.fragmentContainer, homeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Retrieve the last saved trip from Firebase
        getLastSavedTrip();
    }

    private void getLastSavedTrip() {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Query to get the last trip under the user's UID
        Query lastTripQuery = databaseRef.child("users").child(uid).child("trips").orderByKey().limitToLast(1);

        // Add a ValueEventListener to retrieve the last trip
        lastTripQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if there are any trips
                if (dataSnapshot.exists()) {
                    // Retrieve the last trip
                    for (DataSnapshot tripSnapshot : dataSnapshot.getChildren()) {
                        Trip trip = tripSnapshot.getValue(Trip.class);
                        if (trip != null) {
                            // Extract the city from the trip data
                            String city = trip.getCity();

                            // Perform geocoding to obtain coordinates of the city
                            geocodeCity(city);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
            }
        });
    }


    private void geocodeCity(String city) {
        // Perform geocoding to obtain coordinates of the city
        Geocoder geocoder = new Geocoder(requireContext());
        try {
            List<Address> addresses = geocoder.getFromLocationName(city, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng cityLocation = new LatLng(address.getLatitude(), address.getLongitude());

                // Move the camera of the map to the city's location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLocation, 12f));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
