/*
 * BerryTubeChat android client
 * Copyright (C) 2012-2015 Daniel Triendl <trellmor@trellmor.com>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * BerryTubeChat Main Activity
 * 
 * @author Daniel Triendl
 */
public class MainActivity extends ActionBarActivity {
	private static final String TAG = MainActivity.class.getName();

	private EditText mEditUser;
	private EditText mEditPassword;
	private CheckBox mCheckRemember;
	private ImageView mImageLuna;
	Handler mTimerHandler = new Handler();
	Runnable mTimerRunnable = new Runnable() {
		@Override
		public void run() {
			mClickCount = 0;
		}
	};
	private int mClickCount = 0;

	/**
	 * Key for login settings
	 */
	public final static String KEY_LOGIN = "com.trellmor.berrytubechat.login";
	/**
	 * Key for login username settings
	 */
	public final static String KEY_USERNAME = "com.trellmor.berrytubechat.login.username";
	/**
	 * Key for login password settings
	 */
	public final static String KEY_PASSWORD = "com.trellmor.berrytubechat.login.password";
	/**
	 * Key for login remember username and password setting
	 */
	public final static String KEY_REMEMBER = "com.trellmor.BerryTubeChat.login.rememberLogin";
	/**
	 * Key used to encrypt the password
	 */
	public final static String KEY_CRYPTO_KEY = "com.trellmor.BerryTubeChat.login.cryptokey";

	public final static String KEY_SETTINGS = "com.trellmor.berrytubechat.settings";

	/**
	 * Key for scrollback buffer size setting
	 */
	public final static String KEY_SCROLLBACK = "com.trellmor.berrytubechat.settings.scrollback";
	/**
	 * Key for show drink count setting
	 */
	public final static String KEY_DRINKCOUNT = "com.trellmor.berrytubechat.settings.drinkcount";
	/**
	 * Key for show pop up on new poll setting
	 */
	public final static String KEY_POPUP_POLL = "com.trellmor.berrytubechat.settings.popup_poll";
	/**
	 * Key for user flair index setting
	 */
	public final static String KEY_FLAIR = "com.trellmor.berrytubechat.settings.flair";
	/**
	 * Key for showing notification setting
	 */
	public final static String KEY_SQUEE = "com.trellmor.berrytubechat.settings.squee";
	/**
	 * Key for playing notification sound settings
	 */
	public final static String KEY_SQUEE_RINGTONE = "com.trellmor.berrytubechat.settings.squee.ringtone";
	/**
	 * Key for notification vibrate settings
	 */
	public final static String KEY_SQUEE_VIBRATE = "com.trellmor.berrytubechat.settings.squee.vibrate";
	/**
	 * Key for displaying timestamp settings
	 */
	public final static String KEY_TIMESTAMP = "com.trellmor.berrytubechat.settings.timestamp";
	/**
	 * Key for displaying current video settings
	 */
	public final static String KEY_VIDEO = "com.trellmor.berrytubechat.settings.video";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mEditUser = (EditText) findViewById(R.id.edit_user);
		mEditPassword = (EditText) findViewById(R.id.edit_password);
		mCheckRemember = (CheckBox) findViewById(R.id.check_remember);
		mImageLuna = (ImageView) findViewById(R.id.image_luna);

		SharedPreferences settings = getSharedPreferences(KEY_LOGIN,
				Context.MODE_PRIVATE);
		String user = settings.getString(KEY_USERNAME, "");
		String password = settings.getString(KEY_PASSWORD, "");
		String key = settings.getString(KEY_CRYPTO_KEY, "");

		boolean remember = settings.getBoolean(KEY_REMEMBER, false);

		if (key == null || "".equals(key)) {
			try {
				key = SimpleCrypto.generateKey();
				settings.edit().putString(KEY_CRYPTO_KEY, key).commit();
			} catch (NoSuchAlgorithmException e) {
				Log.w(TAG, e.getMessage());
				// Remeber not available because of missing key
				mCheckRemember.setVisibility(View.GONE);
				remember = false;
			}
		}

