package fragments;

import android.os.Bundle;
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

public class PopUpFragment extends Fragment {

    private EditText mSearchText;

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
                    // Handle search action
                    String query = mSearchText.getText().toString();
                    // Pass search query to HomeFragment and navigate to it
                    ((MainActivity) requireActivity()).navigateToHomeFragmentWithSearch(query);
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
}

