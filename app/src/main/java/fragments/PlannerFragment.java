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

        // delete button
        ImageButton deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeleteButtonClick();
            }
        });

        //ViewPager2 and TabLayout
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);

        retrieveTripData();

        return view;
    }

    private void retrieveTripData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            DatabaseReference tripRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(uid).child("trips");

            Query lastTripQuery = tripRef.orderByKey().limitToLast(1);

            lastTripQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        DataSnapshot lastTripSnapshot = snapshot.getChildren().iterator().next();
                        String tripKey = lastTripSnapshot.getKey();
                        String city = lastTripSnapshot.child("city").getValue(String.class);
                        String startDate = lastTripSnapshot.child("startDate").getValue(String.class);
                        String endDate = lastTripSnapshot.child("endDate").getValue(String.class);
                        List<String> days = lastTripSnapshot.child("days").getValue(new GenericTypeIndicator<List<String>>() {});

                        if (days != null) {
                            Map<String, List<Map<String, Object>>> itineraryMap = new HashMap<>();
                            DataSnapshot itinerarySnapshot = lastTripSnapshot.child("itinerary");

                            for (DataSnapshot yearSnapshot : itinerarySnapshot.getChildren()) {
                                String year = yearSnapshot.getKey();
                                for (DataSnapshot monthSnapshot : yearSnapshot.getChildren()) {
                                    String month = monthSnapshot.getKey();
                                    for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) {
                                        String day = daySnapshot.getKey();
                                        String date = day + "/" + month + "/" + year;

                                        List<Map<String, Object>> places = new ArrayList<>();
                                        for (DataSnapshot placeSnapshot : daySnapshot.getChildren()) {
                                            if (placeSnapshot.getKey().equals("day") || placeSnapshot.getKey().equals("month") || placeSnapshot.getKey().equals("year")) {
                                                continue;
                                            }

                                            Map<String, Object> placeDetails = new HashMap<>();
                                            placeDetails.put("name", placeSnapshot.child("name").getValue(String.class));
                                            placeDetails.put("visited", placeSnapshot.child("visited").getValue(Boolean.class));
                                            placeDetails.put("placeId", placeSnapshot.child("placeId").getValue(String.class));
                                            placeDetails.put("address", placeSnapshot.child("address").getValue(String.class));
                                            placeDetails.put("key", placeSnapshot.getKey()); // Add place key

                                            places.add(placeDetails);
                                        }
                                        itineraryMap.put(date, places);
                                    }
                                }
                            }
                            setUpViewPagerAndTabLayout(city, startDate, endDate, days, itineraryMap, tripKey); // Pass tripKey here
                        } else {
                            Log.e("PlannerFragment", "Days list is null for trip with city: " + city);
                        }
                    } else {
                        Log.d("PlannerFragment", "No trip data found.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseError", "Database error: " + error.getMessage());
                }
            });
        } else {
            Log.d("PlannerFragment", "User not authenticated.");
        }
    }

    private void setUpViewPagerAndTabLayout(String city, String startDate, String endDate, List<String> days,
                                            Map<String, List<Map<String, Object>>> itineraryMap, String tripKey) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(requireActivity(), days, itineraryMap, tripKey);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(days.get(position))).attach();
    }

    private void onDeleteButtonClick() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
        }
    }

    // ViewPager2 Adapter
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        private List<String> days;
        private Map<String, List<Map<String, Object>>> itineraryMap;
        private String tripKey;

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<String> days,
                                Map<String, List<Map<String, Object>>> itineraryMap, String tripKey) {
            super(fragmentActivity);
            this.days = days;
            this.itineraryMap = itineraryMap;
            this.tripKey = tripKey;
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String day = days.get(position);
            String[] dateParts = day.split("/");
            String formattedDate = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
            List<Map<String, Object>> itinerary = itineraryMap.get(formattedDate);
            return DayFragment.newInstance(day, itinerary != null ? itinerary : new ArrayList<>(), tripKey);
        }
        @Override
        public int getItemCount() {
            return days.size();
        }
    }
}