		if (key != null && !"".equals(key)) {
			try {
				password = SimpleCrypto.decrypt(key, password);
			} catch (GeneralSecurityException e) {
				Log.w(TAG, e.getMessage());
				remember = false;
			}

			if (remember) {
				mEditUser.setText(user);
				mEditPassword.setText(password);
			}
		}
		mCheckRemember.setChecked(remember);

		checkNotificationSound();

		Calendar c = Calendar.getInstance();
		if (c.get(Calendar.MONTH) == Calendar.APRIL && c.get(Calendar.DAY_OF_MONTH) == 1) {
			mImageLuna.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_donate:
			BerryTubeUtils.openDonatePage(this);
			return true;
		case R.id.menu_about:
			BerryTubeUtils.openAboutDialog(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences settings = getSharedPreferences(KEY_LOGIN,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		boolean remember = mCheckRemember.isChecked();
		String password = mEditPassword.getText().toString();
		String key = settings.getString(KEY_CRYPTO_KEY, "");
		if (key != null && !"".equals(key)) {
			try {
				password = SimpleCrypto.encrypt(key, password);
			} catch (Exception e) {
				// Failed to encrypt password
				remember = false;
				Log.w(TAG, e.getMessage());
			}
		} else {
			remember = false;
		}

		if (remember) {
			editor.putString(KEY_USERNAME, mEditUser.getText().toString());
			editor.putString(KEY_PASSWORD, password);
			editor.putBoolean(KEY_REMEMBER, remember);
		} else {
			editor.clear();
		}
		editor.apply();
	}

	public void login(View view) {
		String username = mEditUser.getText().toString();
		String password = mEditPassword.getText().toString();

		if ("".equals(username)) {
			mEditUser.requestFocus();
			return;
		}

		Intent chat = new Intent(this, ChatActivity.class);
		chat.putExtra(KEY_USERNAME, username);
		chat.putExtra(KEY_PASSWORD, password);

		startActivity(chat);
	}

	public void luna(View view) {
		if (mImageLuna.getVisibility() != View.VISIBLE) {
			mTimerHandler.removeCallbacks(mTimerRunnable);
			mClickCount++;

			if (mClickCount == 1) {
				Toast.makeText(this, R.string.toast_dance, Toast.LENGTH_SHORT).show();
			} else if (mClickCount == 7) {
				mImageLuna.setVisibility(View.VISIBLE);
			}

			mTimerHandler.postDelayed(mTimerRunnable, 500);
		}
	}

	private boolean copyNotificationSoundFile(File squee) {
		try {
			squee.getParentFile().mkdirs();
			InputStream is = getResources().openRawResource(R.raw.squee);
			try {
				FileOutputStream out = new FileOutputStream(squee);
				try {
					int size;
					byte[] buffer = new byte[1024];
					while ((size = is.read(buffer, 0, 1024)) >= 0) {
						out.write(buffer, 0, size);
					}
					out.flush();
					return true;
				} finally {
					out.close();
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Squee output file not found", e);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			Log.e(TAG, "Saving squee failed", e);
		}
		return false;

	}

	private void checkNotificationSound() {
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			File squee = new File(
					Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS),
					"squee.wav");

			if (!squee.exists()) {
				if (!copyNotificationSoundFile(squee)) {
					return;
				}
			}

			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			if ("".equals(settings.getString(KEY_SQUEE_RINGTONE, ""))) {
				MediaScannerConnection.scanFile(this,
						new String[] { squee.toString() }, null,
						new MediaScannerConnection.OnScanCompletedListener() {

							@Override
							public void onScanCompleted(String path, Uri uri) {
								Log.d(TAG, "Squee saved " + path);
								updateNotificationSoundPreference(uri);
							}
						});
			}
		}
	}
	
	private void updateNotificationSoundPreference(Uri uri) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.edit().putString(KEY_SQUEE_RINGTONE, uri.toString()).commit();
	}
}
