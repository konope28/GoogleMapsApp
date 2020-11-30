package com.example.lab6;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener, SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Button clearButton;
    private TextView sensorTextView;
    private FloatingActionButton exitActionButton;
    private FloatingActionButton sensorActionButton;
    List<Marker> markerList;

    private boolean buttonsFlag;
    private boolean sensorFlag;
    int height;
    ObjectAnimator exitAnimator;
    ObjectAnimator sensorAnimator;

    private ConstraintLayout mainContainer;
    private final String LATLNG_JSON_FILE = "latlngs.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        mainContainer = findViewById(R.id.mainView);
        clearButton = findViewById(R.id.clear_memory_button);
        sensorTextView = findViewById(R.id.sensor_text_view);
        sensorActionButton = findViewById(R.id.sensor_action_button);
        exitActionButton = findViewById(R.id.exit_sensor_button);
        markerList = new ArrayList<>();
        buttonsFlag = false;

        mainContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                height = mainContainer.getHeight();
                mainContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(mSensor != null){
            mSensorManager.unregisterListener(this, mSensor);
        }
        storeJson();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSensor != null){
            mSensorManager.registerListener(this, mSensor, 100000);
        }
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        sensorTextView.setVisibility(View.INVISIBLE);
        exitActionButton.setVisibility(View.INVISIBLE);
        sensorActionButton.setVisibility(View.INVISIBLE);

        restoreJson();
    }

    @Override
    public void onMapLoaded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        exitActionButton.setVisibility(View.VISIBLE);
        sensorActionButton.setVisibility(View.VISIBLE);
        exitAnimator = ObjectAnimator.ofFloat(exitActionButton, View.Y, height);
        exitAnimator.setDuration(0).start();

        sensorAnimator = ObjectAnimator.ofFloat(sensorActionButton, View.Y, height);
        sensorAnimator.setDuration(0).start();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPosition = mMap.getCameraPosition();

        if(cameraPosition.zoom < 14f){
            mMap.moveCamera(CameraUpdateFactory.zoomTo(1f));
        }

        if(!buttonsFlag){
            buttonsFlag = true;

            exitAnimator = ObjectAnimator.ofFloat(exitActionButton, View.Y, height-(int)(height/6));
            exitAnimator.setDuration(200).start();

            sensorAnimator = ObjectAnimator.ofFloat(sensorActionButton, View.Y, height-(int)(height/6));
            sensorAnimator.setDuration(200).start();
        }

        return false;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f)",latLng.latitude, latLng.longitude)));
        markerList.add(marker);
    }


    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }
    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    public void onClearMemoryClick(View view) throws IOException {
        onExitFABClick(findViewById(R.id.mainView));
        mMap.clear();
        markerList.clear();
        storeJson();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onSensorFABClick(View view) {
        if(!sensorFlag){
            sensorFlag = true;
            sensorTextView.setVisibility(View.VISIBLE);
        }
        else{
            sensorFlag = false;
            sensorTextView.setVisibility(View.INVISIBLE);
        }
    }

    public void onExitFABClick(View view) {
        exitAnimator = ObjectAnimator.ofFloat(exitActionButton, View.Y, height);
        exitAnimator.setDuration(200).start();

        sensorAnimator = ObjectAnimator.ofFloat(sensorActionButton, View.Y, height);
        sensorAnimator.setDuration(200).start();
        buttonsFlag = false;
        sensorFlag = false;
        sensorTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getString(R.string.acceleration)).append("\n")
                .append(String.format("x: %.4f  y: %.4f",event.values[0], event.values[1]));
        sensorTextView.setText(stringBuilder.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



    private void storeJson(){

        List <LatLng> latLngs = new ArrayList<>();
        int i;
        for (i=0; i< markerList.size(); i++){
            latLngs.add(new LatLng(markerList.get(i).getPosition().latitude, markerList.get(i).getPosition().longitude));
        }

        Gson gson = new Gson();
        String listJson = gson.toJson(latLngs);
        try {
            FileOutputStream fileOutputStream = openFileOutput(LATLNG_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(fileOutputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreJson(){
        int BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        try {
            FileInputStream fileInputStream = openFileInput(LATLNG_JSON_FILE);
            FileReader reader = new FileReader(fileInputStream.getFD());
            char [] buf = new char[BUFFER_SIZE];
            StringBuilder builder = new StringBuilder();
            int n;
            while((n = reader.read(buf)) >= 0 ){
                String temp = String.valueOf(buf);

                String substring = (n < BUFFER_SIZE) ? temp.substring(0,n) : temp;
                builder.append(substring);
            }
            reader.close();
            fileInputStream.close();
            String jsonList = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>(){}.getType();
            List<LatLng> o = gson.fromJson(jsonList, collectionType);
            if(mMap != null && o != null){
                for(LatLng latLng :o){

                    Marker marker = mMap.addMarker(new MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .alpha(0.8f)
                            .title(String.format("Position: (%.2f, %.2f)",latLng.latitude, latLng.longitude)));
                    markerList.add(marker);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

