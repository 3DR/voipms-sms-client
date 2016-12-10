package net.kourlas.voipms_sms.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import net.kourlas.voipms_sms.R;

public class CustomizeActivity extends AppCompatActivity {

    String contactName = "";
    String led = "500";
    Switch fast;
    Integer color =  0xFFD12C63;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        Log.v("name", contactName);

        Intent i = getIntent();

        if (i!=null) {
            contactName= i.getStringExtra("contact");

        }
        Log.v("name", contactName);
        saveLed();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityMonitor.getInstance().deleteReferenceToActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityMonitor.getInstance().deleteReferenceToActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityMonitor.getInstance().setCurrentActivity(this);
    }

    public void saveLed(){
        SharedPreferences sharedPreferences = getSharedPreferences("ledData", Context.MODE_PRIVATE);
        SharedPreferences sharedPref = getSharedPreferences("color", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(contactName, led);
        editor.apply();

        SharedPreferences.Editor editor1 = sharedPref.edit();
        editor1.putInt(contactName, color);
        editor1.apply();

        String cNm = sharedPreferences.getString(contactName, "");
        Log.v("saved rate of ", cNm + " for " + contactName);
    }
}
