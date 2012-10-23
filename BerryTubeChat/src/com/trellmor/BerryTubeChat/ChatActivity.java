package com.trellmor.BerryTubeChat;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.trellmor.BerryTube.BerryTube;
import com.trellmor.BerryTube.BerryTubeCallback;
import com.trellmor.BerryTube.ChatMessage;
import com.trellmor.BerryTube.ChatUser;

import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ChatActivity extends Activity {

	private ArrayAdapter<ChatMessage> chatAdapter = null;
	private ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
	private ListView listChat;
	private TextView textNick;
	private EditText editChatMsg;
	private TextView textDrinks;
	private ArrayList<ChatUser> userList = new ArrayList<ChatUser>();

	private BerryTube socket = null;

	private String Username = "";
	private String Password = "";
	private String Nick = "";
	private int scrollback = 100;
	private int drinkCount = 0;
	private int myDrinkCount = 0;	

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
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

		listChat = (ListView) findViewById(R.id.list_chat);
		chatAdapter = new ArrayAdapter<ChatMessage>(this, R.layout.chat_item,
				chatMessages);
		listChat.setAdapter(chatAdapter);

		Intent intent = getIntent();
		Username = intent.getStringExtra(MainActivity.KEY_USERNAME);
		Password = intent.getStringExtra(MainActivity.KEY_PASSWORD);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_chat, menu);
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
		case R.id.menu_users:
			selectUser();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		SharedPreferences settings = getSharedPreferences(
				MainActivity.KEY_SETTINGS, Context.MODE_PRIVATE);
		scrollback = settings.getInt(MainActivity.KEY_SCROLLBACK, 100);

		try {
			socket = new BerryTube(Username, Password);
			socket.connect(new BerryTubeCallback() {

				@Override
				public void onSetNick(String nick) {
					Nick = nick;
					textNick.setText(nick);
					editChatMsg.setEnabled(true);
				}

				@Override
				public void onChatMessage(ChatMessage chatMsg) {
					if (!chatMessages.contains(chatMsg)) {
						chatMessages.add(chatMsg);
						while (chatMessages.size() > scrollback)
							chatMessages.remove(0);
						chatAdapter.notifyDataSetChanged();
					}
				}

				@Override
				public void onUserJoin(ChatUser user) {
					userList.add(user);
				}

				@Override
				public void onUserPart(ChatUser user) {
					userList.remove(user);
				}

				@Override
				public void onUserReset() {
					userList.clear();
				}

				@Override
				public void onDrinkCount(int count) {
					drinkCount = count;
					updateDrinkCount();
				}
			});
		} catch (MalformedURLException e) {
			Log.w(this.getClass().toString(), e.getMessage());
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		// Stop socket.io connection
		if (socket != null) {
			if (socket.isConnected())
				socket.disconnect();

			socket = null;
		}
	}
	
	private void sendChatMsg() {
		String textmsg = editChatMsg.getText().toString();
		if (socket.isConnected() && Nick != "" && textmsg != "") {
			socket.sendChat(textmsg);
			editChatMsg.setText("");
		}
	}
	
	private void updateDrinkCount() {
		if (drinkCount > 0) {
			if (textDrinks.getVisibility() != View.VISIBLE)
				textDrinks.setVisibility(View.VISIBLE);
			
			textDrinks.setText(Integer.toString(myDrinkCount) + "/" + Integer.toString(drinkCount) + " drinks");
		} else {
			textDrinks.setVisibility(View.GONE);
			myDrinkCount = 0;
		}
	}
	
	public void drink(View view) {
		if (myDrinkCount < drinkCount) {
			myDrinkCount++;
			updateDrinkCount();
		}
	}
	
	private void selectUser() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_user);
		
		
		ArrayList<ChatUser> userList = new ArrayList<ChatUser>();
		for (ChatUser chatUser : this.userList) {
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
		
		builder.setItems(userNicks.toArray(new String[userList.size()]), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String nick = userNicks.get(which);
				int start = editChatMsg.getSelectionStart();
				int end = editChatMsg.getSelectionEnd();
				
				editChatMsg.getText().replace(Math.min(start, end), Math.max(start, end), nick, 0, nick.length());
				
				dialog.dismiss();
			}
		});
		
		AlertDialog alert = builder.create();
		alert.show();
	}
}
