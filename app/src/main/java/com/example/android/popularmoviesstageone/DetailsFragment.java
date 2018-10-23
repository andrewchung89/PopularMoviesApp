package com.example.android.popularmoviesstageone;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetailsFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;

    Integer id;
    TextView mTitle;
    TextView mRating;
    TextView mDate;
    TextView mOverview;
    ImageView mPoster;
    Button favourite;
    Button share;
    String[] youtube_ids;
    int trailer_count;
    int review_count;
    String LOG_TAG = "DetailsFragment";
    View rootView;
    SharedPreferences sharedPreferences;
    String sort_type;
    byte[] fav_poster;
    public static int movie_id;


    public DetailsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences("popular_movies",getActivity().MODE_PRIVATE);
        sort_type = sharedPreferences.getString("sort_type", "popular");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_details, container, false);

        mTitle = (TextView) rootView.findViewById(R.id.tv_title);
        mRating = (TextView) rootView.findViewById(R.id.tv_user_rating);
        mDate = (TextView) rootView.findViewById(R.id.tv_release_date);
        mOverview = (TextView) rootView.findViewById(R.id.tv_synopsis);
        mPoster = (ImageView) rootView.findViewById(R.id.iv_poster);
        favourite = (Button) rootView.findViewById(R.id.favourite);
        favourite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                addToFavorites();
            }
        });
        share = (Button) rootView.findViewById(R.id.share);
        share.setOnClickListener(this);

        Bundle arguments = this.getArguments();
        if(arguments != null)
            id = arguments.getInt("MovieId");

        FetchMovieDetails fetchMovieDetails = new FetchMovieDetails();
        fetchMovieDetails.execute();


        setRetainInstance(true);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            if(trailer_count != 0)
                shareIntent.putExtra(Intent.EXTRA_TEXT, "http://www.youtube.com/watch?v=" + youtube_ids[0]);
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_to)));
        }
    }

    public void addToFavorites() {

        ContentValues values = new ContentValues();
        values.put(MoviesProvider._ID, id);
        values.put(MoviesProvider.TITLE, mTitle.getText().toString());
        values.put(MoviesProvider.SYNOPSIS, mOverview.getText().toString());
        values.put(MoviesProvider.USER_RATING, mRating.getText().toString());
        values.put(MoviesProvider.RELEASE_DATE, mDate.getText().toString());

        BitmapDrawable drawable = (BitmapDrawable) mPoster.getDrawable();
        Bitmap bmp = drawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] image = stream.toByteArray();
        values.put(MoviesProvider.POSTER, image);

        Uri uri = getContext().getContentResolver().insert(MoviesProvider.CONTENT_URI, values);
        Log.d(LOG_TAG, uri.toString());

        if (uri.toString().equals("Duplicate")) {
            deleteFavourite();
        } else {
            Toast.makeText(getContext(), R.string.favourite_add, Toast.LENGTH_SHORT).show();
        }
    }


    public void deleteFavourite(){

       String stringId = Integer.toString(id);
       Uri uri = MoviesProvider.CONTENT_URI;
       uri = uri.buildUpon().appendPath(stringId).build();
       int movieDeleted = getContext().getContentResolver().delete(uri, null, null);
       if (movieDeleted == 1) {
           Toast.makeText(getContext(),R.string.favourite_delete, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }



    public class FetchMovieDetails extends AsyncTask<Void, Void, Void> {

        String LOG_TAG = "FetchMovieDetails";
        String movie_title;
        String date;
        String overview;
        String poster_path;
        String rating;
        Double ratings;

        JSONArray reviews;

        public View createLineView() {
            View v = new View(getContext());
            LinearLayout.LayoutParams v_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    3);
            v_params.topMargin = 30;
            v_params.bottomMargin = 30;
            v.setLayoutParams(v_params);
            return v;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if(sort_type.equals("favourites")) {
                Uri favorites = Uri.parse("content://com.example.android.provider.popularmoviesstageone/favourites");
                Cursor c = getActivity().getContentResolver().query(favorites, null, null, null, "_id");
                if (c.moveToFirst()) {
                    do {
                        if (c.getString(c.getColumnIndex(MoviesProvider._ID)).equals(id.toString())) {
                            movie_title = c.getString(c.getColumnIndex(MoviesProvider.TITLE));
                            rating = c.getString(c.getColumnIndex(MoviesProvider.USER_RATING));
                            overview = c.getString(c.getColumnIndex(MoviesProvider.SYNOPSIS));
                            date = c.getString(c.getColumnIndex(MoviesProvider.RELEASE_DATE));
                            fav_poster = c.getBlob(c.getColumnIndex(MoviesProvider.POSTER));
                        }
                    } while (c.moveToNext());
                }
            }
            else {
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;


                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + Integer.toString(id) + "?api_key=d75de3c3778dad7f64220bd9519dbfd3");
                    Log.d(LOG_TAG, "URL: " + url.toString());

                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }
                    if (buffer.length() == 0) {
                        return null;
                    }
                    String movieJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + movieJsonStr);

                    JSONObject main = new JSONObject(movieJsonStr);
                    movie_title = main.getString("original_title");
                    date = main.getString("release_date");
                    ratings = main.getDouble("vote_average");
                    overview = main.getString("overview");
                    poster_path = "http://image.tmdb.org/t/p/w185" + main.getString("poster_path");

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }


                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + Integer.toString(id) + "/videos" + "?api_key=d75de3c3778dad7f64220bd9519dbfd3");
                    Log.d(LOG_TAG, "URL: " + url.toString());

                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }
                    if (buffer.length() == 0) {
                        return null;
                    }
                    String trailerJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + trailerJsonStr);

                    JSONObject main = new JSONObject(trailerJsonStr);
                    String results = main.getString("results");
                    JSONArray trailers = new JSONArray(results);
                    trailer_count = trailers.length();
                    Log.d(LOG_TAG, "Number of Trailers:" + trailer_count);


                    if (trailer_count != 0) {
                        youtube_ids = new String[trailer_count];
                        for (int i = 0; i < trailer_count; i++) {
                            JSONObject obj = trailers.getJSONObject(i);
                            youtube_ids[i] = obj.getString("key");
                        }
                    }

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }


                try {
                    String base_url = "https://api.themoviedb.org/3/movie/";
                    URL url = new URL(base_url + Integer.toString(id) + "/reviews" + "?api_key=d75de3c3778dad7f64220bd9519dbfd3");
                    Log.d(LOG_TAG, "URL: " + url.toString());

                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }
                    if (buffer.length() == 0) {
                        return null;
                    }
                    String reviewJsonStr = buffer.toString();
                    Log.d(LOG_TAG, "JSON Parsed: " + reviewJsonStr);

                    JSONObject main = new JSONObject(reviewJsonStr);
                    String results = main.getString("results");
                    reviews = new JSONArray(results);
                    review_count = main.getInt("total_results");
                    Log.d(LOG_TAG, "Number of Reviews:" + review_count);

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mTitle.setText(movie_title);
            mOverview.setText(overview);

            mPoster.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mPoster.setPadding(8, 8, 8, 8);

            if(sort_type.equals("favourites")) {
                mRating.setText(rating);
                mDate.setText(date);
                Bitmap bmp = BitmapFactory.decodeByteArray(fav_poster, 0, fav_poster.length);
                mPoster.setImageBitmap(bmp);
                share.setVisibility(View.INVISIBLE);
                favourite.setVisibility(View.VISIBLE);
            }
            else {
                mRating.setText("User Rating: " + Double.toString(ratings) + "/10");
                mDate.setText("Release Date: " + date);
                Picasso.with(getContext()).load(poster_path).into(mPoster);
                favourite.setVisibility(View.VISIBLE);
                LinearLayout details_layout = (LinearLayout) rootView.findViewById(R.id.details_layout);


                if (trailer_count != 0) {

                    share.setVisibility(View.VISIBLE);
                    View v = createLineView();
                    details_layout.addView(v);

                    for (int i = 0; i < trailer_count; i++) {
                        Button b = new Button(getContext());
                        LinearLayout.LayoutParams b_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        b_params.setMargins(30, 10, 20, 20);
                        b.setLayoutParams(b_params);
                        b.setText("Watch Trailer " + Integer.toString(i + 1));
                        b.setId(i + 1001);
                        b.setTextSize(18);
                        b.setPadding(20, 10, 20, 10);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String youtube_id = youtube_ids[view.getId() - 1001];
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + youtube_id));
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + youtube_id));
                                    String title = "Watch video via";
                                    Intent chooser = Intent.createChooser(intent, title);
                                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(chooser);
                                    }
                                }

                            }
                        });
                        details_layout.addView(b);
                    }
                } else {
                    share.setVisibility(View.INVISIBLE); //share button invisible
                }


                if (review_count != 0) {

                    details_layout.addView(createLineView());

                    TextView header = new TextView(getContext());
                    LinearLayout.LayoutParams header_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    header_params.setMargins(30, 10, 20, 20);
                    header.setLayoutParams(header_params);
                    header.setText(R.string.reviews);
                    header.setTextSize(25);
                    header.setTextColor(getResources().getColor(R.color.colorPrimary));
                    details_layout.addView(header);

                    for (int i = 0; i < review_count; i++) {
                        TextView tv = new TextView(getContext());
                        LinearLayout.LayoutParams tv_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        tv_params.setMargins(30, 10, 20, 20);
                        tv.setLayoutParams(tv_params);
                        try {
                            String review = reviews.getJSONObject(i).getString("content");
                            tv.setText(review);
                            details_layout.addView(tv);
                            details_layout.addView(createLineView());
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "JSON Error", e);
                        }
                    }
                }
            }
        }


    }
}