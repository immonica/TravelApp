package fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.travelapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlannerFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_planner, container, false);

        // Initialize the delete button and set its click listener
        ImageButton deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the delete button click
                onDeleteButtonClick();
            }
        });

        // Initialize ViewPager2 and TabLayout
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);

        // Retrieve trip data from Firebase
        retrieveTripData();

        return view;
    }

    private void retrieveTripData() {
        // Get the current user's UID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Get a reference to the trips node for the current user
            DatabaseReference tripRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(uid).child("trips");

            // Query to get the last trip added for the user
            Query lastTripQuery = tripRef.orderByKey().limitToLast(1);

            // Attach a ValueEventListener to retrieve the last trip data
            lastTripQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Check if there is any trip data
                    if (snapshot.exists()) {
                        // Retrieve the last trip data
                        DataSnapshot lastTripSnapshot = snapshot.getChildren().iterator().next();
                        String city = lastTripSnapshot.child("city").getValue(String.class);
                        String startDate = lastTripSnapshot.child("startDate").getValue(String.class);
                        String endDate = lastTripSnapshot.child("endDate").getValue(String.class);
                        List<String> days = lastTripSnapshot.child("days").getValue(new GenericTypeIndicator<List<String>>() {});

                        // Log retrieved trip data
                        Log.d("PlannerFragment", "City: " + city);
                        Log.d("PlannerFragment", "StartDate: " + startDate);
                        Log.d("PlannerFragment", "EndDate: " + endDate);
                        Log.d("PlannerFragment", "Days: " + days);

                        // Check if the days list is not null
                        if (days != null) {
                            // Retrieve itinerary data for each day
                            Map<String, List<String>> itineraryMap = new HashMap<>();
                            DataSnapshot itinerarySnapshot = lastTripSnapshot.child("itinerary");

                            for (DataSnapshot yearSnapshot : itinerarySnapshot.getChildren()) {
                                String year = yearSnapshot.getKey();
                                for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) {
                                    String month = monthSnapshot.getKey();
                                    for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) {
                                        String day = daySnapshot.getKey();
                                        String date = day + "/" + month + "/" + year;

                                        List<String> places = new ArrayList<>();
                                        for (DataSnapshot placeSnapshot : daySnapshot.getChildren()) {
                                            // Skip processing if the child is "day", "month", or "year"
                                            if (placeSnapshot.getKey().equals("day") || placeSnapshot.getKey().equals("month") || placeSnapshot.getKey().equals("year")) {
                                                continue;
                                            }

                                            String placeName = placeSnapshot.child("name").getValue(String.class);
                                            if (placeName != null) {
                                                places.add(placeName);
                                            }
                                        }
                                        itineraryMap.put(date, places);
                                        Log.d("PlannerFragment", "Date: " + date + ", Places: " + places);
                                    }
                                }
                            }

                            // Set up ViewPager2 and TabLayout with retrieved trip data and itinerary data
                            setUpViewPagerAndTabLayout(city, startDate, endDate, days, itineraryMap);
                        } else {
                            // Handle case where days list is null
                            Log.e("PlannerFragment", "Days list is null for trip with city: " + city);
                        }
                    } else {
                        // Log that no trip data was found
                        Log.d("PlannerFragment", "No trip data found.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle database error
                    Log.e("FirebaseError", "Database error: " + error.getMessage());
                }
            });
        } else {
            // Log that the user is not authenticated
            Log.d("PlannerFragment", "User not authenticated.");
        }
    }

    private void setUpViewPagerAndTabLayout(String city, String startDate, String endDate, List<String> days, Map<String, List<String>> itineraryMap) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(requireActivity(), days, itineraryMap);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(days.get(position));
            }
        }).attach();
    }

    private void onDeleteButtonClick() {
        // Navigate back to the previous fragment in the back stack
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
        }
    }

    // ViewPager2 Adapter
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        private List<String> days;
        private Map<String, List<String>> itineraryMap;

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<String> days, Map<String, List<String>> itineraryMap) {
            super(fragmentActivity);
            this.days = days;
            this.itineraryMap = itineraryMap;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String day = days.get(position);
            // Convert day to the format year/month/day to match itineraryMap keys
            String[] dateParts = day.split("/");
            String formattedDate = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
            List<String> itinerary = itineraryMap.get(formattedDate);
            return DayFragment.newInstance(day, itinerary != null ? itinerary : new ArrayList<>());
        }

        @Override
        public int getItemCount() {
            return days.size();
        }
    }
}