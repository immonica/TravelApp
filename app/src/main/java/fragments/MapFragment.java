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
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
    private PlacesClient placesClient;
    private int museumsFetchedCount = 0;
    private int parksFetchedCount = 0;
    private int restaurantsFetchedCount = 0;
    private int cafesFetchedCount = 0;
    private int hotelsFetchedCount = 0;
    private int giftShopsFetchedCount = 0;
    private int barsFetchedCount = 0;
    private boolean suggestionsFetched = false;

    // Maintain lists of markers for each place type
    private List<Marker> museumMarkers = new ArrayList<>();
    private List<Marker> parkMarkers = new ArrayList<>();
    private List<Marker> restaurantMarkers = new ArrayList<>();
    private List<Marker> barMarkers = new ArrayList<>();
    private List<Marker> giftShopMarkers = new ArrayList<>();
    private List<Marker> hotelMarkers = new ArrayList<>();
    private List<Marker> cafeMarkers = new ArrayList<>();

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

        // Initialize buttons
        Button buttonMuseum = view.findViewById(R.id.button_museum);
        Button buttonPark = view.findViewById(R.id.button_park);
        Button buttonRestaurant = view.findViewById(R.id.button_restaurant);
        Button buttonCafe = view.findViewById(R.id.button_cafe);
        Button buttonHotel = view.findViewById(R.id.button_hotel);
        Button buttonGiftShop = view.findViewById(R.id.button_gift_shop);
        Button buttonBar = view.findViewById(R.id.button_bar);

        // Set OnClickListener for museum button
        buttonMuseum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle museum markers
                toggleMarkers("museum");
            }
        });

        // Set OnClickListener for park button
        buttonPark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle park markers
                toggleMarkers("park");
            }
        });

        // Set OnClickListener for restaurant button
        buttonRestaurant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle restaurant markers
                toggleMarkers("restaurant");
            }
        });

        // Set OnClickListener for cafe button
        buttonCafe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle cafe markers
                toggleMarkers("cafe");
            }
        });

        // Set OnClickListener for hotel button
        buttonHotel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle hotel markers
                toggleMarkers("hotel");
            }
        });

        // Set OnClickListener for gift shop button
        buttonGiftShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle gift shop markers
                toggleMarkers("gift_shop");
            }
        });

        // Set OnClickListener for bar button
        buttonBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle bar markers
                toggleMarkers("bar");
            }
        });

        // Find the "Clear Filters" button by its ID
        LinearLayout clearFiltersButton = view.findViewById(R.id.clear_filters_button);
        clearFiltersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllFilters();
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

        Log.d(TAG, "onMapReady: Last saved trip retrieved");
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

                if (!suggestionsFetched) {
                    // Fetch suggestions and save them to Firebase
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "museum", "museums");
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "park", "parks");
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "restaurant", "restaurants");
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "gift_shop", "shops");
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "cafe", "cafes");
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "bar", "bars");
                    fetchAndSavePlaceSuggestions(city, trip.getKey(), "hotel", "hotels");
                    suggestionsFetched = true;
                }

            } else {
                Log.e(TAG, "Geocoding failed for city: " + city);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchAndSavePlaceSuggestions(String city, String tripKey, String placeType, String firebaseNode) {
        // Use Places API to fetch place suggestions for the city
        String apiKey = getString(R.string.my_map_api_key);
        Places.initialize(requireContext(), apiKey);
        PlacesClient placesClient = Places.createClient(requireContext());

        // Define the text query to search for the place type in the city
        String query = placeType + " in " + city;

        // Create a request to fetch place suggestions
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
                    Log.d(TAG, "Number of " + placeType + " predictions: " + predictions.size()); // Log the number of predictions
                    int count = 0;
                    for (AutocompletePrediction prediction : predictions) {
                        if (count >= 5) break; // Save up to 5 place suggestions
                        String placeName = prediction.getPrimaryText(null).toString();
                        String placeAddress = prediction.getFullText(null).toString();
                        Log.d(TAG, placeType + " name: " + placeName); // Log each place name
                        Log.d(TAG, placeType + " address: " + placeAddress); // Log each place address

                        // Perform geocoding for the place's address to obtain coordinates
                        Geocoder geocoder = new Geocoder(requireContext());
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(placeAddress, 1);
                            if (!addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                double latitude = address.getLatitude();
                                double longitude = address.getLongitude();

                                // Get the placeId from prediction
                                String placeId = prediction.getPlaceId();

                                // Create a new Place instance with placeId
                                PlaceSuggestion placeSuggestion = new PlaceSuggestion(placeName, city, placeAddress, latitude, longitude, placeId,placeType);
                                savePlaceSuggestionToFirebase(placeSuggestion, tripKey, firebaseNode);

                                // Display marker for the place
                                displayPlaceMarker(placeSuggestion);

                            } else {
                                Log.e(TAG, "Geocoding failed for " + placeType + ": " + placeName);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        count++;
                    }

                    // Update the fetched count based on place type
                    if (placeType.equals("museum")) {
                        museumsFetchedCount++;
                    } else if (placeType.equals("park")) {
                        parksFetchedCount++;
                    }else if (placeType.equals("restaurant")) {
                        restaurantsFetchedCount++;
                    }else if (placeType.equals("cafe")) {
                        cafesFetchedCount++;
                    }else if (placeType.equals("hotel")) {
                        hotelsFetchedCount++;
                    }else if (placeType.equals("gift_shop")) {
                        giftShopsFetchedCount++;
                    }else if (placeType.equals("bar")) {
                        barsFetchedCount++;
                    }

                    // Check if both museums and parks fetch operations are completed
                    if (museumsFetchedCount >= 5 && parksFetchedCount >= 5 && restaurantsFetchedCount >= 5 && cafesFetchedCount >= 5 && hotelsFetchedCount >= 5 && giftShopsFetchedCount >= 5  && barsFetchedCount >= 5) {
                        museumsFetchedCount = 0;
                        parksFetchedCount = 0;
                        restaurantsFetchedCount = 0;
                        cafesFetchedCount = 0;
                        hotelsFetchedCount = 0;
                        giftShopsFetchedCount = 0;
                        barsFetchedCount = 0;
                    }

                })
                .addOnFailureListener((exception) -> {
                    Log.e(TAG, "Error fetching " + placeType + " suggestions: " + exception.getMessage());
                });

    }

    private void savePlaceSuggestionToFirebase(PlaceSuggestion placeSuggestion, String tripKey, String firebaseNode) {
        // Get the current user's UID
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get a reference to the Firebase database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        // Create a new node for the place suggestion under the trip
        DatabaseReference placeRef = databaseRef.child("users").child(uid).child("trips").child(tripKey).child(firebaseNode).push();

        // Set the PlaceSuggestion object as the value for the database reference
        placeRef.setValue(placeSuggestion)
                .addOnSuccessListener((aVoid) -> {
                    Log.d(TAG, "Place suggestion saved to Firebase: " + placeSuggestion.getName());
                })
                .addOnFailureListener((e) -> {
                    Log.e(TAG, "Error saving place suggestion to Firebase: " + e.getMessage());
                });
    }

    // Toggle visibility of markers based on place type
    private void toggleMarkers(String placeType) {
        // Hide all markers first
        hideAllMarkers();

        // Show markers for the selected place type
        switch (placeType) {
            case "museum":
                showMarkers(museumMarkers);
                break;
            case "park":
                showMarkers(parkMarkers);
                break;
            case "restaurant":
                showMarkers(restaurantMarkers);
                break;
            case "cafe":
                showMarkers(cafeMarkers);
                break;
            case "hotel":
                showMarkers(hotelMarkers);
                break;
            case "gift_shop":
                showMarkers(giftShopMarkers);
                break;
            case "bar":
                showMarkers(barMarkers);
                break;
        }
    }

    // Helper method to show markers for a specific place type
    private void showMarkers(List<Marker> markers) {
        for (Marker marker : markers) {
            marker.setVisible(true);
        }
    }

    // Helper method to hide all markers
    private void hideAllMarkers() {
        for (Marker marker : museumMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : parkMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : restaurantMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : cafeMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : barMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : hotelMarkers) {
            marker.setVisible(false);
        }
        for (Marker marker : giftShopMarkers) {
            marker.setVisible(false);
        }
    }

    private void displayPlaceMarker(PlaceSuggestion placeSuggestion) {
        // Get place details
        String placeName = placeSuggestion.getName();
        double latitude = placeSuggestion.getLatitude();
        double longitude = placeSuggestion.getLongitude();
        String placeType = placeSuggestion.getPlaceType();

        // Determine marker icon based on place type
        BitmapDescriptor markerIcon = getMarkerIcon(placeType);

        // Add a marker on the map for the place
        LatLng placeLocation = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(placeLocation)
                .title(placeName)
                .icon(markerIcon);
        Marker marker = mMap.addMarker(markerOptions);

        // Add the marker to the appropriate list based on place type
        switch (placeSuggestion.getPlaceType()) {
            case "museum":
                museumMarkers.add(marker);
                break;
            case "park":
                parkMarkers.add(marker);
                break;
            case "restaurant":
                restaurantMarkers.add(marker);
                break;
            case "cafe":
                cafeMarkers.add(marker);
                break;
            case "hotel":
                hotelMarkers.add(marker);
                break;
            case "gift_shop":
                giftShopMarkers.add(marker);
                break;
            case "bar":
                barMarkers.add(marker);
                break;
        }

        // Set the marker tag as the placeId associated with the place
        marker.setTag(placeSuggestion.getPlaceId());
    }

    private BitmapDescriptor getMarkerIcon(String placeType) {
        switch (placeType) {
            case "museum":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            case "park":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
            case "restaurant":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
            case "cafe":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
            case "hotel":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
            case "gift_shop":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
            case "tourist_attraction":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE);
            case "bar":
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
            default:
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
        }
    }

    private void clearAllFilters() {
        // Show all markers
        showMarkers(museumMarkers);
        showMarkers(parkMarkers);
        showMarkers(restaurantMarkers);
        showMarkers(cafeMarkers);
        showMarkers(hotelMarkers);
        showMarkers(giftShopMarkers);
        showMarkers(barMarkers);
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