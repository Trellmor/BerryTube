/*
 * BerryTubeChat android client
 * Copyright (C) 2012 Daniel Triendl <trellmor@trellmor.com>
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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.trellmor.berrytube.BerryTube;
import com.trellmor.berrytubechat.R;

/**
 * BerryTubeChat Main Activity
 * 
 * @author Daniel Triendl
 */
public class MainActivity extends Activity {
	private EditText editUser;
	private EditText editPassword;
	private CheckBox checkRemember;

	/**
	 * Key for login settings
	 */
	public final static String KEY_LOGIN = "com.trellmor.berrytubechat.login";
	/**
	 * Key for login username settins
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
	 * Key for play squee sound setting
	 */
	public final static String KEY_SQUEE = "com.trellmor.berrytubechat.settings.squee";
	public final static String KEY_TIMESTAMP = "com.trellmor.berrytubechat.settings.timestamp";
	public final static String KEY_VIDEO = "com.trellmor.berrytubechat.settings.video";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (BerryTube.isServiceRunning(this)) {
			Intent chat = new Intent(this, ChatActivity.class);
			startActivity(chat);
		}

		setContentView(R.layout.activity_main);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar ab = getActionBar();
			ab.setDisplayHomeAsUpEnabled(false);
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			ab.show();
		}

		editUser = (EditText) findViewById(R.id.edit_user);
		editPassword = (EditText) findViewById(R.id.edit_password);
		checkRemember = (CheckBox) findViewById(R.id.check_remember);

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
				Log.w(this.getClass().toString(), e.getMessage());
				// Remeber not available because of missing key
				checkRemember.setVisibility(View.GONE);
				remember = false;
			}
		}

		if (key != null && !"".equals(key)) {
			try {
				password = SimpleCrypto.decrypt(key, password);
			} catch (GeneralSecurityException e) {
				Log.w(this.getClass().toString(), e.getMessage());
				remember = false;
			}

			if (remember) {
				if (user != "")
					editUser.setText(user);
				if (password != "")
					editPassword.setText(password);

			}
		}
		checkRemember.setChecked(remember);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.menu_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
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

		boolean remember = checkRemember.isChecked();
		String password = editPassword.getText().toString();
		String key = settings.getString(KEY_CRYPTO_KEY, "");
		if (key != null && !"".equals(key)) {
			try {
				password = SimpleCrypto.encrypt(key, password);
			} catch (Exception e) {
				// Failed to encrypt password
				remember = false;
				Log.w(this.getClass().toString(), e.getMessage());
			}
		} else {
			remember = false;
		}

		if (remember) {
			editor.putString(KEY_USERNAME, editUser.getText().toString());
			editor.putString(KEY_PASSWORD, password);
			editor.putBoolean(KEY_REMEMBER, remember);
		} else {
			editor.clear();
		}
		editor.commit();
	}

	public void login(View view) {
		String username = editUser.getText().toString();
		String password = editPassword.getText().toString();

		if ("".equals(username)) {
			editUser.requestFocus();
			return;
		}

		Intent chat = new Intent(this, ChatActivity.class);
		chat.putExtra(KEY_USERNAME, username);
		chat.putExtra(KEY_PASSWORD, password);

		startActivity(chat);
	}
}
