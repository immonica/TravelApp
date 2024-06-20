package fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.travelapp.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import android.widget.ToggleButton;
import android.widget.CompoundButton;



import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements OnMapReadyCallback{

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap == null) {
            mMap = googleMap;
            initializeMap();
        }
    }
    private void initializeMap() {
        Toast.makeText(getContext(), "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "initializeMap: map is ready");

        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    String placeId = marker.getTag().toString();
                    fetchPlaceDetails(placeId);
                    return true;
                }
            });
        }
    }

    private static final String TAG = "HomeFragment";
    private String city;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //widgets
    private AutoCompleteTextView mSearchText;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutocompleteSupportFragment autocompleteFragment;
    private PlacesClient placesClient;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        getLocationPermission();

        Places.initialize(getActivity(), getString(R.string.my_map_api_key));
        autocompleteFragment = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setHint("Search for a location");
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng latLng = place.getLatLng();
                moveCamera(latLng, DEFAULT_ZOOM, place.getAddress(), place.getId());
                String placeName = place.getName();
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    if (!addresses.isEmpty()) {
                        city = addresses.get(0).getLocality();
                    } else {
                        Log.e(TAG, "No address found for the location");
                        Toast.makeText(getContext(), "No address found for the location", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoding failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Geocoding failed", Toast.LENGTH_SHORT).show();
                }
                performTextSearch(placeName);
            }
            @Override
            public void onError(Status status) {
                Toast.makeText(getContext(), "Some Error is Search", Toast.LENGTH_SHORT).show();
            }
        });

        // Access the search input field of the AutocompleteSupportFragment
        View autoCompleteView = autocompleteFragment.getView();
        if (autoCompleteView != null) {
            EditText searchInput = autoCompleteView.findViewById(com.google.android.libraries.places.R.id.places_autocomplete_search_input);
            if (searchInput != null) {
                searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH
                                || actionId == EditorInfo.IME_ACTION_DONE
                                || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                                || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                            geoLocate();
                            return true;
                        }
                        return false;
                    }
                });
            }
        }

        placesClient = Places.createClient(requireContext());

        // Search Button
        view.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPopupDialog();
            }
        });

        // Account icon
        ImageView accountIcon = view.findViewById(R.id.account_icon);
        accountIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                PopUpFragment popUpFragment = new PopUpFragment();
                fragmentTransaction.replace(R.id.fragmentContainer, popUpFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        return view;
    }

    private void openPopupDialog() {
        View searchDialogView = getLayoutInflater().inflate(R.layout.popup_layout, null);

        EditText cityEditText = searchDialogView.findViewById(R.id.cityEditText);
        EditText startDateEditText = searchDialogView.findViewById(R.id.startDateEditText);
        EditText endDateEditText = searchDialogView.findViewById(R.id.endDateEditText);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(searchDialogView)
                .setTitle("Search for a trip")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create Trip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String city = cityEditText.getText().toString().trim();
                        String startDateString = startDateEditText.getText().toString().trim();
                        String endDateString = endDateEditText.getText().toString().trim();
                        if (!city.isEmpty() && !startDateString.isEmpty() && !endDateString.isEmpty()) {
                            saveTripToFirebase(city, startDateString, endDateString);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        startDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(startDateEditText, endDateEditText);
            }
        });
        endDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(endDateEditText, startDateEditText);
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDatePickerDialog(final EditText dateEditText, final EditText startDateEditText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                        dateEditText.setText(selectedDate);
                    }
                }, year, month, day);

        // Set the minimum date to the day after the start date
        if (startDateEditText.getText().toString().isEmpty()) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        } else {
            // If start date is set, set minimum date to the day after the start date
            String[] startDateParts = startDateEditText.getText().toString().split("/");
            Calendar startDateCalendar = Calendar.getInstance();
            startDateCalendar.set(Integer.parseInt(startDateParts[2]), Integer.parseInt(startDateParts[1]) - 1, Integer.parseInt(startDateParts[0]));
            datePickerDialog.getDatePicker().setMinDate(startDateCalendar.getTimeInMillis() + (24 * 60 * 60 * 1000));
        }
        datePickerDialog.show();
    }

    private void saveTripToFirebase(String city, String startDate, String endDate) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tripRef = databaseRef.child("users").child(uid).child("trips").push();

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("city", city);
        tripData.put("startDate", startDate);
        tripData.put("endDate", endDate);

        // Calculate days between start and end dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date startDateObj = sdf.parse(startDate);
            Date endDateObj = sdf.parse(endDate);
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTime(startDateObj);
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(endDateObj);

            List<String> daysList = new ArrayList<>();

            while (!startCalendar.after(endCalendar)) {
                String day = sdf.format(startCalendar.getTime());
                daysList.add(day);
                startCalendar.add(Calendar.DATE, 1);
            }

            tripData.put("days", daysList);
            tripRef.setValue(tripData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Trip data saved successfully!");
                            Fragment mapFragment = new MapFragment();
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainer, mapFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error saving trip data: " + e.getMessage());
                        }
                    });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void geoLocate(){
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(requireContext());
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(list.size() > 0){
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0), "somePlaceId");
        }
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME,
                Place.Field.ADDRESS, Place.Field.PHONE_NUMBER, Place.Field.WEBSITE_URI, Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            displayPlaceDetails(place);
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Place not found: " + exception.getMessage());
        });
    }

    private void displayPlaceDetails(Place place) {
        String name = place.getName();
        String address = place.getAddress();
        String phoneNumber = place.getPhoneNumber();
        Uri websiteUri = place.getWebsiteUri();
        LatLng latLng = place.getLatLng();

        StringBuilder info = new StringBuilder();
        info.append("Name: ").append(name).append("\n");
        info.append("Address: ").append(address).append("\n");
        info.append("Phone: ").append(phoneNumber).append("\n");

        if (websiteUri != null) {
            info.append("Website: ").append(websiteUri.toString());
        }

        View dialogView = getLayoutInflater().inflate(R.layout.place_details_dialog, null);

        TextView placeNameTextView = dialogView.findViewById(R.id.place_name_text_view);
        placeNameTextView.setText(name);

        TextView placeAddressTextView = dialogView.findViewById(R.id.place_address_text_view);
        placeAddressTextView.setText(address);

        TextView placePhoneTextView = dialogView.findViewById(R.id.place_phone_text_view);
        placePhoneTextView.setText(phoneNumber);

        TextView placeWebsiteTextView = dialogView.findViewById(R.id.place_website_text_view);
        if (websiteUri != null) {
            placeWebsiteTextView.setText(websiteUri.toString());
        } else {
            placeWebsiteTextView.setVisibility(View.GONE);
        }

        fetchPlacePhoto(place, dialogView);

        ToggleButton toggleFavoriteButton = dialogView.findViewById(R.id.toggle_favorite_button);
        checkIfFavorite(place.getId(), toggleFavoriteButton);  // Check and set the initial state of the toggle button

        toggleFavoriteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    saveToFavorites(place, city, place.getId());
                } else {
                    removeFromFavorites(place.getId());
                }
            }
        });

        //Directions icon
        ImageView directionsIcon = dialogView.findViewById(R.id.directions_icon);
        directionsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGoogleMapsForDirections(address);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView)
                .setTitle("Place Details")
                .setPositiveButton("OK", null)
                .show();
    }

    private void openGoogleMapsForDirections(String address) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Open Google Maps")
                .setMessage("Are you sure you want to open Google Maps to see directions to this location?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(address));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(getContext(), "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void fetchPlacePhoto(Place place, View dialogView) {
        List<Place.Field> fields = Arrays.asList(Place.Field.PHOTO_METADATAS);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(place.getId(), fields);

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place fetchedPlace = response.getPlace();
            List<PhotoMetadata> photoMetadataList = fetchedPlace.getPhotoMetadatas();
            if (photoMetadataList != null && !photoMetadataList.isEmpty()) {
                PhotoMetadata photoMetadata = photoMetadataList.get(0);

                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxHeight(1600)
                        .setMaxWidth(1600)
                        .build();

                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                    if (bitmap != null) {
                        ImageView imageView = dialogView.findViewById(R.id.place_photo_image_view);
                        if (imageView != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                        }
                    }
                }).addOnFailureListener((exception) -> {
                    Log.e(TAG, "Place photo not found: " + exception.getMessage());
                });
            }
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Place not found: " + exception.getMessage());
        });
    }

    private void checkIfFavorite(String placeId, ToggleButton toggleFavoriteButton) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("favorites").child(placeId);

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    toggleFavoriteButton.setChecked(true);
                } else {
                    toggleFavoriteButton.setChecked(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error checking favorite status: " + databaseError.getMessage());
            }
        });

    }

    private void saveToFavorites(Place place, String city, String placeId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("favorites").push();

        String key = favRef.getKey();

        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("name", place.getName());
        favoriteData.put("address", place.getAddress());
        favoriteData.put("city", city);
        favoriteData.put("latLng", place.getLatLng().latitude + "," + place.getLatLng().longitude);
        favoriteData.put("placeType", "favorite");
        favoriteData.put("key", key);
        favoriteData.put("placeId", placeId);

        favRef.setValue(favoriteData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Place added to favorites");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error adding place to favorites: " + e.getMessage());
                    }
                });
    }

    private void removeFromFavorites(String placeId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).child("favorites").child(placeId);

        favRef.removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Place removed from favorites");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error removing place from favorites: " + e.getMessage());
                    }
                });

    }

    private void performTextSearch(String query) {
        PlacesClient placesClient = Places.createClient(requireContext());
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FindAutocompletePredictionsResponse response = task.getResult();
                if (response != null) {
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    for (AutocompletePrediction prediction : predictions) {
                        Log.i(TAG, "Place: " + prediction.getPlaceId() + ", " + prediction.getFullText(null));
                    }
                }
            } else {
                Log.e(TAG, "Text search failed: " + task.getException().getMessage());
            }
        });
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        try{
            if(mLocationPermissionsGranted){
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM,
                                    "My Location", "");
                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(getContext(), "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title, String placeId){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            Marker marker = mMap.addMarker(options);
            marker.setTag(placeId);
        }
        hideSoftKeyboard();
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(HomeFragment.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(requireContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(requireContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(requireActivity(),
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(requireActivity(),
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;

                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard() {
        if (getContext() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (getView() != null && inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }
}