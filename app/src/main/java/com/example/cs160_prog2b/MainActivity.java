package com.example.cs160_prog2b;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;
//import org.json.simple.*;
//import org.json.simple.parser.JSONParser;

import com.android.volley.toolbox.JsonObjectRequest;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;


import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//AppCompatActivity,
public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener{
    UserState userState;
    GoogleMap mMap;
    private static final String TAG = MainActivity.class.getSimpleName();
    final String apiKey = "AIzaSyBPNvmHksoWV2yEJDKc4e00LSheQQrcgTo";

    HashMap<String, Station> nearbyStations;
    Station selectedStation;
    Marker selectedMarker;
    Marker searchLocationMarker;

    ArrayList<String> cheapestIDs;
    ArrayList<View> showHideViews;

    int lastSelectedCarCode = 0;

    // A default location and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(37.89057, -122.30368);
    private static final int DEFAULT_ZOOM = 13;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    Location mLastKnownLocation = new Location("dummyprovider");

    // The entry point to the Fused Location Provider.
    FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        nearbyStations = new HashMap<>();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation(); // Also sets markers for nearby charging stations

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
**/
        cheapestIDs = new ArrayList<>(3);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // List of views to show hide upon clicking on marker on map
        showHideViews = new ArrayList<>();
        showHideViews.add((View) findViewById(R.id.bottomInfo));
        showHideViews.add((View) findViewById(R.id.stationName));
        showHideViews.add((View) findViewById(R.id.address));
        showHideViews.add((View) findViewById(R.id.minToGetThere));
        showHideViews.add((View) findViewById(R.id.availability));
        showHideViews.add((View) findViewById(R.id.price));
        showHideViews.add((View) findViewById(R.id.kW));
        showHideViews.add((View) findViewById(R.id.timeToCharge));
        showHideViews.add((View) findViewById(R.id.dragBar));

        selectedStation = new Station();
        userState = new UserState();
        setCar();
        setBatteryLevel();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if (selectedMarker != null) {
            if (cheapestIDs.contains(selectedMarker.getTag())) {
                selectedMarker.setIcon(BitmapDescriptorFactory.fromBitmap(
                        getBitmapFromVectorDrawable(
                                MainActivity.this, R.drawable.ic_green_pin)));
            } else {
                selectedMarker.setIcon(BitmapDescriptorFactory.fromBitmap(
                        getBitmapFromVectorDrawable(
                                MainActivity.this, R.drawable.orange_pin)));
            }
        }


        if (cheapestIDs.contains(marker.getTag())) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(
                    getBitmapFromVectorDrawable(
                            MainActivity.this, R.drawable.big_green_pin)));
        } else {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(
                    getBitmapFromVectorDrawable(
                            MainActivity.this, R.drawable.big_orange_pin)));
        }

        // Initialize the places SDK
        Places.initialize(getApplicationContext(), apiKey);
        // Create a new Places client instance
        PlacesClient placesClient = Places.createClient(this);

        // Define a Place ID.
        final String placeId = (String) marker.getTag();;

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

                Station station = nearbyStations.get(placeId);
                station.setPlace(place);

                for (View v: showHideViews) {
                    v.setVisibility(View.VISIBLE);
                }

                TextView textView = (TextView) findViewById(R.id.stationName);
                textView.setText(place.getName());

                textView = (TextView) findViewById(R.id.address);
                textView.setText(place.getAddress());

                textView = (TextView) findViewById(R.id.availability);
                System.out.println(place.getOpeningHours());
                if (place.isOpen() != null) {
                    if (place.isOpen()) {
                        textView.setText("1 Charger Available");
                        textView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
                        station.available = 1;
                    } else {
                        textView.setText("No Chargers Available");
                        textView.setTextColor(Color.RED);
                        station.available = 0;
                    }
                } else {
                    if(Math.random() < 0.5) {
                        textView.setText("1 Charger Available");
                        textView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
                        station.available = 1;
                    } else {
                        textView.setText("No Chargers Available");
                        textView.setTextColor(Color.RED);
                        station.available = 0;
                    }
                }

                //Calculations

                //distance
                double distance = DistanceCalculator.distance(mLastKnownLocation.getLatitude(),
                        mLastKnownLocation.getLongitude(), station.latitude, station.longitude, "M");
                station.distance = distance;

                //time to get there
                double speed = 15; //miles per hour
                double time = distance/speed *60.0;
                station.minToGetThere = (int)time;
                textView = (TextView) findViewById(R.id.minToGetThere);
                textView.setText(String.format("%.0f MIN", time));

                //price
                int price;
                if (cheapestIDs.contains(placeId)){
                    price = 11;
                } else {
                    price = getRandomNumberInRange(12, 15);
                }
                station.price = price;
                textView = (TextView) findViewById(R.id.price);
                textView.setText(String.format("$0.%d per kWh", price));

                // wattage
                int wattage;
                if (place.getName().contains("Tesla")) {
                    wattage = 50;
                    // time to charge
                    double chargeHours = ((double) userState.car.maxBattery - (double)  userState.batteryLevel) / (double) userState.car.chargeRate50;
                    double chargeMinutes = (chargeHours * 60.0) % 60;

                    textView = (TextView) findViewById(R.id.timeToCharge);
                    textView.setText(String.format("%.0fH %.0fM to full charge", chargeHours, chargeMinutes));

                    station.chargeHours = chargeHours;
                    station.chargeMins = chargeMinutes;

                } else {
                    wattage = 22;
                    // time to charge
                    double chargeHours = ((double) userState.car.maxBattery - (double)  userState.batteryLevel) / (double) userState.car.chargeRate22;
                    double chargeMinutes = (chargeHours * 60.0) % 60;

                    textView = (TextView) findViewById(R.id.timeToCharge);
                    textView.setText(String.format("%.0fH %.0fM to full charge", chargeHours, chargeMinutes));

                    station.chargeHours = chargeHours;
                    station.chargeMins = chargeMinutes;
                }
                station.wattage = wattage;
                textView = (TextView) findViewById(R.id.kW);
                textView.setText(String.format("%d kW", wattage));

                nearbyStations.put(station.placeID, station);
                //System.out.println(station.place.getName());
                //System.out.println(nearbyStations.get(placeId).place.getName());
                selectedStation = station;

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
        }

        );


        selectedMarker = marker;
        return false;
    }



    void getNearbyStations(double latitude, double longitude) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url =String.format("https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=%f,%f&" +
                "radius=6000&" +
                "keyword=electric vehicle charging charger&" +
                "key=AIzaSyBPNvmHksoWV2yEJDKc4e00LSheQQrcgTo",
                latitude, longitude);
        System.out.println(url);

        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        //System.out.println(response);

                        try {
                            JSONArray jsonArray = response.getJSONArray("results");
                            ArrayList<Integer> cheapest = new ArrayList<>(3);
                            cheapest.add(getRandomNumberInRange(0, jsonArray.length()));

                            int j;
                            do {
                                j = getRandomNumberInRange(0, jsonArray.length());
                            }while (cheapest.contains(j));
                            cheapest.add(j);

                            do {
                                j = getRandomNumberInRange(0, jsonArray.length());
                            }while (cheapest.contains(j));
                            cheapest.add(j);

                            for (int  i = 0; i < jsonArray.length(); i++) {
                                JSONObject result = jsonArray.getJSONObject(i);
                                String placeID = result.getString("place_id");
                                JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");
                                Station station = new Station(placeID, lat, lng);
                                nearbyStations.put(placeID, station);

                                if (cheapest.contains(i)) {
                                    cheapestIDs.add(placeID);
                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(station.latitude, station.longitude))
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    getBitmapFromVectorDrawable(
                                                            MainActivity.this, R.drawable.ic_green_pin))));
                                    marker.setTag(placeID);
                                } else {
                                    Marker marker = mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(station.latitude, station.longitude))
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    getBitmapFromVectorDrawable(
                                                            MainActivity.this, R.drawable.orange_pin))));
                                    marker.setTag(placeID);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }



                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("That didn't work!");
                error.printStackTrace();
                //textView.setText("That didn't work!");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation( ) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        if (searchLocationMarker != null) {
            searchLocationMarker.remove();
        }

        mFusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);
        //Task locationResult = mFusedLocationProviderClient.getLastLocation();
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            System.out.println("Current location retrieved successfully!");
                            mLastKnownLocation = (Location) task.getResult(); // location?
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            searchLocationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()))
                                    .title("Current Location")
                                    .icon(BitmapDescriptorFactory.fromBitmap(
                                            getBitmapFromVectorDrawable(
                                                    MainActivity.this, R.drawable.ic_current_location_marker))));
                            getNearbyStations(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());

                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            searchLocationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(mDefaultLocation)
                                    .title("Current Location")
                                    .icon(BitmapDescriptorFactory.fromBitmap(
                                            getBitmapFromVectorDrawable(
                                                    MainActivity.this, R.drawable.ic_current_location_marker))));
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }

    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    void setCar() {
        TextView textView = (TextView)findViewById(R.id.carName);
        textView.setText(userState.car.carName); //set text for text view
    }

    void setBatteryLevel() {
        TextView textView = (TextView)findViewById(R.id.batteryMiles);
        textView.setText(String.format("%d", userState.batteryLevel)); //set text for text view
    }


    private static int getRandomNumberInRange(int min, int max) { // max is exclusive

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }


    public void selectVehicle(View view) {
        View v = (View) findViewById(R.id.popup);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.shadowOverlay);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.SelectYourVehicle);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.RenaultButton);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.NissanLeafButton);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.volkswagenButton);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.closeButton);
        v.setVisibility(View.VISIBLE);

        v = (View) findViewById(R.id.saveVehicle);
        v.setVisibility(View.VISIBLE);

    }

    public void saveVehicle(View view) {
        View v = (View) findViewById(R.id.popup);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.shadowOverlay);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.SelectYourVehicle);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.RenaultButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.NissanLeafButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.volkswagenButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.closeButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.saveVehicle);
        v.setVisibility(View.INVISIBLE);

        userState.car.changeVehicle(lastSelectedCarCode);
        TextView textView = (TextView)findViewById(R.id.carName);
        textView.setText(userState.car.carName);
    }


    public void cancelVehicleChange(View view) {
        View v = (View) findViewById(R.id.popup);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.shadowOverlay);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.SelectYourVehicle);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.RenaultButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.NissanLeafButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.volkswagenButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.closeButton);
        v.setVisibility(View.INVISIBLE);

        v = (View) findViewById(R.id.saveVehicle);
        v.setVisibility(View.INVISIBLE);

    }


    public void updateCurrentCarSelection (View view) {


        Button button = (Button) view;
        button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.darkGreen));
        button.setTextColor(Color.WHITE);
        button.setTypeface(button.getTypeface(), Typeface.BOLD);
        lastSelectedCarCode = Integer.valueOf((String) button.getTag());

        if (button.getTag().equals("0")) {
            Drawable img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.nissan_logo);
            button.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);

            // Deselect other buttons
            Button but = (Button) findViewById(R.id.RenaultButton);
            img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.renault_logo_white);
            but.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);
            but.setBackgroundColor(Color.WHITE);
            but.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
            but.setTypeface(null, Typeface.NORMAL);

            but = (Button) findViewById(R.id.volkswagenButton);
            img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.volkswagen_logo_white);
            but.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);
            but.setBackgroundColor(Color.WHITE);
            but.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
            but.setTypeface(null, Typeface.NORMAL);

        } else if (button.getTag().equals("1")) {
            Drawable img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.renault_logo_green);
            button.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);

            // Deselect other buttons
            Button but = (Button) findViewById(R.id.NissanLeafButton);
            img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.nissan_logo_white);
            but.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);
            but.setBackgroundColor(Color.WHITE);
            but.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
            but.setTypeface(null, Typeface.NORMAL);

            but = (Button) findViewById(R.id.volkswagenButton);
            img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.volkswagen_logo_white);
            but.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);
            but.setBackgroundColor(Color.WHITE);
            but.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
            but.setTypeface(null, Typeface.NORMAL);

        } else {
            Drawable img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.volkswagen_logo_green);
            button.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);

            // Deselect other buttons
            Button but = (Button) findViewById(R.id.NissanLeafButton);
            img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.nissan_logo_white);
            but.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);
            but.setBackgroundColor(Color.WHITE);
            but.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
            but.setTypeface(null, Typeface.NORMAL);

            but = (Button) findViewById(R.id.RenaultButton);
            img = ContextCompat.getDrawable(MainActivity.this, R.mipmap.renault_logo_white);
            but.setCompoundDrawablesWithIntrinsicBounds( null, img, null, null);
            but.setBackgroundColor(Color.WHITE);
            but.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textColor));
            but.setTypeface(null, Typeface.NORMAL);

        }
    }


    public void changeLocation(View view) {

        EditText mEdit   = (EditText)findViewById(R.id.SearchbyAddress);
        String input = mEdit.getText().toString();

        System.out.println(input);

        if (input.toLowerCase().equals("current location")) {
            getDeviceLocation();
        } else {

            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?" +
                    "address=%s&" +
                    "key=AIzaSyBPNvmHksoWV2yEJDKc4e00LSheQQrcgTo", input);
            System.out.println(url);

            // Request a string response from the provided URL.
            JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {
                                JSONArray jsonArray = response.getJSONArray("results");

                                JSONObject result = jsonArray.getJSONObject(0);

                                JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lat, lng), DEFAULT_ZOOM));

                                searchLocationMarker.remove();
                                searchLocationMarker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(lat, lng))
                                        .title("Current Location")
                                        .icon(BitmapDescriptorFactory.fromBitmap(
                                                getBitmapFromVectorDrawable(
                                                        MainActivity.this, R.drawable.ic_current_location_marker))));

                                getNearbyStations(lat, lng);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println("Failed to fetch long lat!");
                    error.printStackTrace();
                    //textView.setText("That didn't work!");
                }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        }
    }

    @Override
    public void onMapClick(LatLng point) {

        for (View v: showHideViews) {
            v.setVisibility(View.INVISIBLE);
        }

        if (selectedMarker != null) {
            if (cheapestIDs.contains(selectedMarker.getTag())) {
                selectedMarker.setIcon(BitmapDescriptorFactory.fromBitmap(
                        getBitmapFromVectorDrawable(
                                MainActivity.this, R.drawable.ic_green_pin)));
            } else {
                selectedMarker.setIcon(BitmapDescriptorFactory.fromBitmap(
                        getBitmapFromVectorDrawable(
                                MainActivity.this, R.drawable.orange_pin)));
            }
        }

        selectedMarker = null;

    }


// Methods to go to new Activity


    // Go to Station View
    public void goToStationView(View view) {
        Intent intent = new Intent(this, StationDetailsActivity.class);
        intent.putExtra("Station", selectedStation);
        startActivityForResult(intent, 1);
    }




}

