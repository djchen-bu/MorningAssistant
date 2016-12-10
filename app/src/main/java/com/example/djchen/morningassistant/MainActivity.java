package com.example.djchen.morningassistant;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.net.Uri;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static com.example.djchen.morningassistant.R.id.alarm_off;
import static java.lang.Math.abs;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // make alarm manager
    AlarmManager alarm_manager;
    TimePicker alarm_timepicker;
    TextView update_text;
    Context contex; //ATTEMPT ONE
    PendingIntent pending_intent;

    //FOR LOCATION JUNK
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location myLastLoc;
    private GoogleApiClient myGoogleAPIClient;
    private boolean RequestLocUpdates = false;
    private LocationRequest myLocReq;
    private static int FASTEST_INTERVAL = 500;
    private static int DISPLACEMENT = 5;
    private TextView lblLocation;
    private double currentLatitude;
    private double currentLongitude;
    private double locDisplacement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.contex = this;

        if(myGoogleAPIClient == null) {
            myGoogleAPIClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        //Initialize alarm manager

        alarm_manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //Initialize timepicker
        alarm_timepicker = (TimePicker) findViewById(R.id.timepicker);
        //initialize the text update box
        update_text = (TextView) findViewById(R.id.update_text);

        // create an instance of a calender
        final Calendar calender = Calendar.getInstance();

        //Create Intent class
        final Intent my_intent = new Intent(this.contex, Alarm_Receiver.class);


        //Initialize start Buttons
        final Button alarm_on = (Button) findViewById(R.id.alarm_on);

        //create an onclick listener to start alarm
        alarm_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calender.set(Calendar.HOUR_OF_DAY, alarm_timepicker.getHour());
                calender.set(Calendar.MINUTE, alarm_timepicker.getMinute());

                int hour = alarm_timepicker.getHour();
                int minute = alarm_timepicker.getMinute();
                // Convert int values to strings
                if (hour > 12) {
                    hour = hour - 12;

                }

                String hour_string = String.valueOf(hour);
                String minute_string = String.valueOf(minute);
                //method that changes the update text textbox
                if (minute < 10) {
                    minute_string = "0" + String.valueOf(minute);
                }


                set_alarm_text("Alarm set to: " + hour_string + ":" + minute_string);

                //boolean to tell ringtone you pressed buttn
                my_intent.putExtra("extra", "On");

                pending_intent = PendingIntent.getBroadcast(MainActivity.this, 0, my_intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // set alarm manager
                alarm_manager.set(AlarmManager.RTC_WAKEUP, calender.getTimeInMillis(), pending_intent);

            }


        });

        //Initialize stop Button
        final Button alarm_off = (Button) findViewById(R.id.alarm_off);

        //create an onclick listener to stop alarm

        alarm_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //method that changes the update text textbox

                my_intent.putExtra("extra", "Off");

                set_alarm_text("Alarm off");

                alarm_manager.cancel(pending_intent);

                //tells clock off was pressed

                //stop the Ringtone

                sendBroadcast(my_intent);

            }
        });

        double initialLatitude = currentLatitude;
        double initialLongitude = currentLongitude;

        locDisplacement = abs((currentLatitude - initialLatitude)) + Math.abs((currentLongitude - initialLongitude));
        Log.e("In displacement", "cool");
        if(locDisplacement >= 0.0027) {
            alarm_manager.cancel(pending_intent);
            sendBroadcast(my_intent);
        }
    }

    private void set_alarm_text(String output) {
        update_text.setText(output);
    }


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    @Override
    protected void onStart() {
        myGoogleAPIClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices(); //defined elsewhere! :)
        if (myGoogleAPIClient.isConnected() && RequestLocUpdates) {
            startLocationUpdates(); //elsewhere
        }
    }

    @Override
    protected void onStop() {
        myGoogleAPIClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void togglePeriodLocationUpdate() {
        if (!RequestLocUpdates) {
            RequestLocUpdates = true;
            startLocationUpdates();
        } else {
            RequestLocUpdates = false;
            stopLocationUpdates();
        }
    }

   // protected synchronized void buildGoogleApiClient() {
       // if(myGoogleAPIClient == null) {
         //   myGoogleAPIClient = new GoogleApiClient.Builder(this)
           //         .addConnectionCallbacks(this)
             //       .addOnConnectionFailedListener(this)
               //     .addApi(LocationServices.API)
                 //   .build();
        //}
    //}

    protected void createLocationRequest() {
        myLocReq = new LocationRequest();
        int UPDATE_INTERVAL = 100;
        myLocReq.setInterval(UPDATE_INTERVAL); //THIS MIGHT BE WEIRD
        myLocReq.setFastestInterval(FASTEST_INTERVAL); //MIGHT ALSO BE WEIRD!
        myLocReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        myLocReq.setSmallestDisplacement(DISPLACEMENT); //CHECK ME!!!
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }

        return true;
    }

    protected void startLocationUpdates() {
        //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;
        //}
        LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleAPIClient, myLocReq, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(myGoogleAPIClient, this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        myLastLoc = LocationServices.FusedLocationApi.getLastLocation(myGoogleAPIClient);
        if(myLastLoc != null) {
            ///DO SOMETHING HERE TO STORE VALUES FOR TOTAL???
        }
        if(RequestLocUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        myGoogleAPIClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult.getErrorCode()); //Something aint right here

    }

    @Override
    public void onLocationChanged(Location location) {
        myLastLoc = location; //This could make things weird with total distance required
        Toast.makeText(getApplicationContext(), "Location Changed!", Toast.LENGTH_LONG).show();
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
    }
}



