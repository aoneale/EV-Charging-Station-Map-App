package com.example.cs160_prog2b;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class StationDetailsActivity extends FragmentActivity implements OnStreetViewPanoramaReadyCallback {

    Station station = new Station();

    final String apiKey = "AIzaSyBPNvmHksoWV2yEJDKc4e00LSheQQrcgTo";
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_details);

        // Get the Intent that started this activity
        Intent intent = getIntent();

        station = intent.getParcelableExtra("Station");

        System.out.println("Got the station!!");
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getPlace();

    }


    public void openStreetView(View view) {
        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.streetviewpanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        View view = (View) findViewById(R.id.GoogleStreetViewFrame);
        view.setVisibility(View.VISIBLE);
        panorama.setPosition(new LatLng(station.latitude, station.longitude));
    }

    public void closeStreetView(View view) {
        View v = (View) findViewById(R.id.GoogleStreetViewFrame);
        v.setVisibility(View.GONE);
    }


    void getPlace() {

        // Initialize the places SDK
        Places.initialize(getApplicationContext(), apiKey);
        // Create a new Places client instance
        final PlacesClient placesClient = Places.createClient(this);

        // Define a Place ID.
        final String placeId = station.placeID;

        // Specify the fields to return.
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ADDRESS, Place.Field.ID, Place.Field.LAT_LNG,
                Place.Field.RATING, Place.Field.NAME, Place.Field.UTC_OFFSET, Place.Field.OPENING_HOURS,
                Place.Field.WEBSITE_URI, Place.Field.PHOTO_METADATAS);

        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
            @Override
            public void onSuccess(FetchPlaceResponse response) {
                Place place = response.getPlace();
                Log.i(TAG, "Place found: " + place.getName());

                station.setPlace(place);

                // Station Name
                TextView textView = (TextView) findViewById(R.id.stationName3);
                textView.setText(place.getName());

                // Address
                textView = (TextView) findViewById(R.id.address3);
                textView.setText(place.getAddress());

                // Availability
                textView = (TextView) findViewById(R.id.availability3);

                if (station.available == 0) {
                    textView.setText("No Chargers Available");
                    textView.setTextColor(Color.RED);
                } else {
                    textView.setText("1 Charger Available");
                    textView.setTextColor(ContextCompat.getColor(StationDetailsActivity.this, R.color.textColor));
                }

                // Time to get there
                textView = (TextView) findViewById(R.id.minToGetThere3);
                textView.setText(String.format("%d MIN", station.minToGetThere));

                // Price
                textView = (TextView) findViewById(R.id.price3);
                textView.setText(String.format("$0.%d per kWh", station.price));

                // Time to Charge
                textView = (TextView) findViewById(R.id.timeToCharge3);
                textView.setText(String.format("%.0fH %.0fM to full charge", station.chargeHours, station.chargeMins));

                // Wattage
                textView = (TextView) findViewById(R.id.kW3);
                textView.setText(String.format("%d kW", station.wattage));

                // Website
                String website = place.getWebsiteUri().toString();
                textView = (TextView) findViewById(R.id.website);
                textView.setText(website);

                // Rating
                Double rating = place.getRating();
                textView = (TextView) findViewById(R.id.avgRating);
                if (rating == null) {
                    textView.setText("No Reviews");
                } else {
                    textView.setText(String.format("%.1f", rating));
                }


                // Picture
                if (place.getPhotoMetadatas() != null) {

                    PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(0);

                    // Create a FetchPhotoRequest.
                    FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                            .build();
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener(new OnSuccessListener<FetchPhotoResponse>() {
                        @Override
                        public void onSuccess(FetchPhotoResponse fetchPhotoResponse) {
                            Bitmap bitmap = fetchPhotoResponse.getBitmap();
                            ImageView imageView = (ImageView) findViewById(R.id.picture);
                            imageView.setImageBitmap(bitmap);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imageView.setForeground(null);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            if (exception instanceof ApiException) {
                                ApiException apiException = (ApiException) exception;
                                int statusCode = apiException.getStatusCode();
                                // Handle error with given status code.
                                Log.e(TAG, "Place not found: " + exception.getMessage());
                            }
                        }
                    });
                }
        }

    });


}



    public void returnToMap(View view) {
        Intent intent = new Intent();

        //intent.putExtra("maxLevelOn", maxLevelOn);

        setResult(RESULT_OK, intent);
        finish();
    }



}
