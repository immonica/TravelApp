package fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelapp.R;

import java.util.ArrayList;
import java.util.List;

public class DayFragment extends Fragment {

    private static final String ARG_DAY = "day";
    private static final String ARG_ITINERARY = "itinerary";

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
                    itineraryTextView.setText(place);
                    dayContentLayout.addView(itineraryView);
                }
            }
        }

        return view;
    }
}
