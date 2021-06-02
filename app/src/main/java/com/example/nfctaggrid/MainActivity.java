package com.example.nfctaggrid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    PendingIntent mPendingIntent;
    IntentFilter writeTagFilters[];
    TextView tvNFCContent;
    Tag myTag;
    Context context = this;

    ImageView s;
    TabLayout tabLayout;
    Button btn_calibrate;
    Button btn_start;

    Queue<Float> samplesX;
    Queue<Float> samplesY;
    Queue<Float> samplesZ;
    int bufferLength;
    SensorEventListener acc;
    private SensorManager mSensorManager;
    private float[] thresholds = {0,0,0};
    private float[] bufferSums = {0,0,0}; //holds built up buffers
    private int bufferIterator;           //holds information how many values have been added already
    private boolean calibration = false;
    private boolean mapreadmode = true;
    private int timeoutSamples;
    private int timeoutSampleIterator;
    private float factor;

    private String mode; //takes values normal, buffer, timeout

    //device specific
    private float offsetY = 2600;
    private float offsetX = 1800;
    private float scaleValue = 550;

    private float x;            //values of coordinate system to be modified by either NFC or accelerometer
    private float y;
    private boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //get refs
        tabLayout = findViewById(R.id.tablayout);
        tabLayout.getTabAt(0).select();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                                               @Override
                                               public void onTabSelected(TabLayout.Tab tab) {
                                                   Log.i("info", "chouette " + tab.getText());
                                                   if(tab.getText().equals("accelerometer")){
                                                       Log.i("info", "chouette equals acc");
                                                       Intent intent = new Intent(context, Accelerometer.class);
                                                       startActivity(intent);
                                                   }
                                               }

                                               @Override
                                               public void onTabUnselected(TabLayout.Tab tab) {

                                               }

                                               @Override
                                               public void onTabReselected(TabLayout.Tab tab) {

                                               }
                                           });
            //set image
        s = (ImageView) findViewById(R.id.iV_map);
        s.setImageResource(R.drawable.laval);

        btn_start = findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                started = true;
                btn_start.setText("Started.");
            }
        });
        btn_calibrate = findViewById(R.id.btn_calibrate);
        btn_calibrate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!calibration){
                    thresholds[0] = 0;
                    thresholds[1] = 0;
                    thresholds[2] = 0;
                    btn_calibrate.setText("Calibrating...");
                } else
                    btn_calibrate.setText("Calibrate");
                calibration = calibration == false;
            }
        });
        tvNFCContent = findViewById(R.id.tvNFCContent);
        //NFC stuff
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelero = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        acc = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    if (calibration){
                        if (Math.abs(event.values[0])>thresholds[0])
                            thresholds[0] = Math.abs(event.values[0]);
                        if (Math.abs(event.values[1])>thresholds[1])
                            thresholds[1] = Math.abs(event.values[1]);
                        if (Math.abs(event.values[2])>thresholds[2])
                            thresholds[2] = Math.abs(event.values[2]);
                    } else {
                        if(started){
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
                                    bufferSums[0]=avgX;
                                    bufferSums[1]=avgY;
                                    bufferIterator=bufferLength;
                                } else
                                if(Math.abs(avgY)>thresholds[1]){
                                    mode="buffer";
                                    bufferSums[1]=avgY;
                                    bufferSums[0]=avgX;
                                    bufferIterator=bufferLength;
                                }
                            }
                            if(mode.equals("buffer")){
                                if(bufferIterator>=0){
                                    bufferSums[0]+=avgX;
                                    bufferSums[1]+=avgY;
                                    bufferIterator--;
                                } else {
                                    mode="timeout";
                                    timeoutSampleIterator=timeoutSamples;
                                }
                            }
                            if(mode.equals("timeout")){
                                if(timeoutSampleIterator>0){
                                    //go on moving
                                    x+=bufferSums[0]*factor;
                                    y-=bufferSums[1]*factor;
                                    updateView();
                                    // and decrease iterator
                                    if(Math.abs(avgY)>thresholds[1] || Math.abs(avgX)>thresholds[0])
                                        timeoutSampleIterator=timeoutSamples;
                                    else
                                        timeoutSampleIterator--;
                                } else {
                                    mode="normal";
                                }
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

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        mSensorManager.registerListener(acc, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 10000);
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        mSensorManager.unregisterListener(acc);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getTagInfo(intent);
    }

    private void getTagInfo(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage ndefMessage = (NdefMessage) rawMsgs[0];
        String msg = processRtdTextRecord(ndefMessage.getRecords()[0].getPayload());
        //parse text and extract coords
        if(mapreadmode){
            //set image
            if(msg.equals("map:moon")){
                s.setImageResource(R.drawable.moon);
                s.setScaleX(2.5f);
                s.setScaleY(2.5f);
            } else {
                s.setImageResource(R.drawable.laval);
            }
            mapreadmode = false;
        } else {
            String[] parts = msg.split("\\|");
            x = Float.parseFloat(parts[0]);
            y = Float.parseFloat(parts[1]);
            updateView();
            Log.i("print","x: "+x+" y: "+y);
            tvNFCContent.setText(msg);
        }

        //move image :)
    }

    private void updateView() {
        s.setX(-x*scaleValue+offsetX);
        s.setY(-y*scaleValue+offsetY);
    }

    private String processRtdTextRecord(byte[] payload) {
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        String text = "";
        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e("UnsupportedEncoding", e.toString());
        }
        return text;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        tabLayout.getTabAt(0).select();
    }
}