package com.trellmor.BerryTubeChat;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	private EditTextPreference prefScrollback;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		prefScrollback = (EditTextPreference) findPreference(MainActivity.KEY_SCROLLBACK);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();

		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		onSharedPreferenceChanged(null, MainActivity.KEY_SCROLLBACK);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(MainActivity.KEY_SCROLLBACK)) {
			Preference pref = findPreference(key);
			if (pref instanceof EditTextPreference) {
				int scrollback;
				try {
					scrollback = Integer.parseInt(prefScrollback.getText()
							.toString());
				} catch (NumberFormatException e) {
					scrollback = 100;
				}

				if (scrollback <= 0)
					scrollback = 100;

				String scrollbackSummary = getString(R.string.pref_scrollback_summary);
				prefScrollback.setSummary(String.format(scrollbackSummary,
						scrollback));
			}
		}
	}
}
