package fragments;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference databaseRef;
    private Trip trip;
    private List<Museum> museums = new ArrayList<>();
    private boolean museumSuggestionsFetchedAndSaved = false;
    private boolean attractionSuggestionsFetchedAndSaved = false;
    private PlacesClient placesClient;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize Firebase database reference
        databaseRef = FirebaseDatabase.getInstance().getReference();

        Places.initialize(getActivity(), getString(R.string.my_map_api_key));
        placesClient = Places.createClient(requireContext());

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

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Object tag = marker.getTag();
                if (tag != null) {
                    // Obtain placeId from marker tag
                    String placeId = tag.toString();

                    // Use placeId to fetch place details
                    fetchPlaceDetails(placeId);
                } else {
                    Log.e(TAG, "Marker tag is null");
                }
                return true;
            }
        });


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
                if (!attractionSuggestionsFetchedAndSaved) {
                    fetchAndSaveTouristAttractionSuggestions(city, trip.getKey());
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

                                // Get the placeId from prediction
                                String placeId = prediction.getPlaceId();

                                // Create a new Museum instance with placeId
                                Museum museum = new Museum(museumName, city, museumAddress, latitude, longitude, placeId);
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

    private void fetchAndSaveTouristAttractionSuggestions(String city, String tripKey) {
        // Use Places API to fetch tourist attraction suggestions for the city
        String apiKey = getString(R.string.my_map_api_key);
        Places.initialize(requireContext(), apiKey);
        PlacesClient placesClient = Places.createClient(requireContext());

        // Define the text query to search for tourist attractions in the city
        String query = "tourist attractions in " + city;

        // Create a request to fetch tourist attraction suggestions
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
                    Log.d(TAG, "Number of tourist attraction predictions: " + predictions.size()); // Log the number of predictions
                    int count = 0;
                    for (AutocompletePrediction prediction : predictions) {
                        if (count >= 5) break; // Save up to 5 tourist attraction suggestions
                        String attractionName = prediction.getPrimaryText(null).toString();
                        String attractionAddress = prediction.getFullText(null).toString();
                        Log.d(TAG, "Attraction name: " + attractionName); // Log each attraction name
                        Log.d(TAG, "Attraction address: " + attractionAddress); // Log each attraction address

                        // Perform geocoding for the attraction's address to obtain coordinates
                        Geocoder geocoder = new Geocoder(requireContext());
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(attractionAddress, 1);
                            if (!addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                double latitude = address.getLatitude();
                                double longitude = address.getLongitude();

                                // Get the placeId from prediction
                                String placeId = prediction.getPlaceId();

                                // Create a new TouristAttraction instance with placeId
                                TouristAttraction attraction = new TouristAttraction(attractionName, city, attractionAddress, latitude, longitude, placeId);
                                saveAttractionToFirebase(attraction, tripKey);
                            } else {
                                Log.e(TAG, "Geocoding failed for tourist attraction: " + attractionName);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        count++;
                    }

                    // Call displayAttractionMarkers after fetching and saving tourist attraction suggestions
                    displayAttractionMarkers(tripKey);
                })
                .addOnFailureListener((exception) -> {
                    Log.e(TAG, "Error fetching tourist attraction suggestions: " + exception.getMessage());
                });

        attractionSuggestionsFetchedAndSaved = true;
    }


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

    private void saveAttractionToFirebase(TouristAttraction attraction, String tripKey) {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get a reference to the Firebase database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        // Create a new node for the museum suggestion under the trip
        DatabaseReference attractionRef = databaseRef.child("users").child(uid).child("trips").child(tripKey).child("attraction").push();

        // Set the Museum object as the value for the database reference
        attractionRef.setValue(attraction)
                .addOnSuccessListener((aVoid) -> {
                    Log.d(TAG, "Atraction suggestion saved to Firebase: " + attraction.getName());
                })
                .addOnFailureListener((e) -> {
                    Log.e(TAG, "Error saving attraction suggestion to Firebase: " + e.getMessage());
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
                        Marker marker = mMap.addMarker(new MarkerOptions().position(museumLocation).title(museumName));

                        // Set the marker tag as the placeId associated with the museum
                        marker.setTag(museum.getPlaceId());
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

    private void displayAttractionMarkers(String tripKey) {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get a reference to the Firebase database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        // Get a reference to the museums node under the trip
        DatabaseReference attractionRef = databaseRef.child("users").child(uid).child("trips").child(tripKey).child("attraction");


        // Attach a ValueEventListener to retrieve the museums
        attractionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Iterate through each museum
                for (DataSnapshot attractionSnapshot : dataSnapshot.getChildren()) {
                    // Create a TouristAttraction object from the snapshot
                    TouristAttraction attraction = attractionSnapshot.getValue(TouristAttraction.class);
                    if (attraction != null) {
                        // Get attraction details
                        String attractionName = attraction.getName();
                        String attractionAddress = attraction.getAddress();
                        double latitude = attraction.getLatitude();
                        double longitude = attraction.getLongitude();

                        // Add a marker on the map for the attraction
                        LatLng attractionLocation = new LatLng(latitude, longitude);
                        Marker marker = mMap.addMarker(new MarkerOptions().position(attractionLocation).title(attractionName));

                        // Set the marker tag as the placeId associated with the attraction
                        marker.setTag(attraction.getPlaceId());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
                Log.e(TAG, "Error fetching attraction from Firebase: " + databaseError.getMessage());
            }
        });
    }

    private void fetchPlaceDetails(String placeId) {
        // Define fields you want to retrieve
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.PHONE_NUMBER, Place.Field.WEBSITE_URI, Place.Field.LAT_LNG);

        // Construct a FetchPlaceRequest
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        // Fetch place details asynchronously
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            // Handle fetched place details
            displayPlaceDetails(place);
        }).addOnFailureListener((exception) -> {
            // Handle fetch failure
            Log.e(TAG, "Place not found: " + exception.getMessage());
        });
    }

    private void displayPlaceDetails(Place place) {
        // Get the place details
        String name = place.getName();
        String address = place.getAddress();
        String phoneNumber = place.getPhoneNumber();
        Uri websiteUri = place.getWebsiteUri();
        LatLng latLng = place.getLatLng();

        // Construct the information string
        StringBuilder info = new StringBuilder();
        info.append("Name: ").append(name).append("\n");
        info.append("Address: ").append(address).append("\n");
        info.append("Phone: ").append(phoneNumber).append("\n");

        if (websiteUri != null) {
            info.append("Website: ").append(websiteUri.toString());
        }

        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.place_details_dialog_trip, null);

        // Set the place details to the TextViews in the custom dialog layout
        TextView placeNameTextView = dialogView.findViewById(R.id.place_name_text_view_trip);
        placeNameTextView.setText(name);

        TextView placeAddressTextView = dialogView.findViewById(R.id.place_address_text_view_trip);
        placeAddressTextView.setText(address);

        TextView placePhoneTextView = dialogView.findViewById(R.id.place_phone_text_view_trip);
        placePhoneTextView.setText(phoneNumber);

        TextView placeWebsiteTextView = dialogView.findViewById(R.id.place_website_text_view_trip);
        if (websiteUri != null) {
            placeWebsiteTextView.setText(websiteUri.toString());
        } else {
            placeWebsiteTextView.setVisibility(View.GONE);
        }

        // Fetch place photo
        fetchPlacePhoto(place, dialogView);

        // Show the custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView)
                .setTitle("Place Details")
                .setPositiveButton("OK", null)
                .show();

        // Handle the Save to Itinerary button click
        Button saveToItineraryButton = dialogView.findViewById(R.id.save_to_itinerary_button);
        saveToItineraryButton.setOnClickListener(v -> showDatePickerDialog(place.getId(), name, address, trip.getKey()));
    }

    private void fetchPlacePhoto(Place place, View dialogView) {
        // Define the fields to be returned for the photo
        List<Place.Field> fields = Arrays.asList(Place.Field.PHOTO_METADATAS);

        // Construct a FetchPlaceRequest
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(place.getId(), fields);

        // Fetch place details asynchronously
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place fetchedPlace = response.getPlace();
            // Get the photo metadata
            List<PhotoMetadata> photoMetadataList = fetchedPlace.getPhotoMetadatas();
            if (photoMetadataList != null && !photoMetadataList.isEmpty()) {
                // Get the first photo metadata
                PhotoMetadata photoMetadata = photoMetadataList.get(0);

                // Create a FetchPhotoRequest
                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxHeight(1600) // Set maximum height of the photo
                        .setMaxWidth(1600) // Set maximum width of the photo
                        .build();

                // Fetch the photo asynchronously
                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                    // Display the photo in your ImageView
                    if (bitmap != null) {
                        // Set the fetched photo bitmap to the ImageView in the dialog layout
                        ImageView imageView = dialogView.findViewById(R.id.place_photo_image_view_trip);
                        if (imageView != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE); // Set visibility to VISIBLE
                        }
                    }
                }).addOnFailureListener((exception) -> {
                    // Handle photo fetch failure
                    Log.e(TAG, "Place photo not found: " + exception.getMessage());
                });
            }
        }).addOnFailureListener((exception) -> {
            // Handle fetch failure
            Log.e(TAG, "Place not found: " + exception.getMessage());
        });
    }

    private void showDatePickerDialog(String placeId, String placeName, String placeAddress, String tripKey) {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch the days for the current trip from Firebase
        databaseRef.child("users").child(uid).child("trips").child(tripKey).child("days")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<String> days = new ArrayList<>();
                        for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
                            days.add(daySnapshot.getValue(String.class));
                        }

                        // Convert the list of days to an array
                        String[] daysArray = days.toArray(new String[0]);

                        // Show an AlertDialog with the available days
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle("Select a Day")
                                .setSingleChoiceItems(daysArray, -1, (dialog, which) -> {
                                    // Get the selected day
                                    String selectedDate = daysArray[which];
                                    // Save the place to the selected day in the itinerary
                                    savePlaceToItinerary(uid, tripKey, selectedDate, placeId, placeName, placeAddress);
                                    // Dismiss the dialog
                                    dialog.dismiss();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle database error
                        Log.e(TAG, "Error fetching days from Firebase: " + databaseError.getMessage());
                    }
                });
    }

    private void savePlaceToItinerary(String uid, String tripKey, String selectedDate, String placeId, String placeName, String placeAddress) {
        // Get a reference to the Firebase database
        DatabaseReference itineraryRef = databaseRef.child("users").child(uid).child("trips").child(tripKey).child("itinerary").child(selectedDate).push();

        // Create a map to store place details
        Map<String, String> placeDetails = new HashMap<>();
        placeDetails.put("placeId", placeId);
        placeDetails.put("name", placeName);
        placeDetails.put("address", placeAddress);

        // Save place details to the selected date in the itinerary
        itineraryRef.setValue(placeDetails)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Place saved to itinerary", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving place to itinerary: " + e.getMessage());
                });
    }


}