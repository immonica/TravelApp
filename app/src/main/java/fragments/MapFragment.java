package fragments;

import static android.content.ContentValues.TAG;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference databaseRef;
    private Trip trip;
    private List<Museum> museums = new ArrayList<>();
    private boolean museumSuggestionsFetchedAndSaved = false;


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

        // Set OnClickListener for list_icon
        ImageView listIcon = view.findViewById(R.id.list_icon);
        listIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replace the current fragment with PlannerFragment
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                PlannerFragment plannerFragment = new PlannerFragment();
                fragmentTransaction.replace(R.id.fragmentContainer, plannerFragment);
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
                        trip = tripSnapshot.getValue(Trip.class);
                        if (trip != null) {
                            // Set the key of the trip
                            trip.setKey(tripSnapshot.getKey());

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

                if (!museumSuggestionsFetchedAndSaved) {
                    // Fetch museum suggestions and save them to Firebase
                    fetchAndSaveMuseumSuggestions(city, trip.getKey());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inside your MapFragment class

    // Add this method below your other methods in the class
    private void fetchAndSaveMuseumSuggestions(String city, String tripKey) {
        // Use Places API to fetch museum suggestions for the city
        String apiKey = getString(R.string.my_map_api_key);
        Places.initialize(requireContext(), apiKey);
        PlacesClient placesClient = Places.createClient(requireContext());

        // Define the text query to search for museums in the city
        String query = "museum in " + city;

        // Create a request to fetch museum suggestions
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setQuery(query)
                .build();

        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Perform the search asynchronously
        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((response) -> {
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    Log.d(TAG, "Number of museum predictions: " + predictions.size()); // Log the number of predictions
                    int count = 0;
                    for (AutocompletePrediction prediction : predictions) {
                        if (count >= 5) break; // Save up to 5 museum suggestions
                        String museumName = prediction.getPrimaryText(null).toString();
                        String museumAddress = prediction.getFullText(null).toString();
                        Log.d(TAG, "Museum name: " + museumName); // Log each museum name
                        Log.d(TAG, "Museum address: " + museumAddress); // Log each museum address

                        // Perform geocoding for the museum's address to obtain coordinates
                        Geocoder geocoder = new Geocoder(requireContext());
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(museumAddress, 1);
                            if (!addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                double latitude = address.getLatitude();
                                double longitude = address.getLongitude();
                                // Create a new Museum instance
                                Museum museum = new Museum(museumName, city, museumAddress, latitude, longitude);
                                saveMuseumToFirebase(museum, tripKey);
                            } else {
                                Log.e(TAG, "Geocoding failed for museum: " + museumName);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        count++;
                    }
                    // Call displayMuseumMarkers after fetching and saving museum suggestions
                    displayMuseumMarkers(tripKey);
                })
                .addOnFailureListener((exception) -> {
                    Log.e(TAG, "Error fetching museum suggestions: " + exception.getMessage());
                });

        museumSuggestionsFetchedAndSaved = true;
    }


    // Inside your MapFragment class

    // Add this method below your other methods in the class
    private void saveMuseumToFirebase(Museum museum, String tripKey) {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get a reference to the Firebase database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        // Create a new node for the museum suggestion under the trip
        DatabaseReference museumRef = databaseRef.child("users").child(uid).child("trips").child(tripKey).child("museums").push();

        // Set the Museum object as the value for the database reference
        museumRef.setValue(museum)
                .addOnSuccessListener((aVoid) -> {
                    Log.d(TAG, "Museum suggestion saved to Firebase: " + museum.getName());
                })
                .addOnFailureListener((e) -> {
                    Log.e(TAG, "Error saving museum suggestion to Firebase: " + e.getMessage());
                });
    }

    private void displayMuseumMarkers(String tripKey) {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get a reference to the Firebase database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        // Get a reference to the museums node under the trip
        DatabaseReference museumsRef = databaseRef.child("users").child(uid).child("trips").child(tripKey).child("museums");


        // Attach a ValueEventListener to retrieve the museums
        museumsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Iterate through each museum
                for (DataSnapshot museumSnapshot : dataSnapshot.getChildren()) {
                    // Create a Museum object from the snapshot
                    Museum museum = museumSnapshot.getValue(Museum.class);
                    if (museum != null) {
                        // Get museum details
                        String museumName = museum.getName();
                        String museumAddress = museum.getAddress();
                        double latitude = museum.getLatitude();
                        double longitude = museum.getLongitude();

                        // Add a marker on the map for the museum
                        LatLng museumLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(museumLocation).title(museumName));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
                Log.e(TAG, "Error fetching museums from Firebase: " + databaseError.getMessage());
            }
        });
    }

}