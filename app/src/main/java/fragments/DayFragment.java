package fragments;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DayFragment extends Fragment {

    private static final String ARG_DAY = "day";
    private static final String ARG_ITINERARY = "itinerary";
    private static final String ARG_TRIP_KEY = "trip_key";
    private PlacesClient placesClient;
    private DatabaseReference tripRef;
    private String tripKey;

    public static DayFragment newInstance(String day, List<Map<String, Object>> itinerary, String tripKey) {
        DayFragment fragment = new DayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, day);
        args.putSerializable(ARG_ITINERARY, (Serializable) itinerary);
        args.putString(ARG_TRIP_KEY, tripKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);

        Places.initialize(requireContext(), getString(R.string.my_map_api_key));
        placesClient = Places.createClient(requireContext());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            tripRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(currentUser.getUid()).child("trips");
        }

        if (getArguments() != null) {
            String day = getArguments().getString(ARG_DAY);
            List<Map<String, Object>> itinerary = (List<Map<String, Object>>) getArguments().getSerializable(ARG_ITINERARY);
            tripKey = getArguments().getString(ARG_TRIP_KEY);

            LinearLayout dayContentLayout = view.findViewById(R.id.day_content_layout);
            dayContentLayout.removeAllViews();

            Log.d(TAG, "Day: " + day);
            if (itinerary == null || itinerary.isEmpty()) {
                Log.d(TAG, "No itinerary data for this day.");
                TextView noLocationsTextView = new TextView(getContext());
                noLocationsTextView.setText("No locations for this day saved");
                dayContentLayout.addView(noLocationsTextView);
            } else {
                Log.d(TAG, "Itinerary data found for this day.");
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                for (Map<String, Object> place : itinerary) {
                    View itineraryView = layoutInflater.inflate(R.layout.itinerary_layout, dayContentLayout, false);
                    TextView itineraryTextView = itineraryView.findViewById(R.id.itinerary_text_view);
                    ImageView imageView = itineraryView.findViewById(R.id.place_itinerary_image_view);
                    CheckBox visitCheckBox = itineraryView.findViewById(R.id.visit_checkbox);
                    Button removeButton = itineraryView.findViewById(R.id.remove_itinerary_button);

                    String placeName = (String) place.get("name");
                    itineraryTextView.setText(placeName);
                    visitCheckBox.setChecked((Boolean) place.get("visited"));

                    visitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        place.put("visited", isChecked);
                        updateVisitedStatus(currentUser.getUid(), tripKey, day, place);
                        sortAndRefreshViews(dayContentLayout, layoutInflater);
                    });
                    fetchPlacePhoto(placeName, imageView);

                    removeButton.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle("Confirm");
                        builder.setMessage("Are you sure you want to remove this place from the itinerary?");
                        builder.setPositiveButton("Yes", (dialog, which) -> {
                            removePlaceFromItinerary(dayContentLayout, itineraryView, place);
                        });
                        builder.setNegativeButton("No", (dialog, which) -> {
                        });
                        builder.show();
                    });

                    itineraryView.setOnClickListener(v -> {
                        openGoogleMapsForDirections((String) place.get("address"));
                    });

                    dayContentLayout.addView(itineraryView);
                    Log.d(TAG, "Place Name: " + place.get("name"));
                }
            }
        }

        return view;
    }
    private void updateVisitedStatus(String uid, String tripKey, String day, Map<String, Object> place) {
        if (tripRef != null) {
            boolean visited = (Boolean) place.get("visited");
            Log.d(TAG, "Updating visited status for place in trip: " + tripKey + ", Day: " + day + ", Visited: " + visited);

            DatabaseReference placeRef = tripRef.child(tripKey)
                    .child("itinerary")
                    .child(day)
                    .child((String) place.get("key"))
                    .child("visited");
            placeRef.setValue(visited)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Visited status updated successfully for place in trip: " + tripKey);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating visited status for place in trip: " + tripKey + ", Error: " + e.getMessage());
                    });
        } else {
            Log.e(TAG, "TripRef is null");
        }
    }

    private void sortAndRefreshViews(LinearLayout dayContentLayout, LayoutInflater layoutInflater) {
        List<View> placeViews = new ArrayList<>();
        for (int i = 0; i < dayContentLayout.getChildCount(); i++) {
            placeViews.add(dayContentLayout.getChildAt(i));
        }

        Collections.sort(placeViews, new Comparator<View>() {
            @Override
            public int compare(View view1, View view2) {
                CheckBox visitCheckBox1 = view1.findViewById(R.id.visit_checkbox);
                CheckBox visitCheckBox2 = view2.findViewById(R.id.visit_checkbox);
                boolean visited1 = visitCheckBox1.isChecked();
                boolean visited2 = visitCheckBox2.isChecked();
                if (!visited1 && visited2) {
                    return -1;
                } else if (visited1 && !visited2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        dayContentLayout.removeAllViews();
        for (View view : placeViews) {
            dayContentLayout.addView(view);
        }
    }

    private void removePlaceFromItinerary(LinearLayout dayContentLayout, View itineraryView, Map<String, Object> place) {
        dayContentLayout.removeView(itineraryView);

        String placeKey = (String) place.get("key");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference placeRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(currentUser.getUid()).child("trips")
                    .child(tripKey).child("itinerary").child(getArguments().getString(ARG_DAY)).child(placeKey);
            placeRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Place removed from itinerary", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error removing place from itinerary: " + e.getMessage());
                        Toast.makeText(requireContext(), "Failed to remove place from itinerary", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void fetchPlacePhoto(String cityName, ImageView imageView) {
        FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                .setQuery(cityName)
                .build();

        placesClient.findAutocompletePredictions(predictionsRequest)
                .addOnSuccessListener((response) -> {
                    if (!response.getAutocompletePredictions().isEmpty()) {
                        String placeId = response.getAutocompletePredictions().get(0).getPlaceId();
                        fetchPhotoByPlaceId(placeId, imageView);
                    }
                })
                .addOnFailureListener((exception) -> {
                    Log.e(TAG, "City not found: " + exception.getMessage());
                });
    }

    private void fetchPhotoByPlaceId(String placeId, ImageView imageView) {
        List<Place.Field> fields = Arrays.asList(Place.Field.PHOTO_METADATAS);

        FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(placeId, fields);

        placesClient.fetchPlace(placeRequest).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            List<PhotoMetadata> photoMetadataList = place.getPhotoMetadatas();
            if (photoMetadataList != null && !photoMetadataList.isEmpty()) {
                PhotoMetadata photoMetadata = photoMetadataList.get(0);
                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxHeight(1600)
                        .setMaxWidth(1600)
                        .build();

                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }).addOnFailureListener((exception) -> {
                    Log.e(TAG, "Place photo not found: " + exception.getMessage());
                });
            }
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Place not found: " + exception.getMessage());
        });
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

    @Override
    public void onResume() {
        super.onResume();
        sortAndRefreshViews((LinearLayout) getView().findViewById(R.id.day_content_layout), LayoutInflater.from(requireContext()));
    }

}