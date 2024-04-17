package fragments;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelapp.MainActivity;
import com.example.travelapp.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PopUpFragment extends Fragment {

    private EditText mSearchText;

    // TAG for logging
    private static final String TAG = "PopUpFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_popup, container, false);

        // Initialize EditText for search
        mSearchText = rootView.findViewById(R.id.editText);

        // Set listener for search action
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                        event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    // Log message to check if the method is being called
                    Log.d(TAG, "Search action triggered");
                    // Handle search action
                    String query = mSearchText.getText().toString();
                    // Call geoLocate() to search for the location
                    geoLocate(query);
                    return true;
                }
                return false;
            }
        });


        // Button Click Listener
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopUpFragment();
            }
        });

        return rootView;
    }

    private void closePopUpFragment() {
        // Go back to the previous fragment (HomeFragment)
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void geoLocate(String searchString) {
        Log.d(TAG, "geoLocate: geolocating with query: " + searchString);

        Geocoder geocoder = new Geocoder(requireContext());
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());

            // Here you can use the address details to handle the location
            // For example, you might pass the latitude and longitude to the HomeFragment
            double latitude = address.getLatitude();
            double longitude = address.getLongitude();
            String locationName = address.getAddressLine(0);
            // Pass the location details to the HomeFragment and navigate to it
            ((MainActivity) requireActivity()).navigateToHomeFragmentWithLocation(latitude, longitude, locationName);
        }
    }
}

