package com.trellmor.BerryTubeChat;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.trellmor.BerryTube.BerryTube;
import com.trellmor.BerryTube.BerryTubeBinder;
import com.trellmor.BerryTube.BerryTubeCallback;
import com.trellmor.BerryTube.ChatMessage;
import com.trellmor.BerryTube.ChatUser;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ChatActivity extends Activity {

	private ChatMessageAdapter mChatAdapter = null;
	private ArrayList<ChatMessage> mChatMessages = null;
	private ListView listChat;
	private TextView textNick;
	private EditText editChatMsg;
	private TextView textDrinks;

	private BerryTubeBinder mBinder = null;
	private boolean mServiceConnected = false;
	private ServiceConnection mService = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mServiceConnected = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mServiceConnected = true;
			mBinder = (BerryTubeBinder) service;
			mBinder.getService().registerCallback(mCallback);
			if (mBinder.getService().isConnected()) {
				setNick(mBinder.getService().getNick());
				drinkCount = mBinder.getService().getDrinkCount();
				updateDrinkCount();
			} else {
				try {
					mBinder.getService().connect(Username, Password);
				} catch (MalformedURLException e) {
					Log.w(ChatActivity.class.toString(), e);
				}
			}
		}
	};

	private String Username = "";
	private String Password = "";
	private String mNick = "";

	public String getNick() {
		return mNick;
	}

	protected void setNick(String nick) {
		if (nick != null) {
			mNick = nick;
			editChatMsg.setEnabled(true);
		} else {
			mNick = "Anonymous";
			editChatMsg.setEnabled(false);
		}

		textNick.setText(mNick);
		if (mChatAdapter != null)
			mChatAdapter.setNick(nick);
	}

	private int scrollback = 100;
	private int drinkCount = 0;
	private int myDrinkCount = 0;
	private boolean showDrinkCount = true;
	private BerryTubeCallback mCallback = null;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
		}

		editChatMsg = (EditText) findViewById(R.id.edit_chat_msg);
		TextView.OnEditorActionListener chatMsgListener = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					sendChatMsg();
				}
				return true;
			}
		};
		editChatMsg.setOnEditorActionListener(chatMsgListener);

		textDrinks = (TextView) findViewById(R.id.text_drinks);
		textNick = (TextView) findViewById(R.id.text_nick);
		textNick.setText("Anonymous");

		getConfigurationInstance();
		if (mChatMessages == null)
			mChatMessages = new ArrayList<ChatMessage>();
		listChat = (ListView) findViewById(R.id.list_chat);
		mChatAdapter = new ChatMessageAdapter(this, R.layout.chat_item,
				mChatMessages);
		listChat.setAdapter(mChatAdapter);

		Intent intent = getIntent();
		Username = intent.getStringExtra(MainActivity.KEY_USERNAME);
		Password = intent.getStringExtra(MainActivity.KEY_PASSWORD);

		createCallback();

		Intent serviceIntent = new Intent(this, BerryTube.class);
		startService(serviceIntent);
		bindService(serviceIntent, mService, BIND_ABOVE_CLIENT);
	}

	@Override
	public void onStart() {
		super.onStart();

		loadPreferences();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		if (mBinder != null) {
			mBinder.getService().unregisterCallback(mCallback);
			mCallback = null;
		}
		if (mServiceConnected)
			unbindService(mService);

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_chat, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		Intent backtoHome = new Intent(Intent.ACTION_MAIN);
		backtoHome.addCategory(Intent.CATEGORY_HOME);
		backtoHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(backtoHome);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;

		switch (item.getItemId()) {
		case R.id.menu_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_users:
			selectUser();
			return true;
		case R.id.menu_logout:
			stopService(new Intent(this, BerryTube.class));
			intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class ConfigurationInstance {
		public ArrayList<ChatMessage> ChatMessages;
		public int MyDrinkCount;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		ConfigurationInstance instance = new ConfigurationInstance();
		instance.ChatMessages = mChatMessages;
		instance.MyDrinkCount = myDrinkCount;
		return instance;
	}

	private void getConfigurationInstance() {
		@SuppressWarnings("deprecation")
		ConfigurationInstance instance = (ConfigurationInstance) getLastNonConfigurationInstance();
		if (instance != null) {
			mChatMessages = instance.ChatMessages;
			myDrinkCount = instance.MyDrinkCount;
		}
	}

	private void createCallback() {
		mCallback = new BerryTubeCallback() {

			@Override
			public void onSetNick(String nick) {
				setNick(nick);
			}

			@Override
			public void onChatMessage(ChatMessage chatMsg) {
				mChatMessages.add(chatMsg);
				while (mChatMessages.size() > scrollback)
					mChatMessages.remove(0);
				mChatAdapter.notifyDataSetChanged();

			}

			@Override
			public void onDrinkCount(int count) {
				drinkCount = count;
				updateDrinkCount();
			}
		};
	}

	private void loadPreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		try {
			scrollback = Integer.parseInt(settings.getString(
					MainActivity.KEY_SCROLLBACK, "100"));
		} catch (NumberFormatException e) {
			scrollback = 100;
		}

		if (scrollback <= 0)
			scrollback = 100;

		showDrinkCount = settings.getBoolean(MainActivity.KEY_DRINKCOUNT, true);
		updateDrinkCount();
	}

	private void sendChatMsg() {
		String textmsg = editChatMsg.getText().toString();
		if (mBinder.getService().isConnected() && !"".equals(mNick) && textmsg != "") {
			mBinder.getService().sendChat(textmsg);
			editChatMsg.setText("");
		}
	}

	private void updateDrinkCount() {
		if (!showDrinkCount) {
			setTextDrinksVisible(false);
			return;
		}

		if (drinkCount > 0) {
			if (myDrinkCount > drinkCount)
				myDrinkCount = 0;

			setTextDrinksVisible(true);

			textDrinks.setText(Integer.toString(myDrinkCount) + "/"
					+ Integer.toString(drinkCount) + " drinks");
		} else {
			setTextDrinksVisible(false);
			myDrinkCount = 0;
		}
	}

	public void drink(View view) {
		if (myDrinkCount < drinkCount) {
			myDrinkCount++;
			updateDrinkCount();
		}
	}

	private void setTextDrinksVisible(boolean Visible) {
		int visibility = (Visible) ? View.VISIBLE : View.GONE;

		if (textDrinks != null && textDrinks.getVisibility() != visibility)
			textDrinks.setVisibility(visibility);
	}

	private void selectUser() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_user);

		ArrayList<ChatUser> userList = new ArrayList<ChatUser>();
		for (ChatUser chatUser : mBinder.getService().getUsers()) {
			userList.add(chatUser.clone());
		}
		Collections.sort(userList, new Comparator<ChatUser>() {

			@Override
			public int compare(ChatUser lhs, ChatUser rhs) {
				if (lhs.getType() == rhs.getType()) {
					return lhs.getNick().compareTo(rhs.getNick());
				} else if (lhs.getType() > rhs.getType()) {
					return -1;
				} else {
					return +1;
				}

			}
		});

		final ArrayList<String> userNicks = new ArrayList<String>();
		for (ChatUser chatUser : userList) {
			userNicks.add(chatUser.getNick());
		}

		builder.setItems(userNicks.toArray(new String[userList.size()]),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String nick = userNicks.get(which);
						int start = editChatMsg.getSelectionStart();
						int end = editChatMsg.getSelectionEnd();

						editChatMsg.getText().replace(Math.min(start, end),
								Math.max(start, end), nick, 0, nick.length());

						dialog.dismiss();
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}
}
