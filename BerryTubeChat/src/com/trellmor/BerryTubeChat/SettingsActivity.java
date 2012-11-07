package com.trellmor.BerryTubeChat;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

/**
 * BerryTubeChat settings
 * 
 * @author Daniel Triendl
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	private EditTextPreference prefScrollback;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		prefScrollback = (EditTextPreference) findPreference(MainActivity.KEY_SCROLLBACK);
	}

	@Override
	protected void onPause() {
		super.onPause();

		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		onSharedPreferenceChanged(null, MainActivity.KEY_SCROLLBACK);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
