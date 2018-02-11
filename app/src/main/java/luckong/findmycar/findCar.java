package luckong.findmycar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class findCar extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng userLocation;
    private LatLng carLocation;
    private Polyline line;
    private TextView calculatedDistance;

    LocationManager lm;
    LocationListener mLocationListener;
    double savedVehicleLatitude;
    double savedVehicleLongitude;

    //Sensor variables
    private SensorManager sensorManager;
    float[] mGravity;
    float[] mMagnetic;
    float[] mRotationMatrix = new float[9];
    private float mFacing = Float.NaN;
    Sensor accelerometer;
    Sensor magnetometer;

    Criteria criteria;
    String provider;
    Location location;
    SharedPreferences vehiclePref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_car);


        //Get car coordinates in shared pref
        vehiclePref = getApplicationContext().getSharedPreferences(TrackingReceiver.SAVED_VEHICLE_PREF, 0);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        //Button Initializations
        Button navigationButton = (Button)findViewById(R.id.navigation);
        Button walkingDirectionsButton = (Button)findViewById(R.id.directions);
        Button stopNavigation = (Button)findViewById(R.id.stopNavigation);

        navigationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Start Navigation
                navigateUserToParking(mMap);

                //Remove Navigation Button
                View b = findViewById(R.id.navigation);
                b.setVisibility(View.GONE);

                //Make Stop Navigation Button visible
                View s = findViewById(R.id.stopNavigation);
                s.setVisibility(View.VISIBLE);

                sensorManager.registerListener(rotationListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
                sensorManager.registerListener(rotationListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        stopNavigation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                try {
                    lm.removeUpdates(mLocationListener);
                    mLocationListener = null;
                } catch (SecurityException e) {
                    System.out.println("EXCEPTION E: NO LOCATION PERMISSIONS ON STOP NAVIGATION BUTTON"); // lets the user know there is a problem with the gps
                }

                //remove polyline
                line.remove();

                showInitialLocations(mMap);

                //Make Stop Navigation Button visible
                View s = findViewById(R.id.stopNavigation);
                s.setVisibility(View.GONE);

                //Remove Navigation Button
                View b = findViewById(R.id.navigation);
                b.setVisibility(View.VISIBLE);

                //Unregister rotation listener
                sensorManager.unregisterListener(rotationListener);


            }
        });

        walkingDirectionsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(carLocation != null && userLocation != null){
                    String uri = "http://maps.google.com/maps?saddr="+userLocation.latitude+","+userLocation.longitude+"&daddr="+carLocation.latitude+","+carLocation.longitude+"&mode=walking";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    findCar.this.startActivity(intent);
                }else{
                    Toast.makeText(getApplicationContext(), "Unable to parse URL", Toast.LENGTH_SHORT).show();
                }

            }
        });


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    @Override
    protected void onResume(){
        super.onResume();

        //Update Map Camera
        sensorManager.registerListener(rotationListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(rotationListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(rotationListener);
    }

    private SensorEventListener rotationListener = new SensorEventListener(){
        public void onAccuracyChanged(Sensor sensor, int accuracy){}
        public void onSensorChanged(SensorEvent event){

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                mGravity = event.values;
            }
            if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            {
                mMagnetic = event.values;
            }

            if (mGravity != null && mMagnetic != null){
                if(SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity, mMagnetic)){
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(mRotationMatrix, orientation);
                    mFacing = orientation[0];

                    mFacing = mFacing*(180/3.14159f);

                    //TODO: Stop remaking LocationManager objects every Azimuth change
                    lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    userLocation = new LatLng(latitude, longitude);
                    CameraPosition userPosition = new CameraPosition.Builder().target(userLocation).bearing(mFacing).zoom(21).build();
                    System.out.println("mFacing RUNNING");
                    if(mMap != null){
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(userPosition),200, null);
                    }else{
                        System.out.println("mMAP IS NULL, CANT UPDATE CAMERA");
                    }


                }
            }
        }

    };


    public void navigateUserToParking(GoogleMap googleMap){
        mMap = googleMap;

        long REQUEST_LOCATION_MIN_TIME = 1000;
        float REQUEST_LOCATION_MIN_DISTANCE = 0;

        calculatedDistance = (TextView) findViewById(R.id.calculatedDistance);


        //User's Location
        lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                line.remove();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                userLocation = new LatLng(latitude, longitude);
                drawPolyline();

                //Calculate distance between user and parking location and display it
                double calculatedResult = 0;
                calculatedResult = measureDistance(latitude, longitude, savedVehicleLatitude, savedVehicleLongitude);
                double resultRounded = round(calculatedResult, 2);
                String distance = String.valueOf(resultRounded);
                calculatedDistance.setText(getString(R.string.calculatedDistance, distance + "ft Away"));

                if(Float.isNaN(mFacing)){
                    if(location.hasBearing()){
                        System.out.println("USING LOCATION FOR BEARING");
                        CameraPosition userPosition = new CameraPosition.Builder().target(userLocation).bearing(location.getBearing()).zoom(21).build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(userPosition),1000, null);
                    }else{
                        System.out.println("NO BEARING");
                    }
                }


                //TODO: unregister listener


            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        //Create locationmanager and location objects
        lm.requestLocationUpdates(provider, REQUEST_LOCATION_MIN_TIME, REQUEST_LOCATION_MIN_DISTANCE, mLocationListener);
        location = lm.getLastKnownLocation(provider);
        while(location == null){
            location = lm.getLastKnownLocation(provider);
        }


        if(savedVehicleLatitude > 90 || savedVehicleLongitude > 180) { //If the location does not exist, output that no location was saved and stop process
            System.out.println("EXITING");
            Toast.makeText(getApplicationContext(), "You haven't parked yet!", Toast.LENGTH_LONG).show();
            sensorManager.unregisterListener(rotationListener);
            lm.removeUpdates(mLocationListener);
            finish();
        } else { //If location exists, find location
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            // Add a marker at car location and move the camera
            Toast.makeText(getApplicationContext(), "Parking Location Found!", Toast.LENGTH_LONG).show();

            carLocation = new LatLng(savedVehicleLatitude, savedVehicleLongitude);
            //TODO:  INCREASE carLocation ACCURACY to a higher percent
            userLocation = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(carLocation).title("Car Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 21.0f));
            CameraUpdateFactory.zoomTo(20);
            mMap.setMyLocationEnabled(true);
            drawPolyline();
        }
    }

    //Shows the user's current location and car location upon map startup
    public void showInitialLocations(GoogleMap googleMap){
        mMap = googleMap;

        long REQUEST_LOCATION_MIN_TIME = 1000;
        float REQUEST_LOCATION_MIN_DISTANCE = 0;

        //Set purple distance text in the top left corner to nothing
        calculatedDistance = (TextView) findViewById(R.id.calculatedDistance);
        calculatedDistance.setText(getString(R.string.calculatedDistance, ""));

        //User's Location
        lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        //Create locationmanager and location objects
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = lm.getBestProvider(criteria, false);
        lm.requestLocationUpdates(provider, REQUEST_LOCATION_MIN_TIME, REQUEST_LOCATION_MIN_DISTANCE, mLocationListener);
        location = lm.getLastKnownLocation(provider);
        while(location == null){
            location = lm.getLastKnownLocation(provider);
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        userLocation = new LatLng(latitude, longitude);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        savedVehicleLatitude = getDouble(vehiclePref, "latitude", 91);
        savedVehicleLongitude = getDouble(vehiclePref, "longitude", 181);

        carLocation = new LatLng(savedVehicleLatitude, savedVehicleLongitude);

        //the include method will calculate the min and max bound.
        builder.include(carLocation);
        builder.include(userLocation);

        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        mMap.addMarker(new MarkerOptions().position(carLocation).title("Car Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker)));
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        mMap.setMyLocationEnabled(true);
        mMap.animateCamera(cu);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady( GoogleMap googleMap) {
        mMap = googleMap;

        lm = (LocationManager)findCar.this.getSystemService(Context.LOCATION_SERVICE);
        ContextCompat.checkSelfPermission(findCar.this, Manifest.permission.ACCESS_FINE_LOCATION);

        //Check if GPS is on
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            showInitialLocations(mMap);
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(findCar.this);
            builder.setTitle("GPS Not Detected");  // GPS not found
            builder.setMessage("Enable GPS?"); // Want to enable?

            //YES button will let user turn on Location Services, then proceeds to save parking location
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    findCar.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    try {
                        showInitialLocations(mMap);
                    }catch(Exception e){
                        Toast.makeText(findCar.this, "GPS not enabled, unable to navigate to parking", Toast.LENGTH_LONG).show();
                    }

                }
            });

            //NO button will result in an error message with no parking location saved
            builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(findCar.this, "GPS not enabled, unable to navigate to parking", Toast.LENGTH_LONG).show();
                }
            });
            builder.create().show();
        }



    }

    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    Polyline drawPolyline(){
        line = mMap.addPolyline(new PolylineOptions().add(userLocation, carLocation).width(10).color(Color.MAGENTA));
        return line;
    }

    double measureDistance(double lat1, double long1, double lat2, double long2){
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(long2 - long1);
        double distance = Math.acos(Math.sin(lat1Rad)*Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)) * 6371; //Great-Circle Distance Formula
        return Math.round(distance*3280.84);

    }

    public static double round(double value, int scale) {
        return Math.round(value * Math.pow(10, scale)) / Math.pow(10, scale);
    }

}
