package com.trellmor.BerryTubeChat;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.EditText;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);        
    }
}
