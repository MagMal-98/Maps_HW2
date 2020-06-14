package com.mm.maps_hw;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;

    List<MarkerCords> markerList;
    public String JSON_FILE = "markers.json";

    static public SensorManager sensorManager;
    private Sensor mSensor;
    private TextView sensor_display;
    private FloatingActionButton record_button;
    private FloatingActionButton cancel_button;
    private boolean is_sensor_working;
    private Button clear_memory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerList = new ArrayList<>();

        // SENSOR
        sensor_display = findViewById(R.id.sensor_display);
        record_button = findViewById(R.id.record_button);
        cancel_button = findViewById(R.id.cancel_button);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = null;

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        is_sensor_working = false;

        record_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is_sensor_working = !is_sensor_working;
                start_sensor(is_sensor_working);
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensor_display.setVisibility(View.INVISIBLE);
                start_sensor(false);
                record_button.animate().translationY(120f).alpha(0f).setDuration(1000);
                cancel_button.animate().translationY(120f).alpha(0f).setDuration(1000);
            }
        });

        clear_memory = findViewById(R.id.clear_memory);

        clear_memory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                markerList.clear();
                Json_save();
            }
        });
    }

    private void Json_save() {
        Gson gson = new Gson();
        String listJson = gson.toJson(markerList);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void start_sensor(boolean is_sensor_working) {
        if(mSensor != null){

            if(is_sensor_working) {
                sensor_display.setVisibility(View.VISIBLE);
                sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }else {
                sensor_display.setVisibility(View.INVISIBLE);
                sensorManager.unregisterListener(this);
            }

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        Json_restore();
    }

    private void Json_restore() {
        FileInputStream inputStream;
        Gson gson = new Gson();
        String readJson;

        try{
            inputStream = openFileInput(JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[10000];
            int n;
            StringBuilder builder = new StringBuilder();
            while((n = reader.read(buf)) >= 0){
                String tmp = String.valueOf(buf);
                String substring = (n<10000) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<MarkerCords>>() { }.getType();
            List<MarkerCords> o = gson.fromJson(readJson, collectionType);

            if(o != null){
                for(MarkerCords mc : o){
                    markerList.add(mc);
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(mc.mLat, mc.mLon))
                            .alpha(0.8f)
                            .title(String.format("Position: (%.2f, %.2f)", mc.mLat, mc.mLon)));
                }
            }

        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback(){
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult!=null){
                    if(gpsMarker!=null)
                        gpsMarker.remove();
                    /*Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title("Current Location"));*/
                }
            }
        };
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
       /* Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null){
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });*/

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopLocationUpdates();
        if(mSensor != null)
            MapsActivity.sensorManager.unregisterListener(this, mSensor);    }

    private void stopLocationUpdates(){
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        /*float distance = 0f;

        if (markerList.size()>0){
            Marker lastMarker = markerList.get(markerList.size()-1);
            float [] tmpDis = new float[3];
            Location.distanceBetween(lastMarker.getPosition().latitude, lastMarker.getPosition().longitude,
                    latLng.latitude, latLng.longitude, tmpDis);
            distance = tmpDis[0];

            PolylineOptions rectOptions = new PolylineOptions()
                    .add(lastMarker.getPosition())
                    .add(latLng)
                    .width(10)
                    .color(Color.BLUE);
            mMap.addPolyline(rectOptions);
        }*/

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                //.icon(bitmapDescriptorFromVector(this, R.drawable.marker))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));

        markerList.add(new MarkerCords(latLng.latitude, latLng.longitude));

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        record_button.animate().translationY(0f).alpha(1f).setDuration(1000);
        cancel_button.animate().translationY(0f).alpha(1f).setDuration(1000);
        return false;
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String display = "Acceleration:\nx:"+event.values[0]+" y:"+event.values[1]+" z:"+event.values[2];
        sensor_display.setText(display);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        Json_save();
        super.onDestroy();
    }
}
