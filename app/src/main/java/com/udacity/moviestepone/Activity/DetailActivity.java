package com.udacity.moviestepone.Activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.udacity.moviestepone.Fragment.DetailFragment;
import com.udacity.moviestepone.R;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

}
