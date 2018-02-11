package luckong.findmycar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.widget.ImageButton;
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {
    //Defining TrackingReceiver, activates when power source disconnects from phone
    public static BroadcastReceiver br;
    IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
    private float accuracy = 100;
    public static final String SAVED_VEHICLE_PREF = "VehicleLocation";
    private static int locationChangeCount;
    public static boolean reset;
    public static SharedPreferences vehicleSaved;
    private LocationManager lm;
    LocationListener mLocationListener;

    //TODO: Improve Help Slides
    //TODO: Get launcher icons for all densities
    //TODO: Commit to github
    //TODO: Disable ads button for free
    //TODO: Final testing before publish

    //Save the user's coordinates (If they requested to do so manually via the home menu)
    private void saveCoordinates(){

        lm = (LocationManager)MenuActivity.this.getSystemService(Context.LOCATION_SERVICE);

        //Create SharedPreferences file that will be used to store user's saved parking coordinates,
        //this information will be shared with other the other classes
        SharedPreferences vehiclePref = MenuActivity.this.getSharedPreferences(SAVED_VEHICLE_PREF, 0);
        final SharedPreferences.Editor vehiclePrefEdit = vehiclePref.edit();

        mLocationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                float newAccuracy = location.getAccuracy();

                //Improve user's parking location coordinates
                if(newAccuracy < accuracy) {
                    accuracy = newAccuracy;
                    System.out.println("ACCURACY IMPROVED");
                    System.out.println("ACCURACY IS : " + newAccuracy);

                    //Get new latitude/longitude
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    //Store new coordinates in SharedPrefs file
                    putDouble(vehiclePrefEdit, "latitude", latitude);
                    putDouble(vehiclePrefEdit, "longitude", longitude);
                    vehiclePrefEdit.apply();

                    locationChangeCount++;

                    System.out.println("Location change count: " + locationChangeCount);


                    //TODO: Set time limit for improved parking coordinate accuracy
                }


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

        ContextCompat.checkSelfPermission(MenuActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

        lm.requestLocationUpdates(lm.GPS_PROVIDER, 1000, 0, mLocationListener);
        Location location = lm.getLastKnownLocation(lm.GPS_PROVIDER);
        while(location == null){
            location = lm.getLastKnownLocation(lm.GPS_PROVIDER);
        }

        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        //Save location for use in other Activities/Classes

        putDouble(vehiclePrefEdit, "latitude", latitude);
        putDouble(vehiclePrefEdit, "longitude", longitude);
        vehiclePrefEdit.apply();

        Toast.makeText(MenuActivity.this, "Parking Location Saved Successfully!", Toast.LENGTH_LONG).show();

    }

    private void turnOnGPS(){

        LocationManager lm = (LocationManager)MenuActivity.this.getSystemService(Context.LOCATION_SERVICE);

        //Check if GPS is enabled, if not then prompt user to turn it on
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            System.out.println("NO GPS FOUND");
            AlertDialog.Builder builder = new AlertDialog.Builder(MenuActivity.this);
            builder.setTitle("GPS Not Detected");  // GPS not found
            builder.setMessage("Enable GPS?"); // Want to enable?
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    MenuActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            builder.setNegativeButton("No", null);
            builder.create().show();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        int permissionGPS=123;


        //allow TrackingReceiver's location lister to update
        SharedPreferences pref = this.getSharedPreferences("ContinueSaving", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("continueLocationChanged", true);
        editor.commit();


        br = new TrackingReceiver();

        //Get whether or not location is saved
        vehicleSaved = getSharedPreferences("VehicleSaved", 0);



        //Ask user for location services
        ActivityCompat.requestPermissions(this,new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, permissionGPS);

        turnOnGPS(); //Check if GPS is on


        //Defining Buttons on home menu
        ImageButton findMyCar = (ImageButton)findViewById(R.id.find_car_button);
        ImageButton saveManually = (ImageButton)findViewById(R.id.save_manually_button);
        ImageButton reset = (ImageButton)findViewById(R.id.reset_button);
        ImageButton help = (ImageButton)findViewById(R.id.help_button);


        help.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(getApplicationContext(), IntroActivity.class);
                startActivity(intent);
            }
        });


        //create new activity for Find Car map to lead user to their car
        findMyCar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Check if location was saved
                MenuActivity.reset = vehicleSaved.getBoolean("reset", true);
                if(MenuActivity.reset == false) {

                    unregisterReceiver(br);
                    startActivity(new Intent(MenuActivity.this, findCar.class));
                }else{
                    Toast.makeText(MenuActivity.this, "You haven't parked yet!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Reset button will forget previous saved parking location
        reset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Save that vehicle location is reset
                SharedPreferences.Editor editor = vehicleSaved.edit();
                editor.putBoolean("reset", true);
                editor.commit();
                MenuActivity.reset = true;

                //Clear coordinates
                SharedPreferences vehiclePref = getSharedPreferences(TrackingReceiver.SAVED_VEHICLE_PREF, 0);
                if (vehiclePref.contains("latitude")){
                    vehiclePref.edit().clear().commit();
                }
                Toast.makeText(getApplicationContext(), "Car Location Reset", Toast.LENGTH_LONG).show();
            }
        });

        //save user's coordinates manually
        saveManually.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Check if GPS is enabled, if not then prompt user to turn it on
                lm = (LocationManager)MenuActivity.this.getSystemService(Context.LOCATION_SERVICE);
                MenuActivity.reset = false;
                SharedPreferences.Editor editor = vehicleSaved.edit();
                editor.putBoolean("reset", false);
                editor.commit();

                //Check if GPS is enabled, if not then prompt user to turn it on
                if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    System.out.println("NO GPS FOUND");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MenuActivity.this);
                    builder.setTitle("GPS Not Detected");  // GPS not found
                    builder.setMessage("Enable GPS?"); // Want to enable?
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MenuActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                saveCoordinates();
                            }
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.create().show();
                }else{
                    saveCoordinates();
                }

            }
        });

    }



    public void onDestroy()
    {
        super.onDestroy();

        //tell trackingReceiver's location listener to stop updating
        SharedPreferences pref = this.getSharedPreferences("ContinueSaving", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("continueLocationChanged", false);
        editor.commit();



        /*Unregister TrackingReceiver when app is closed,
        will prevent app from saving user's gps location if
        they unplug their phone from a power source
        when the app is closed*/
        //In try block because br could've already been unregistered on FindCar button press
        try{
            unregisterReceiver(br);
        }catch(Exception e){

        }



    }

    public void onStop(){
        super.onStop();

        try {
            if(mLocationListener != null){
                lm.removeUpdates(mLocationListener);
            }
            mLocationListener = null;
        } catch (SecurityException e) {

        }
    }

    public void onResume(){
        super.onResume();

        //Registering TrackingReceiver
        this.registerReceiver(br, filter);

        //Determine if the user is opening the app for the first time, and show Help instructions if so
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean("firstTime", false);
        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("firstTime", Boolean.TRUE);
            edit.commit();
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
        }

    }

    //Convert coordinate values to double
    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }


}
