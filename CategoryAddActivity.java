package com.example.smartreaderapp.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartreaderapp.databinding.ActivityCategoryAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class CategoryAddActivity extends AppCompatActivity {

    private ActivityCategoryAddBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private String category = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // Back button event
        binding.backBtn.setOnClickListener(v -> onBackPressed());

        // Submit button event
        binding.submitBtn.setOnClickListener(v -> validateData());
    }

    private void validateData() {
        category = binding.categoryEt.getText().toString().trim();

        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please enter category....", Toast.LENGTH_SHORT).show();
        } else {
            addCategoryFirebase();
        }
    }

    private void addCategoryFirebase() {
        progressDialog.setMessage("Adding category....");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", String.valueOf(timestamp));
        hashMap.put("category", category);
        hashMap.put("timestamp", timestamp);
        hashMap.put("uid", firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : "Unknown");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(String.valueOf(timestamp))
                .setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(CategoryAddActivity.this, "Category added successfully!", Toast.LENGTH_SHORT).show();
                    binding.categoryEt.setText(""); // Clear input field after adding
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(CategoryAddActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
