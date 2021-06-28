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
    private TextView txt_state;

    private TabLayout tabLayout;

    private TextView txt_posx;
    private TextView txt_posy;

    private TextView txt_vectorX;
    private TextView txt_vectorY;

    Queue<Float> samplesX;
    Queue<Float> samplesY;
    Queue<Float> samplesZ;
    int bufferLength;
    SensorEventListener acc;
    private float[] thresholds = {0,0,0};
    private float[] bufferSums = {0,0,0}; //holds built up buffers
    private int bufferIterator;           //holds information how many values have been added already
    private boolean calibration = true;
    private int timeoutSamples;
    private int timeoutSampleIterator;
    private float[] position = {0,0};
    private Button btn;
    private float factor;

    private String mode; //takes values normal, buffer, timeout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);

        //1 get layout refs
        txt_x = (TextView) findViewById(R.id.txt_x);
        txt_z = (TextView) findViewById(R.id.txt_z);
        txt_y = (TextView) findViewById(R.id.txt_y);
        txt_posx = findViewById(R.id.txt_posX);
        txt_posy = findViewById(R.id.txt_posY);
        txt_vectorX = findViewById(R.id.txt_vectorX);
        txt_vectorY = findViewById(R.id.txt_vectorY);
        txt_thresholdX = (TextView) findViewById(R.id.txt_threshold_x);
        txt_thresholdY = (TextView) findViewById(R.id.txt_threshold_y);
        txt_thresholdZ = (TextView) findViewById(R.id.txt_threshold_z);
        txt_smoothedX = (TextView) findViewById(R.id.txt_filterX);
        txt_smoothedY = (TextView) findViewById(R.id.txt_filterY);
        txt_smoothedZ = (TextView) findViewById(R.id.txt_filterZ);
        txt_state = findViewById(R.id.txt_state);
        txt_state.setText("normal");
        btn = findViewById(R.id.btn_calibration);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //init variable that tracks if we search a max threshold or not
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
                        if(mode.equals("normal")){
                            if(Math.abs(avgX)>thresholds[0]){
                                mode="buffer";
                                txt_state.setText("buffer");
                                bufferSums[0]=avgX;
                                bufferSums[1]=avgY;
                                bufferIterator=bufferLength;
                                txt_smoothedX.setText(Float.toString(avgX).substring(0,4));
                            } else
                                txt_smoothedX.setText("0");
                            if(Math.abs(avgY)>thresholds[1]){
                                mode="buffer";
                                txt_state.setText("buffer");
                                bufferSums[1]=avgY;
                                bufferSums[0]=avgX;
                                bufferIterator=bufferLength;
                                txt_smoothedY.setText(Float.toString(avgY).substring(0,4));
                            } else
                                txt_smoothedY.setText("0");
                        }
                        if(mode.equals("buffer")){
                            if(bufferIterator>=0){
                                bufferSums[0]+=avgX;
                                bufferSums[1]+=avgY;
                                txt_state.setText("buffer "+timeoutSampleIterator);
                                bufferIterator--;
                            } else {
                                mode="timeout";
                                txt_state.setText("timeout "+timeoutSampleIterator);
                                txt_vectorX.setText(bufferSums[0]+"");
                                txt_vectorY.setText(bufferSums[1]+"");

                                timeoutSampleIterator=timeoutSamples;
                            }
                        }
                        if(mode.equals("timeout")){
                            if(timeoutSampleIterator>0){
                                //go on moving
                                position[0]+=bufferSums[0]*factor;
                                position[1]+=bufferSums[1]*factor;
                                txt_posx.setText(position[0]+"");
                                txt_posy.setText(position[1]+"");
                                txt_state.setText("timeout "+timeoutSampleIterator);
                                // and decrease iterator
                                if(Math.abs(avgY)>thresholds[1] || Math.abs(avgX)>thresholds[0])
                                    timeoutSampleIterator=timeoutSamples;
                                else
                                    timeoutSampleIterator--;
                            } else {
                                mode="normal";
                                txt_state.setText("normal");
                            }
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        //Init values
        factor = 0.002f;
        mode="normal";
        timeoutSamples=8;     //completely random I have no clue about this
        samplesX = new LinkedList<Float>();
        samplesY = new LinkedList<Float>();
        samplesZ = new LinkedList<Float>();
        bufferLength = 15;
        timeoutSampleIterator=0;
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
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(acc, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 10000);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(acc);
    }
}
