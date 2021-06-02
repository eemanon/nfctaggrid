package com.example.nfctaggrid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private TextView txt_filterX;
    private TextView txt_filterZ;
    private TextView txt_filterY;
    private TextView txt_posX;
    private TextView txt_posY;
    private TextView txt_posZ;

    private Button btn_calibrate;


    private TabLayout tabLayout;
    SensorEventListener acc;
    private double[] thresholds = {0,0,0};
    private double[] previousSpeed = {0,0,0};
    private double[] previousPos = {0,0,0};
    private long [] lastReadingTime = {0,0,0};
    private boolean calibrated;
    private int samplingInterval;

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
        txt_filterX = findViewById(R.id.txt_filterX);
        txt_filterY = findViewById(R.id.txt_filterY);
        txt_filterZ = findViewById(R.id.txt_filterZ);
        txt_posX = findViewById(R.id.txt_posX);
        txt_posY = findViewById(R.id.txt_posY);
        txt_posZ = findViewById(R.id.txt_posZ);

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
        btn_calibrate = findViewById(R.id.btn_calibration);
        btn_calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrated=true;
            }
        });
        //init variables
        calibrated = false;
        samplingInterval = 1000; //in uSeconds
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelero = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        acc = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    txt_x.setText((event.values[0]+"").substring(0,4));
                    txt_y.setText((event.values[1]+"").substring(0,4));
                    txt_z.setText((event.values[2]+"").substring(0,4));
                    if(!calibrated){
                        if (Math.abs(event.values[0])>thresholds[0])
                            thresholds[0] = Math.abs(event.values[0]);
                        if (Math.abs(event.values[1])>thresholds[1])
                            thresholds[1] = Math.abs(event.values[1]);
                        if (Math.abs(event.values[2])>thresholds[2])
                            thresholds[2] = Math.abs(event.values[2]);
                        txt_thresholdX.setText((thresholds[0]+"").substring(0,4));
                        txt_thresholdY.setText((thresholds[1]+"").substring(0,4));
                        txt_thresholdZ.setText((thresholds[2]+"").substring(0,4));
                    } else {
                        //calculate speed and position
                        if (Math.abs(event.values[0])>thresholds[0]){
                            //first reading: The time elapsed between this reading and the previous one is the interval with which we read data
                            if(lastReadingTime[0]==0){

                                //calc speed
                                double xSpeed = (previousSpeed[0]+event.values[0]*samplingInterval/1000000)/2;  //m per s
                                //calc position
                                previousPos[0] = xSpeed*samplingInterval/1000000;
                                //set time of this reading
                                lastReadingTime[0] = System.nanoTime();
                                previousSpeed[0] = xSpeed;

                            } else {
                                long time = System.nanoTime();
                                double interval = (time - lastReadingTime[0])/1000000000;
                                //[ vx1 , vy1 , vz1 ] = [ vx0 + ax1 * (t1 - t0) , vy0 + ay1 * (t1 - t0) , vz0 + az1 * (t1 - t0) ]
                                //[ vx01 , vy01 , vz01 ] = [ (vx0 + vx1) / 2 , (vy0 + vy1) / 2 , (vz0 + vz1) / 2 ]
                                double xSpeed = (previousSpeed[0]+(previousSpeed[0] + event.values[0] * interval))/2;
                                //[ x1 , y1 , z1 ] = [x0 + vx01 * (t1 - t0), y0 + vy01 * (t1 - t0), y0 + vy01 * (t1 - t0) ]
                                previousPos[0] = previousPos[0]+ xSpeed*interval;
                                lastReadingTime[0] = time;
                                previousSpeed[0] = xSpeed;
                            }
                            txt_filterX.setText((event.values[0]+"").substring(0,4));
                            txt_posX.setText((previousPos[0]+"").substring(0,4));

                        } else
                            txt_filterX.setText("0");
                        if (Math.abs(event.values[1])>thresholds[1]){
                            if(lastReadingTime[1]==0){
                                //calc speed
                                double xSpeed = (previousSpeed[1]+event.values[1]*samplingInterval/1000000)/2;  //m per s
                                //calc position
                                previousPos[1] = xSpeed*samplingInterval/1000000;
                                //set time of this reading
                                lastReadingTime[1] = System.nanoTime();
                            } else {
                                long time = System.nanoTime();
                                double interval = (time - lastReadingTime[1])/1000000000;
                                //[ vx1 , vy1 , vz1 ] = [ vx0 + ax1 * (t1 - t0) , vy0 + ay1 * (t1 - t0) , vz0 + az1 * (t1 - t0) ]
                                //[ vx01 , vy01 , vz01 ] = [ (vx0 + vx1) / 2 , (vy0 + vy1) / 2 , (vz0 + vz1) / 2 ]
                                double ySpeed = (previousSpeed[1]+(previousSpeed[1] + event.values[1] * interval))/2;
                                //[ x1 , y1 , z1 ] = [x0 + vx01 * (t1 - t0), y0 + vy01 * (t1 - t0), y0 + vy01 * (t1 - t0) ]
                                previousPos[1] = previousPos[1]+ ySpeed*interval;
                                lastReadingTime[1] = time;
                                previousSpeed[1] = ySpeed;
                            }
                            txt_filterY.setText((event.values[1]+"").substring(0,4));
                            txt_posY.setText((previousPos[1]+"").substring(0,4));

                        } else {
                            txt_filterY.setText("0");
                        }
                        if (Math.abs(event.values[2])>thresholds[2]){
                            if(lastReadingTime[2]==0){
                                //calc speed
                                double xSpeed = (previousSpeed[2]+event.values[2]*samplingInterval/1000000)/2;  //m per s
                                //calc position
                                previousPos[2] = xSpeed*samplingInterval/1000000;
                                //set time of this reading
                                lastReadingTime[2] = System.nanoTime();
                            } else {
                                long time = System.nanoTime();
                                double interval = (time - lastReadingTime[2])/1000000000;
                                //[ vx1 , vy1 , vz1 ] = [ vx0 + ax1 * (t1 - t0) , vy0 + ay1 * (t1 - t0) , vz0 + az1 * (t1 - t0) ]
                                //[ vx01 , vy01 , vz01 ] = [ (vx0 + vx1) / 2 , (vy0 + vy1) / 2 , (vz0 + vz1) / 2 ]
                                double zSpeed = (previousSpeed[2]+(previousSpeed[2] + event.values[2] * interval))/2;
                                //[ x1 , y1 , z1 ] = [x0 + vx01 * (t1 - t0), y0 + vy01 * (t1 - t0), y0 + vy01 * (t1 - t0) ]
                                previousPos[2] = previousPos[0]+ zSpeed*interval;
                                lastReadingTime[2] = time;
                                previousSpeed[2] = zSpeed;
                            }
                            txt_filterZ.setText((event.values[2]+"").substring(0,4));
                            txt_posZ.setText((previousPos[2]+"").substring(0,4));
                        } else
                            txt_filterZ.setText("0");

                    }

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mSensorManager.registerListener(acc, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), samplingInterval);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(acc);
    }
}
