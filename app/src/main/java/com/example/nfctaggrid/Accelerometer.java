package com.example.nfctaggrid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;

import java.util.LinkedList;
import java.util.Queue;

public class Accelerometer extends AppCompatActivity {

    private SensorManager mSensorManager;

    private TextView txt_x;
    private TextView txt_y;
    private TextView txt_z;
    private TextView txt_thresholdX;
    private TextView txt_thresholdY;
    private TextView txt_thresholdZ;

    private TextView txt_smoothedX;
    private TextView txt_smoothedY;
    private TextView txt_smoothedZ;

    private TabLayout tabLayout;

    Queue<Float> samplesX;
    Queue<Float> samplesY;
    Queue<Float> samplesZ;
    int bufferLength;
    SensorEventListener acc;
    private float[] thresholds = {0,0,0};
    private boolean calibration = true;
    private Button btn;

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
        txt_smoothedX = (TextView) findViewById(R.id.txt_filterX);
        txt_smoothedY = (TextView) findViewById(R.id.txt_filterY);
        txt_smoothedZ = (TextView) findViewById(R.id.txt_filterZ);
        btn = findViewById(R.id.btn_calibration);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calibration = false;
            }
        });

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
                    if (calibration){
                        if (Math.abs(event.values[0])>thresholds[0])
                            thresholds[0] = Math.abs(event.values[0]);
                        if (Math.abs(event.values[1])>thresholds[1])
                            thresholds[1] = Math.abs(event.values[1]);
                        if (Math.abs(event.values[2])>thresholds[2])
                            thresholds[2] = Math.abs(event.values[2]);
                        txt_thresholdX.setText(""+thresholds[0]);
                        txt_thresholdY.setText(""+thresholds[1]);
                        txt_thresholdZ.setText(""+thresholds[2]);
                    } else {
                        samplesX.remove();
                        samplesY.remove();
                        samplesZ.remove();
                        samplesX.add(event.values[0]);
                        samplesY.add(event.values[1]);
                        samplesZ.add(event.values[2]);
                        float avgX = avg(samplesX);
                        float avgY = avg(samplesY);
                        float avgZ = avg(samplesZ);
                        if(Math.abs(avgX)>thresholds[0]){
                            txt_smoothedX.setText(Float.toString(avgX));
                        } else
                            txt_smoothedX.setText("0");
                        if(Math.abs(avgY)>thresholds[1]){
                            txt_smoothedY.setText(Float.toString(avgY));
                        } else
                            txt_smoothedY.setText("0");
                        if(Math.abs(avgZ)>thresholds[2]){
                            txt_smoothedZ.setText(Float.toString(avgZ));
                        } else
                            txt_smoothedZ.setText("0");
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        samplesX = new LinkedList<Float>();
        samplesY = new LinkedList<Float>();
        samplesZ = new LinkedList<Float>();
        bufferLength = 5;
        //init queue
        for(int i = 0;i<bufferLength;i++){
            samplesX.add(0.0f);
            samplesY.add(0.0f);
            samplesZ.add(0.0f);
        }

        mSensorManager.registerListener(acc, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 10000);


    }

    private float avg(Queue<Float> samples) {
        float sum = 0.0f;
        for(float number : samples){
            sum+=number;
        }
        return sum/samples.size();
    }


    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(acc);
    }
}
