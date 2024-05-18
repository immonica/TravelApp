package fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelapp.R;

public class DayFragment extends Fragment {

    private static final String ARG_POSITION = "position";

    public static DayFragment newInstance(int position) {
        DayFragment fragment = new DayFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);

        // Retrieve position from arguments and display corresponding text
        int position = getArguments().getInt(ARG_POSITION);
        TextView dayText = view.findViewById(R.id.day_text);
        dayText.setText("Text " + (position + 1));

        return view;
    }
}