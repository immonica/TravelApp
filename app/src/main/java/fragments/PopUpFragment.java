package fragments;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PopUpFragment extends Fragment {

    private static final String TAG = "PopUpFragment";

    private View rootView;
    private TextView emailTextView;

    private DatabaseReference tripsRef;
    private DatabaseReference favoritesRef;
    private ValueEventListener valueEventListener;
    private ValueEventListener favoritesValueEventListener;
    private LinearLayout tripContainer;
    private LinearLayout favoriteContainer;
    private PlacesClient placesClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_popup, container, false);

        // Button close
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopUpFragment();
            }
        });

        // logout_button
        Button logoutButton = rootView.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        emailTextView = rootView.findViewById(R.id.email_text);
        setEmailText();

        //tripContainer
        tripContainer = rootView.findViewById(R.id.trip_container);
        favoriteContainer = rootView.findViewById(R.id.favorites_container);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());
            tripsRef = userRef.child("trips");
            favoritesRef = userRef.child("favorites");
        }

        Places.initialize(requireContext(), getString(R.string.my_map_api_key));
        placesClient = Places.createClient(requireContext());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchTripsData();
        fetchFavoritesData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (tripsRef != null && valueEventListener != null) {
            tripsRef.removeEventListener(valueEventListener);
        }
        if (favoritesRef != null && favoritesValueEventListener != null) {
            favoritesRef.removeEventListener(favoritesValueEventListener);
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
                    if (totalTrips == 0) {
                        TextView noTripsTextView = new TextView(requireContext());
                        noTripsTextView.setText("No trips created yet");
                        noTripsTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                        noTripsTextView.setTextSize(18);
                        noTripsTextView.setPadding(16, 16, 16, 16);

                        tripContainer.addView(noTripsTextView);
                    } else {
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
        // trip_item layout
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View tripView = inflater.inflate(R.layout.trip_layout, tripContainer, false);

        TextView tripTextView = tripView.findViewById(R.id.trip_text_view);
        ImageView tripImageView = tripView.findViewById(R.id.place_photo_image_view);

        tripTextView.setText(trip.getCity() + ": " + trip.getStartDate() + " - " + trip.getEndDate());

        fetchPlacePhoto(trip.getCity(), tripImageView);

        tripContainer.addView(tripView);
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

    private void fetchFavoritesData() {
        if (favoritesRef != null) {
            if (favoritesValueEventListener != null) {
                favoritesRef.removeEventListener(favoritesValueEventListener);
            }
            favoritesValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    favoriteContainer.removeAllViews();
                    if (dataSnapshot.getChildrenCount() == 0) {
                        TextView noFavoritesTextView = new TextView(requireContext());
                        noFavoritesTextView.setText("No favorite locations");
                        noFavoritesTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                        noFavoritesTextView.setTextSize(18);
                        noFavoritesTextView.setPadding(16, 16, 16, 16);
                        favoriteContainer.addView(noFavoritesTextView);
                    } else {
                        for (DataSnapshot favoriteSnapshot : dataSnapshot.getChildren()) {
                            Favorite favorite = favoriteSnapshot.getValue(Favorite.class);
                            if (favorite != null) { addFavoriteView(favorite); } }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error fetching favorites: " + databaseError.getMessage());
                }
            };
            favoritesRef.addValueEventListener(favoritesValueEventListener);
        }
    }

    private void addFavoriteView(Favorite favorite) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View favoriteView = inflater.inflate(R.layout.favorites_layout, favoriteContainer, false);

        TextView nameTextView = favoriteView.findViewById(R.id.favorite_text_view);
        ImageView imageView = favoriteView.findViewById(R.id.place_favorite_image_view);
        Button removeButton = favoriteView.findViewById(R.id.remove_favorite_button);

        nameTextView.setText(favorite.getName());

        fetchPlacePhoto(favorite.getName(), imageView);

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFavoriteFromFirebase(favorite);
            }
        });
        favoriteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmationDialog(favorite.getName());
            }
        });

        favoriteContainer.addView(favoriteView);
    }

    private void removeFavoriteFromFirebase(Favorite favorite) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Remove Favorite");
        builder.setMessage("Are you sure you want to remove this from your favorites?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (favoritesRef != null) {
                    favoritesRef.child(favorite.getKey()).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(requireContext(), "Favorite removed", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Failed to remove favorite: " + e.getMessage());
                                    Toast.makeText(requireContext(), "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                                }
                            });
                } } });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showConfirmationDialog(String locationName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Open in Google Maps");
        builder.setMessage("Are you sure you want to open directions to " + locationName + " in Google Maps?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openGoogleMaps(locationName);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openGoogleMaps(String locationName) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(locationName));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(requireContext(), "Google Maps app is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void closePopUpFragment() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void logoutUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth.getInstance().signOut();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new LoginFragment())
                        .commit();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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