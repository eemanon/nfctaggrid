package com.example.nfctaggrid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    PendingIntent mPendingIntent;
    IntentFilter writeTagFilters[];
    TextView tvNFCContent;
    Tag myTag;
    Context context;

    ImageView s;
    private float offsetY = 900;
    private float offsetX = 900;
    private float scaleValue = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        s = (ImageView) findViewById(R.id.iV_map);
        s.setImageResource(R.drawable.laval);

        context = this;


        final Button button = findViewById(R.id.btn_test);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                s.setY(s.getY()+54);
                s.setX(s.getX()+100);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
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
        String[] parts = msg.split("\\|");
        float x = Float.parseFloat(parts[0]);
        float y = Float.parseFloat(parts[1]);
        s.setX(-x*scaleValue+offsetX);
        s.setY(-y*scaleValue+offsetY);
        Log.i("print","x: "+x+" y: "+y);
        tvNFCContent.setText(msg);
        //move image :)
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
}