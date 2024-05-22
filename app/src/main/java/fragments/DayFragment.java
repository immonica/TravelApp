package fragments;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelapp.R;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DayFragment extends Fragment {

    private static final String ARG_DAY = "day";
    private static final String ARG_ITINERARY = "itinerary";
    private PlacesClient placesClient;

    public static DayFragment newInstance(String day, List<String> itinerary) {
        DayFragment fragment = new DayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, day);
        args.putStringArrayList(ARG_ITINERARY, new ArrayList<>(itinerary != null ? itinerary : new ArrayList<>()));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);

        Places.initialize(requireContext(), getString(R.string.my_map_api_key));
        placesClient = Places.createClient(requireContext());

        if (getArguments() != null) {
            String day = getArguments().getString(ARG_DAY);
            List<String> itinerary = getArguments().getStringArrayList(ARG_ITINERARY);

            LinearLayout dayContentLayout = view.findViewById(R.id.day_content_layout);
            dayContentLayout.removeAllViews();

            if (itinerary == null || itinerary.isEmpty()) {
                TextView noLocationsTextView = new TextView(getContext());
                noLocationsTextView.setText("No locations for this day saved");
                dayContentLayout.addView(noLocationsTextView);
            } else {
                LayoutInflater layoutInflater = LayoutInflater.from(getContext());
                for (String place : itinerary) {
                    View itineraryView = layoutInflater.inflate(R.layout.itinerary_layout, dayContentLayout, false);
                    TextView itineraryTextView = itineraryView.findViewById(R.id.itinerary_text_view);
                    ImageView imageView = itineraryView.findViewById(R.id.place_itinerary_image_view);

                    itineraryTextView.setText(place);
                    // Fetch and set photo for the place
                    fetchPlacePhoto(place, imageView);
                    dayContentLayout.addView(itineraryView);
                }
            }
        }

        return view;
    }

    private void fetchPlacePhoto(String cityName, ImageView imageView) {
        // Create a FindAutocompletePredictionsRequest for the city name
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
        // Define the fields to be returned for the photo
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
}
