package com.example.smartreaderapp.activities;

import static android.content.ContentValues.TAG;

import android.Manifest; // FIXED: Added missing import
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartreaderapp.MyApplication;
import com.example.smartreaderapp.R;
import com.example.smartreaderapp.databinding.ActivityPdfDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class PdfDetailActivity extends AppCompatActivity {

    private ActivityPdfDetailBinding binding;

    private String bookId, bookTitle, bookUrl;

    boolean isInMyFavorites = false;

    private FirebaseAuth firebaseAuth;

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        binding.downloadBookBtn.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() !=null){
            checkIsFavorite();
        }

        loadBookDetails();
        MyApplication.incrementBookViewCount(bookId);


        binding.backBtn.setOnClickListener(v -> onBackPressed());

        binding.readBookBtn.setOnClickListener(v -> {
            Intent intent1 = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
            intent1.putExtra("bookId", bookId);
            startActivity(intent1);
        });

        binding.downloadBookBtn.setOnClickListener(v -> {
            Log.d(TAG_DOWNLOAD, "onClick: Checking permission");
            if (ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_DOWNLOAD, "onClick : Permission already granted, can download book");
                MyApplication.downloadBook(PdfDetailActivity.this, bookId, bookTitle, bookUrl);
            } else {
                Log.d(TAG_DOWNLOAD, "onClick: Permission was not granted, requesting permission...");
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

        });

        binding.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this,"You're not logged in",Toast.LENGTH_SHORT).show();

                }
                else {
                    if (isInMyFavorites) {
                        MyApplication.removeFromFavorites(PdfDetailActivity.this,bookId);
                    }
                    else {
                        MyApplication.addToFavorite(PdfDetailActivity.this,bookId);
                    }
                }
            }
        });


    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG_DOWNLOAD, "Permission Granted");
                    MyApplication.downloadBook(this, bookId, bookTitle, bookUrl);
                } else {
                    Log.d(TAG_DOWNLOAD, "Permission was denied.");
                    Toast.makeText(this, "Permission was denied.", Toast.LENGTH_SHORT).show();
                }
            });

    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookTitle = ""+snapshot.child("title").getValue();
                String description = ""+snapshot.child("description").getValue();
                String categoryId = ""+snapshot.child("categoryId").getValue();
                String viewsCount = ""+snapshot.child("viewsCount").getValue();
                String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                bookUrl = ""+snapshot.child("url").getValue();
                String timestamp = ""+snapshot.child("timestamp").getValue();

                // Make download button visible
                binding.downloadBookBtn.setVisibility(View.VISIBLE);

                String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                // Load additional book details
                MyApplication.loadCategory(""+categoryId, binding.categoryTv);
                MyApplication.loadPdfFromUrlSinglePage(""+bookUrl, ""+bookTitle, binding.pdfView, binding.progressBar,binding.pagesTv);
                MyApplication.loadPdfSize(""+bookUrl, ""+bookTitle, binding.sizeTv);


                // Set UI values
                binding.titleTv.setText(bookTitle);
                binding.descriptionTv.setText(description);
                binding.viewsTv.setText(viewsCount.replace("null","N/A"));
                binding.downloadsTv.setText(downloadsCount.replace("null","N/A"));
                binding.dateTv.setText(date);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load book details", error.toException());
            }
        });
    }


    private void checkIsFavorite(){
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            isInMyFavorites = snapshot.exists();
                            if (isInMyFavorites) {
                                binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.baseline_favorite_24, 0, 0);
                                binding.favoriteBtn.setText("Remove Favorite");
                            } else {
                                binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.baseline_favorite_border_24, 0, 0);
                                binding.favoriteBtn.setText("Add Favorite");

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }

    }

