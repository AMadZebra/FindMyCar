package luckong.findmycar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;

/**
 * Created by Luc on 8/2/2016.
 */
public class TrackingReceiver extends BroadcastReceiver{
    public static final String SAVED_VEHICLE_PREF = "VehicleLocation";
    private boolean accuracyCheck = false;
    private float accuracy = 100;
    private static int locationChangeCount;


    public void saveParkingCoordinates(final Context context){

        long REQUEST_LOCATION_MIN_TIME = 1000;
        float REQUEST_LOCATION_MIN_DISTANCE = 0;

        //Find phone location
        final LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        //Create SharedPreferences file that will be used to store user's saved parking coordinates,
        //this information will be shared with other the other classes
        SharedPreferences vehiclePref = context.getSharedPreferences(SAVED_VEHICLE_PREF, 0);
        final SharedPreferences.Editor vehiclePrefEdit = vehiclePref.edit();

        final LocationListener mLocationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                float newAccuracy = location.getAccuracy();

                //Get whether or not location changed has permission to get new coordinates
                SharedPreferences pref = context.getSharedPreferences("ContinueSaving", 0);
                boolean continueLocationChanged = pref.getBoolean("continueLocationChanged", false);


                //Improve user's parking location coordinates
                if(newAccuracy < accuracy && continueLocationChanged) {
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



        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); //FINE ACCURACY location requirement
        String provider = lm.getBestProvider(criteria, false); //Get the provider that meets the ACCURACY_FINE requirement, whether or not it is currently enabled

        System.out.println("PROVIDER IS: " + provider);

        //Using the provider found,register for location updates, mLocationListener's onLocationChanged(Location) will be called for each update
        lm.requestLocationUpdates(provider, REQUEST_LOCATION_MIN_TIME, REQUEST_LOCATION_MIN_DISTANCE, mLocationListener);
        Location location = lm.getLastKnownLocation(provider);
        while(location == null){
            location = lm.getLastKnownLocation(provider);
        }


        if(location.hasAccuracy()){
            accuracy = location.getAccuracy();
        }


        accuracyCheck = true;



        if(!accuracyCheck){
            Toast.makeText(context, "Unable to find parking location.", Toast.LENGTH_LONG).show();

        }else{

            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            //Save location for use in other Activities/Classes

            putDouble(vehiclePrefEdit, "latitude", latitude);
            putDouble(vehiclePrefEdit, "longitude", longitude);
            vehiclePrefEdit.apply();

            //Mark that location was saved
            SharedPreferences vehicleSaved = context.getSharedPreferences("VehicleSaved", 0);
            SharedPreferences.Editor editor = vehicleSaved.edit();
            editor.putBoolean("reset", false);
            editor.apply();

            Toast.makeText(context, "Parking Location Saved Successfully!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent){
        long REQUEST_LOCATION_MIN_TIME = 1000;
        float REQUEST_LOCATION_MIN_DISTANCE = 0;


        //Find phone location
        final LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        //Check if Location Services is enabled before proceeding to save user's parking location
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){

            saveParkingCoordinates(context);

        }else{ //prompt user to turn on Location Services if it is not already on

            System.out.println("NO GPS FOUND");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("GPS Not Detected");  // GPS not found
            builder.setMessage("Enable GPS?"); // Want to enable?

            //YES button will let user turn on Location Services, then proceeds to save parking location
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        saveParkingCoordinates(context);
                    }
                }
            });

            //NO button will result in an error message with no parking location saved
            builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(context, "GPS not enabled, unable to save parking location", Toast.LENGTH_LONG).show();
                }
            });
            builder.create().show();

        }


    }

    //Convert coordinate values to double
    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }


}


