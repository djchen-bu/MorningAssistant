package com.example.djchen.morningassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Calendar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.common.server.converter.StringToIntConverter;


public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {

    // make alarm manager
    AlarmManager alarm_manager;
    TimePicker alarm_timepicker;
    TextView update_text;
    Context contex; //ATTEMPT ONE
    PendingIntent pending_intent;
    Intent tryIntent;

    //FOR LOCATION JUNK
    private StepDetector mStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private int numSteps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepDetector = new StepDetector();
        mStepDetector.registerListener(this);

        setContentView(R.layout.activity_main);
        this.contex = this;



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
        tryIntent = my_intent;

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

        tryIntent = my_intent;
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

        if(numSteps >= 10) {
            my_intent.putExtra("extra", "Off");

            set_alarm_text("Alarm off");

            alarm_manager.cancel(pending_intent);

            //tells clock off was pressed

            //stop the Ringtone

            sendBroadcast(my_intent);
            numSteps = 0;
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
    public void onResume() {
        super.onResume();
        numSteps = 0;
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        Log.e("STEP", Integer.toString(numSteps));
        if(numSteps >= 10) {
            tryIntent.putExtra("extra", "Off");

            set_alarm_text("Alarm off");

            alarm_manager.cancel(pending_intent);


            //tells clock off was pressed

            //stop the Ringtone

            sendBroadcast(tryIntent);
            numSteps = 0;
        }
    }
}



