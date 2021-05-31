package com.example.nfctaggrid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
public class Accelerometer extends AppCompatActivity {

    private SensorManager mSensorManager;

    private TextView txt_x;
    private TextView txt_y;
    private TextView txt_z;
    private TextView txt_thresholdX;
    private TextView txt_thresholdY;
    private TextView txt_thresholdZ;


    private TabLayout tabLayout;
    SensorEventListener acc;
    private float[] thresholds = {0,0,0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);

        //get layout refs
        txt_x = (TextView) findViewById(R.id.txt_x);
        txt_z = (TextView) findViewById(R.id.txt_z);
        txt_y = (TextView) findViewById(R.id.txt_y);
        txt_thresholdX = (TextView) findViewById(R.id.txt_threshold_x);
        txt_thresholdY = (TextView) findViewById(R.id.txt_threshold_y);
        txt_thresholdZ = (TextView) findViewById(R.id.txt_threshold_z);
        tabLayout = findViewById(R.id.tablayout);
        tabLayout.getTabAt(1).select();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getText().equals("nfc")){
                    finish();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelero = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        acc = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    txt_x.setText(""+event.values[0]);
                    txt_y.setText(""+event.values[1]);
                    txt_z.setText(""+event.values[2]);
                    if (Math.abs(event.values[0])>thresholds[0])
                        thresholds[0] = Math.abs(event.values[0]);
                    if (Math.abs(event.values[1])>thresholds[1])
                        thresholds[1] = Math.abs(event.values[1]);
                    if (Math.abs(event.values[2])>thresholds[2])
                        thresholds[2] = Math.abs(event.values[2]);
                    txt_thresholdX.setText(""+thresholds[0]);
                    txt_thresholdY.setText(""+thresholds[1]);
                    txt_thresholdZ.setText(""+thresholds[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mSensorManager.registerListener(acc, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(acc);
    }
}
