/*
 * BerryTubeChat android client
 * Copyright (C) 2015 Daniel Triendl <trellmor@trellmor.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.trellmor.berrytubechat;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;

import com.trellmor.berrymotes.EmoteSettings;

public class SettingsFragment extends PreferenceFragment
		implements SharedPreferences.OnSharedPreferenceChangeListener {

	private EditTextPreference mPrefScrollback;
	private EditTextPreference mPrefServer;
	private Preference mPrefEmotesEnabled;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		addPreferencesFromResource(R.xml.pref_notification);
		EmoteSettings.addEmoteSettings(this);

		mPrefEmotesEnabled = findPreference(EmoteSettings.KEY_BERRYMOTES_ENABLED);
		mPrefEmotesEnabled.setOnPreferenceChangeListener(sEmoteEnabledChangeListener);

		mPrefScrollback = (EditTextPreference) findPreference(MainActivity.KEY_SCROLLBACK);
		mPrefServer = (EditTextPreference) findPreference(MainActivity.KEY_SERVER);

		bindPreferenceSummaryToValue(findPreference(MainActivity.KEY_FLAIR));
		bindPreferenceSummaryToValue(findPreference(MainActivity.KEY_SQUEE_RINGTONE));
	}

	@Override
	public void onPause() {
		super.onPause();

		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(null, MainActivity.KEY_SCROLLBACK);
		onSharedPreferenceChanged(null, MainActivity.KEY_SERVER);


		if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			//We already asked at app start to enable storage permission, so shouldShowRequestPermissionRationale returns only false,
			//if never again was selected
			if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				mPrefEmotesEnabled.setEnabled(false);
				mPrefEmotesEnabled.setSummary(R.string.pref_description_berrymotes_permission_storage);
			}
		}
	}

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case SettingsActivity.PERMISSION_REQUEST_EXTERNAL_STORAGE:
				if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					//Disable berrymotes
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
					settings.edit().putBoolean(EmoteSettings.KEY_BERRYMOTES_ENABLED, false).apply();
					if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						mPrefEmotesEnabled.setEnabled(false);
						mPrefEmotesEnabled.setSummary(R.string.pref_description_berrymotes_permission_storage);
					}
				}
				break;
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(MainActivity.KEY_SCROLLBACK)) {
			Preference pref = findPreference(key);
			if (pref instanceof EditTextPreference) {
				int scrollback;
				try {
					scrollback = Integer.parseInt(mPrefScrollback.getText());
				} catch (NumberFormatException e) {
					scrollback = 1000;
				}

				if (scrollback <= 0)
					scrollback = 1000;

				String scrollbackSummary = getString(R.string.pref_scrollback_summary);
				mPrefScrollback.setSummary(String.format(scrollbackSummary, scrollback));
			}
		} else if (key.equals(MainActivity.KEY_SERVER)) {
			if (mPrefServer.getText() == null || "".equals(mPrefServer.getText())) {
				mPrefServer.setSummary(R.string.prev_server_summary_default);
			} else {
				mPrefServer.setSummary(mPrefServer.getText());
			}
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);

			} else if (preference instanceof RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					preference.setSummary(R.string.pref_squee_ringtone_silent);

				} else {
					Ringtone ringtone = RingtoneManager.getRingtone(
							preference.getContext(), Uri.parse(stringValue));

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null);
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						String name = ringtone
								.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

	private final Preference.OnPreferenceChangeListener sEmoteEnabledChangeListener = new Preference.OnPreferenceChangeListener() {

		@Override
		public boolean onPreferenceChange(final Preference preference, Object newValue) {
			boolean canChange = EmoteSettings.sEnabledChangeListener.onPreferenceChange(preference, newValue);

			if (canChange && Boolean.TRUE.equals(newValue)) {
				if (ActivityCompat.checkSelfPermission(preference.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
					if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						View view = SettingsFragment.this.getView();
						if (view != null) {
							Snackbar.make(view, R.string.external_storage_permission_rationale, Snackbar.LENGTH_INDEFINITE)
									.setAction(android.R.string.ok, new View.OnClickListener() {
										@Override
										public void onClick(View view) {
											requestStoragePermission();
										}
									}).show();
						}

						canChange = false;
					} else {
						requestStoragePermission();
					}
				}
			}

			return canChange;
		}

		private void requestStoragePermission() {
			ActivityCompat.requestPermissions(getActivity(),
					new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
					SettingsActivity.PERMISSION_REQUEST_EXTERNAL_STORAGE);
		}
	};
}
