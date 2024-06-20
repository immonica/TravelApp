package fragments;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.travelapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupFragment extends Fragment {

    private EditText editTextEmail, editTextPassword, editTextPasswordRetype;
    private Button buttonSignIn;
    private FirebaseAuth mAuth;
    private TextView textViewLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextPasswordRetype = view.findViewById(R.id.editTextPasswordRetype);
        buttonSignIn = view.findViewById(R.id.buttonSignIn);
        textViewLogin = view.findViewById(R.id.textViewLogin);

        mAuth = FirebaseAuth.getInstance();

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpUser();
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLoginFragment();
            }
        });

        return view;
    }

    private void signUpUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordRetype = editTextPasswordRetype.getText().toString().trim();

        if (!password.equals(passwordRetype)) {
            Toast.makeText(getContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        navigateToHomeFragment();
                        Toast.makeText(getContext(), "Sign Up Successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Sign Up failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHomeFragment() {
        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, homeFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void navigateToLoginFragment() {
        LoginFragment loginFragment = new LoginFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, loginFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


}

