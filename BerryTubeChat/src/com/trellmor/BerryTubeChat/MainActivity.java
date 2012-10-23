package com.trellmor.BerryTubeChat;

import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class MainActivity extends Activity {
	private EditText editUser;
	private EditText editPassword;
	private CheckBox checkRemember;

	public final static String KEY_LOGIN = "com.trellmor.BerryTubeChat.login";
	public final static String KEY_USERNAME = "com.trellmor.BerryTubeChat.login.username";
	public final static String KEY_PASSWORD = "com.trellmor.BerryTubeChat.login.password";
	public final static String KEY_REMEMBER = "com.trellmor.BerryTubeChat.login.rememberLogin";

	public final static String KEY_SETTINGS = "com.trellmor.BerryTubeChat.settings";
	public final static String KEY_SCROLLBACK = "com.trellmor.BerryTubeChat.settings.scrollback";

	private final static String CRYPT_SECRET = "6xKqJFsrOoYAUhLInaPg";

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
		Boolean remember = settings.getBoolean(KEY_REMEMBER, false);
		try {
			password = SimpleCrypto.decrypt(CRYPT_SECRET, password);
		} catch (Exception e) {
			Log.w(this.getClass().toString(), e.getMessage());
			remember = false;
		}

		if (remember) {
			if (user != "")
				editUser.setText(user);
			if (password != "")
				editPassword.setText(password);
			checkRemember.setChecked(remember);
		}
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
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStop() {
		super.onStop();

		SharedPreferences settings = getSharedPreferences(KEY_LOGIN,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		Boolean remember = checkRemember.isChecked();
		String password = editPassword.getText().toString();
		try {
			password = SimpleCrypto.encrypt(CRYPT_SECRET, password);
		} catch (Exception e) {
			// Failed to encrypt password
			remember = false;
			Log.w(this.getClass().toString(), e.getMessage());
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

		if ("".equals(password)) {
			editPassword.requestFocus();
			return;
		}

		Intent chat = new Intent(this, ChatActivity.class);
		chat.putExtra(KEY_USERNAME, username);
		chat.putExtra(KEY_PASSWORD, password);

		startActivity(chat);
	}
}
