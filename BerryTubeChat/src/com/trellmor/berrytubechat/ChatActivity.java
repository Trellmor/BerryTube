/*
 * BerryTubeChat android client
 * Copyright (C) 2012-2013 Daniel Triendl <trellmor@trellmor.com>
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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.trellmor.berrytube.BerryTube;
import com.trellmor.berrytube.BerryTubeBinder;
import com.trellmor.berrytube.BerryTubeCallback;
import com.trellmor.berrytube.ChatMessage;
import com.trellmor.berrytube.ChatUser;
import com.trellmor.berrytube.Poll;

/**
 * BerryTubeChat chat window
 * 
 * @author Daniel
 */
public class ChatActivity extends Activity {
	private static final String TAG = ChatActivity.class.getName();

	private static final String KEY_DRINKCOUT = "drinkCount";
	private static final String KEY_MYDRINKCOUNT = "myDrinkCount";

	private ChatMessageAdapter mChatAdapter = null;
	private ListView mListChat;
	private TextView mTextNick;
	private EditText mEditChatMsg;
	private TextView mTextDrinks;
	private TextView mCurrentVideo;
	private NotificationCompat.Builder mNotification = null;

	private BerryTubeBinder mBinder = null;
	private ServiceConnection mService = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBinder = null;
			mListChat.setAdapter(null);
			mChatAdapter = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			initService((BerryTubeBinder) service);
		}
	};

	private String mUsername = "";
	private String mPassword = "";
	private String mNick = "";
	private int mFlair = 0;
	private boolean mShowVideo = false;
	private boolean mFirstPrefLoad = true;
	private int mScrollback = 100;
	private int mDrinkCount = 0;
	private int mMyDrinkCount = 0;
	private boolean mShowDrinkCount = true;
	private boolean mPopupPoll = false;
	private BerryTubeCallback mCallback = null;
	private boolean mLogout = false;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_chat);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
		}

		mEditChatMsg = (EditText) findViewById(R.id.edit_chat_msg);
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
		mEditChatMsg.setOnEditorActionListener(chatMsgListener);

		mTextDrinks = (TextView) findViewById(R.id.text_drinks);
		registerForContextMenu(mTextDrinks);

		mCurrentVideo = (TextView) findViewById(R.id.text_video);
		mCurrentVideo.setMovementMethod(LinkMovementMethod.getInstance());

		mTextNick = (TextView) findViewById(R.id.text_nick);
		mTextNick.setText("Anonymous");

		mListChat = (ListView) findViewById(R.id.list_chat);

		Intent intent = getIntent();
		mUsername = intent.getStringExtra(MainActivity.KEY_USERNAME);
		mPassword = intent.getStringExtra(MainActivity.KEY_PASSWORD);

		if (savedInstanceState != null) {
			mDrinkCount = savedInstanceState.getInt(KEY_DRINKCOUT);
			mMyDrinkCount = savedInstanceState.getInt(KEY_MYDRINKCOUNT);
		}

		startService(new Intent(this, BerryTube.class));
		bindService(new Intent(this, BerryTube.class), mService,
				BIND_ABOVE_CLIENT);
	}

	@Override
	protected void onStart() {
		super.onStart();

		loadPreferences();

		if (mBinder != null) {
			initService(mBinder);
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Kill the callback
		if (mBinder != null) {
			mBinder.getService().setCallback(null);
		}

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onDestroy() {
		if (mService != null) {
			unbindService(mService);
			mService = null;
		}

		mCallback = null;

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
			selectUser(null);
			return true;
		case R.id.menu_logout:
			mLogout = true;

			stopService(new Intent(this, BerryTube.class));
			finish();
			return true;
		case R.id.menu_donate:
			BerryTubeUtils.openDonatePage(this);
			return true;
		case R.id.menu_about:
			BerryTubeUtils.openAboutDialog(this);
			return true;
		case R.id.menu_poll:
			showPoll();
		case R.id.menu_autocomplete_nick:
			autocompleteNick();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		switch (v.getId()) {
		case R.id.text_drinks:
			getMenuInflater().inflate(R.menu.context_text_drinks, menu);
			break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_reset_my_drinks:
			mMyDrinkCount = 0;
			updateDrinkCount();
			return true;
		default:
			return super.onContextItemSelected(item);
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(KEY_DRINKCOUT, mDrinkCount);
		outState.putInt(KEY_MYDRINKCOUNT, mMyDrinkCount);
	}

	private void createCallback() {
		mCallback = new BerryTubeCallback() {

			@Override
			public void onSetNick(String nick) {
				setNick(nick);
			}

			@Override
			public void onLoginError(String error) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						ChatActivity.this);
				builder.setTitle(R.string.login_error);
				builder.setMessage(error);
				builder.setPositiveButton(android.R.string.ok, null);
				builder.show();
			}

			@Override
			public void onChatMessage(ChatMessage chatMsg) {
				mChatAdapter.notifyDataSetChanged();
			}

			@Override
			public void onDrinkCount(int count) {
				mDrinkCount = count;
				updateDrinkCount();
			}

			@Override
			public void onNewPoll(Poll poll) {
				if (mPopupPoll)
					showPoll();
			}

			@Override
			public void onUpatePoll(Poll poll) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onClearPoll() {

			}

			@Override
			public void onVideoUpdate(String name, String id, String type) {
				setTextVideoVisible(true);
				updateCurrentVideo(name, id, type);
			}

			@Override
			public void onKicked() {
				mLogout = true;
				finish();
			}

			@Override
			public void onDisconnect() {
				if (mLogout)
					return;

				AlertDialog.Builder builder = new AlertDialog.Builder(
						ChatActivity.this);
				builder.setTitle(R.string.disconnected);
				builder.setMessage(R.string.message_disconnected);
				builder.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								stopService(new Intent(ChatActivity.this,
										BerryTube.class));
								ChatActivity.this.finish();
							}
						});

				builder.show();
			}
		};
	}

	private void loadPreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		try {
			mScrollback = Integer.parseInt(settings.getString(
					MainActivity.KEY_SCROLLBACK, "100"));
		} catch (NumberFormatException e) {
			mScrollback = 100;
		}

		if (mScrollback <= 0)
			mScrollback = 100;

		if (mBinder != null)
			mBinder.getService().setChatMsgBufferSize(mScrollback);

		try {
			mFlair = Integer.parseInt(settings.getString(
					MainActivity.KEY_FLAIR, "0"));
		} catch (NumberFormatException e) {
			mFlair = 0;
		}

		if (settings.getBoolean(MainActivity.KEY_SQUEE, false)) {
			mNotification = new NotificationCompat.Builder(this);
			mNotification.setSmallIcon(R.drawable.ic_stat_notify_chat);
			mNotification.setLights(0xFF0000FF, 100, 2000);
			mNotification.setAutoCancel(true);

			Intent intent = new Intent(this, MainActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);

			mNotification.setContentIntent(PendingIntent.getActivity(this, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT));
			String squee = settings.getString(MainActivity.KEY_SQUEE_RINGTONE,
					"");
			if (!"".equals(squee)) {
				mNotification.setSound(Uri.parse(squee),
						AudioManager.STREAM_NOTIFICATION);
			}
			if (settings.getBoolean(MainActivity.KEY_SQUEE_VIBRATE, false)) {
				mNotification.setVibrate(new long[] { 0, 100 });
			}
		} else {
			mNotification = null;
		}

		boolean showVideo = settings.getBoolean(MainActivity.KEY_VIDEO, false);
		if (showVideo != mShowVideo) {
			// If the value has changed, act on it
			if (showVideo) {
				if (!mFirstPrefLoad) {
					Toast.makeText(this, R.string.toast_video_enabled,
							Toast.LENGTH_LONG).show();
				}
			} else {
				mBinder.getService().disableVideoMessages();
				setTextVideoVisible(false);
			}
		}
		mShowVideo = showVideo;

		mShowDrinkCount = settings
				.getBoolean(MainActivity.KEY_DRINKCOUNT, true);
		mPopupPoll = settings.getBoolean(MainActivity.KEY_POPUP_POLL, false);
		updateDrinkCount();

		mFirstPrefLoad = false;
	}

	private void sendChatMsg() {
		String textmsg = mEditChatMsg.getText().toString().trim();
		if (mBinder.getService().isConnected() && !"".equals(mNick)
				&& textmsg.length() > 0) {
			mBinder.getService().sendChat(textmsg, mFlair);
			mEditChatMsg.setText("");
		}
	}

	protected void setNick(String nick) {
		if (nick != null) {
			mNick = nick;
			mEditChatMsg.setEnabled(true);
		} else {
			mNick = "Anonymous";
			mEditChatMsg.setEnabled(false);
		}

		mTextNick.setText(mNick);
		if (mChatAdapter != null)
			mChatAdapter.setNick(nick);
	}

	private void updateDrinkCount() {
		if (!mShowDrinkCount) {
			setTextDrinksVisible(false);
			return;
		}

		if (mDrinkCount > 0) {
			if (mMyDrinkCount > mDrinkCount)
				mMyDrinkCount = 0;

			setTextDrinksVisible(true);

			mTextDrinks
					.setText(Integer.toString(mMyDrinkCount)
							+ "/"
							+ Integer.toString(mDrinkCount)
							+ " "
							+ ((mDrinkCount == 1) ? getString(R.string.drink_count_single)
									: getString(R.string.drink_count_plural)));
		} else {
			setTextDrinksVisible(false);
			mMyDrinkCount = 0;
		}
	}

	public void drink(View view) {
		if (mMyDrinkCount < mDrinkCount) {
			mMyDrinkCount++;
			updateDrinkCount();
		}
	}

	private void setTextDrinksVisible(boolean Visible) {
		int visibility = (Visible) ? View.VISIBLE : View.GONE;

		if (mTextDrinks != null && mTextDrinks.getVisibility() != visibility)
			mTextDrinks.setVisibility(visibility);
	}

	private void updateCurrentVideo(String title, String id, String type) {
		StringBuilder sb = new StringBuilder();
		sb.append(getString(R.string.current_video));
		sb.append(" <a href=\"http://");
		if ("yt".equals(type)) {
			sb.append("youtu.be/");
		} else if ("vimeo".equals(type)) {
			sb.append("vimeo.com/");
		}
		sb.append(id).append("\">").append(title).append("</a>");
		mCurrentVideo.setText(Html.fromHtml(sb.toString()));
	}

	private void setTextVideoVisible(boolean visible) {
		if (!mShowVideo) {
			return;
		}

		int visibility = (visible) ? View.VISIBLE : View.GONE;

		if (mCurrentVideo != null
				&& mCurrentVideo.getVisibility() != visibility)
			mCurrentVideo.setVisibility(visibility);
	}

	private void selectUser(String filter) {
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
			if (filter != null) {
				if (chatUser.getNick().toLowerCase(Locale.ENGLISH)
						.startsWith(filter.toLowerCase(Locale.ENGLISH))) {
					userNicks.add(chatUser.getNick());
				}
			} else {
				userNicks.add(chatUser.getNick());
			}
		}

		if (userNicks.size() > 1 || filter == null) {
			builder.setItems(userNicks.toArray(new String[userNicks.size()]),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String nick = userNicks.get(which);
							replaceNick(nick);
							dialog.dismiss();
						}
					});

			AlertDialog alert = builder.create();
			alert.show();
		} else if (userNicks.size() == 1 && filter != null) {
			replaceNick(userNicks.get(0));
		} else {
			Toast toast = Toast.makeText(this, R.string.toast_no_users,
					Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	private void replaceNick(String nick) {
		int selStart = mEditChatMsg.getSelectionStart();
		int selEnd = mEditChatMsg.getSelectionEnd();
		/*
		 * mEditChatMsg.getText().replace( Math.min(selStart, selEnd),
		 * Math.max(selStart, selEnd), nick, 0, nick.length());
		 */
		String msg = mEditChatMsg.getText().toString();
		msg = msg.substring(0, Math.min(selStart, selEnd)) + nick
				+ msg.substring(Math.max(selStart, selEnd));
		mEditChatMsg.setText(msg); // SetText to refresh suggestions from some
									// keyboards
		mEditChatMsg.setSelection(Math.min(selStart, selEnd) + nick.length());
	}

	private void showPoll() {
		Poll poll = mBinder.getService().getPoll();
		if (poll == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.nopoll);
			builder.setMessage(R.string.message_nopoll);
			builder.setPositiveButton(android.R.string.ok, null);

			AlertDialog dialog = builder.create();
			dialog.show();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(poll.getTitle());

			String[] options = new String[mBinder.getService().getPoll()
					.getOptions().size()];
			for (int i = 0; i < options.length; i++) {
				StringBuilder option = new StringBuilder();
				option.append("[");
				if (poll.getObscure()) {
					option.append("??");
				} else {
					option.append(poll.getVotes().get(i));
				}
				option.append("] ").append(poll.getOptions().get(i));

				options[i] = option.toString();
			}
			builder.setItems(options, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					mBinder.getService().votePoll(which);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private void initService(BerryTubeBinder service) {
		mBinder = service;

		if (mCallback == null) {
			createCallback();
		}
		mBinder.getService().setCallback(mCallback);

		mBinder.getService().setChatMsgBufferSize(mScrollback);

		mBinder.getService().setNotification(mNotification);
		mNotification = null;

		if (mChatAdapter == null) {
			mChatAdapter = new ChatMessageAdapter(ChatActivity.this,
					R.layout.chat_item, mBinder.getService().getChatMsgBuffer());
			mListChat.setAdapter(mChatAdapter);
		}

		mChatAdapter.notifyDataSetChanged();
		setNick(mBinder.getService().getNick());
		mDrinkCount = mBinder.getService().getDrinkCount();
		updateDrinkCount();

		if (!mBinder.getService().isConnected()) {
			try {
				// Only connect if we got Username and Password from
				// MainActivity, otherwise wait until BerryTube reconnect
				// normally
				if (mUsername != null && mPassword != null) {
					NotificationCompat.Builder note = new NotificationCompat.Builder(
							this);
					note.setSmallIcon(R.drawable.ic_stat_notify_berrytube);
					note.setLargeIcon(BitmapFactory.decodeResource(
							getResources(), R.drawable.ic_launcher));
					note.setContentTitle(getString(R.string.title_activity_chat));

					Intent intent = new Intent(this, MainActivity.class);
					intent.setAction(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);

					note.setContentIntent(PendingIntent.getActivity(this, 0,
							intent, PendingIntent.FLAG_UPDATE_CURRENT));
					mBinder.getService().connect(mUsername, mPassword, note);
				}
			} catch (MalformedURLException e) {
				Log.w(TAG, e);
			} catch (IllegalStateException e) {
				// already connected, ignore
			}
		}
	}

	private void autocompleteNick() {
		int selStart = Math.min(mEditChatMsg.getSelectionStart(),
				mEditChatMsg.getSelectionEnd());
		int selEnd = Math.max(mEditChatMsg.getSelectionStart(),
				mEditChatMsg.getSelectionEnd());
		String msg = mEditChatMsg.getText().toString();

		// no text selected, select word
		if (selStart == selEnd) {
			if (msg.length() > 0) {

				selStart--;
				for (int i = selStart; i >= 0; i--) {
					if (msg.charAt(i) == ' ')
						break;
					selStart--;
				}
				selStart++;

				for (int i = selEnd; i < msg.length(); i++) {
					if (msg.charAt(i) == ' ')
						break;
					selEnd++;
				}
				mEditChatMsg.setSelection(selStart, selEnd);
			}
		}

		if (msg.length() > 0) {
			selectUser(msg.substring(selStart, selEnd));
		} else {
			selectUser(null);
		}
	}
}
