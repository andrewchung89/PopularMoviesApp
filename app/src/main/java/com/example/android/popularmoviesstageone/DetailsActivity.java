package com.example.android.popularmoviesstageone;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;

public class DetailsActivity extends AppCompatActivity implements DetailsFragment.OnFragmentInteractionListener {

    Integer id;

    String LOG_TAG = "DetailsActivity";

    Boolean restored = Boolean.FALSE;
    private static final String favoritedBookNamesKey = "favoritedBookNamesKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        id = intent.getIntExtra("Movie ID", 0);

        if (!restored) {
            DetailsFragment detailsFragment = new DetailsFragment();
            Bundle arguments = new Bundle();
            arguments.putBoolean("twoPane", Boolean.FALSE);
            arguments.putInt("MovieId", id);
            detailsFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.movie_detail_container, detailsFragment)
                    .commit();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


}
