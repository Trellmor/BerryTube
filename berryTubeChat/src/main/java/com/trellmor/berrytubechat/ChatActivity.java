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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.trellmor.berrymotes.EmoteUtils;
import com.trellmor.berrytube.BerryTube;
import com.trellmor.berrytube.BerryTubeBinder;
import com.trellmor.berrytube.BerryTubeCallback;
import com.trellmor.berrytube.ChatMessage;
import com.trellmor.berrytube.ChatMessageProvider;
import com.trellmor.berrytube.ChatUser;
import com.trellmor.berrytube.Poll;
import com.trellmor.berrytube.UserProvider;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * BerryTubeChat chat window
 * 
 * @author Daniel
 */
public class ChatActivity extends AppCompatActivity {
	private static final String TAG = ChatActivity.class.getName();

	private static final String KEY_DRINKCOUT = "drinkCount";
	private static final String KEY_MYDRINKCOUNT = "myDrinkCount";

	private static final int REQUEST_CODE = 1;

	private static final int LOADER_CHAT = 1000;
	private static final int LOADER_NOTIFICATIONS = 2000;

	private ChatMessageAdapter mChatAdapter = null;
	private ListView mListChat;
	private TextView mTextNick;
	private EditText mEditChatMsg;
	private TextView mTextDrinks;
	private TextView mCurrentVideo;
	private DrawerLayout mDrawerLayout;
	private MenuItem mMenuPoll;
	private View mDrawerNotifications;
	private NotificationHelper mNotiHelper;
	private NotificationCompat.Builder mNotification = null;

	private BerryTubeBinder mBinder = null;
	private ServiceConnection mService = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBinder = null;
			mListChat.setAdapter(null);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			initService((BerryTubeBinder) service);
		}
	};

	private String mUsername = null;
	private String mPassword = null;
	private String mNick = "";
	private int mFlair = 0;
	private boolean mShowVideo = false;
	private boolean mFirstPrefLoad = true;
	private int mScrollback = 1000;
	private int mDrinkCount = 0;
	private int mMyDrinkCount = 0;
	private boolean mShowDrinkCount = true;
	private boolean mPopupPoll = false;
	private String mServer = "";
	private BerryTubeCallback mCallback = null;
	private boolean mLogout = false;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_chat);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mEditChatMsg = findViewById(R.id.edit_chat_msg);
		TextView.OnEditorActionListener chatMsgListener = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					sendChatMsg();
				} else if (actionId == EditorInfo.IME_NULL
						&& event.getAction() == KeyEvent.ACTION_UP
						&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					sendChatMsg();
				}
				return true;
			}
		};
		mEditChatMsg.setOnEditorActionListener(chatMsgListener);

		mTextDrinks = findViewById(R.id.text_drinks);
		registerForContextMenu(mTextDrinks);

		mCurrentVideo = findViewById(R.id.text_video);
		mCurrentVideo.setMovementMethod(LinkMovementMethod.getInstance());

		mTextNick = findViewById(R.id.text_nick);
		mTextNick.setText("Anonymous");

		mListChat = findViewById(R.id.list_chat);
		mChatAdapter = new ChatMessageAdapter(this);
		getLoaderManager().initLoader(LOADER_CHAT, null, new ChatMessageLoaderCallbacks(this, mChatAdapter));
		mListChat.setAdapter(mChatAdapter);
		mListChat.setOnItemLongClickListener(mChatListItemLongClickListener);
		mListChat.setOnItemClickListener(mChatListItemClickListener);

		mDrawerLayout = findViewById(R.id.drawer_layout);
		mDrawerNotifications = findViewById(R.id.drawer_notifications);

		ListView listNotifications = findViewById(R.id.list_notifications);
		listNotifications.setOnItemLongClickListener(mChatListItemLongClickListener);
		listNotifications.setOnItemClickListener(mChatListItemClickListener);

		ChatMessageAdapter chatAdapter = new ChatMessageAdapter(ChatActivity.this);
		Bundle args = new Bundle();
		args.putBoolean(ChatMessageLoaderCallbacks.KEY_NOTIFICATIONS, true);
		getLoaderManager().initLoader(LOADER_NOTIFICATIONS, args, new ChatMessageLoaderCallbacks(ChatActivity.this, chatAdapter));
		listNotifications.setAdapter(chatAdapter);

		if (!EmoteUtils.isBerryMotesInstalled(this, EmoteUtils.BERRYMOTES_VERSION_2_0_0)) {
			ImageView imageEmote = findViewById(R.id.image_emote);
			imageEmote.setVisibility(View.GONE);
		}

		mNotiHelper = new NotificationHelper(this);

		Intent intent = getIntent();
		mUsername = intent.getStringExtra(MainActivity.KEY_USERNAME);
		mPassword = intent.getStringExtra(MainActivity.KEY_PASSWORD);

		if (savedInstanceState != null) {
			mDrinkCount = savedInstanceState.getInt(KEY_DRINKCOUT);
			mMyDrinkCount = savedInstanceState.getInt(KEY_MYDRINKCOUNT);
			if (mUsername == null)
				mUsername = savedInstanceState.getString(MainActivity.KEY_USERNAME);
			if (mPassword == null)
				mPassword = savedInstanceState.getString(MainActivity.KEY_PASSWORD);
		}

		handleSharedText(intent.getStringExtra(MainActivity.KEY_SHARED_TEXT));

		startService(new Intent(this, BerryTube.class));
		bindService(new Intent(this, BerryTube.class), mService,
				BIND_ABOVE_CLIENT);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		handleSharedText(intent.getStringExtra(MainActivity.KEY_SHARED_TEXT));
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
		mMenuPoll = menu.findItem(R.id.menu_poll);
		if (mBinder != null && mBinder.getService().getPoll() != null) {
			mMenuPoll.setVisible(true);
		} else {
			mMenuPoll.setVisible(false);
		}
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
		Intent intent;

		switch (item.getItemId()) {
		case R.id.menu_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_users:
			selectUser(null);
			return true;
		case R.id.menu_ignore_user:
			ignoreUser();
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
			return true;
		case R.id.menu_autocomplete_nick:
			autocompleteNick();
			return true;
		case R.id.menu_notifications:
			if (mDrawerLayout.isDrawerOpen(mDrawerNotifications)) {
				mDrawerLayout.closeDrawer(mDrawerNotifications);
			} else {
				mDrawerLayout.openDrawer(mDrawerNotifications);
			}
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

	public void dismissNotifications(View view) {
		mDrawerLayout.closeDrawer(mDrawerNotifications);
		mBinder.getService().clearNotifications();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(KEY_DRINKCOUT, mDrinkCount);
		outState.putInt(KEY_MYDRINKCOUNT, mMyDrinkCount);
		outState.putString(MainActivity.KEY_USERNAME, mUsername);
		outState.putString(MainActivity.KEY_PASSWORD, mPassword);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
			case REQUEST_CODE:
				String code = data.getStringExtra(EmoteUtils.EXTRA_CODE);
				int start = Math.max(mEditChatMsg.getSelectionStart(), 0);
				int end = Math.max(mEditChatMsg.getSelectionEnd(), 0);
				mEditChatMsg.getText().replace(Math.min(start, end), Math.max(start, end), code, 0, code.length());
				break;
		}
	}

	private void createCallback() {
		mCallback = new BerryTubeCallback() {

			@Override
			public void onSetNick(String nick) {
				setNick(nick);
			}

			@Override
			public void onLoginError(String error) {
				Toast.makeText(ChatActivity.this, getText(R.string.login_error) + error, Toast.LENGTH_LONG).show();
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

				mMenuPoll.setVisible(true);
			}

			@Override
			public void onUpdatePoll(Poll poll) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onClearPoll() {
				mMenuPoll.setVisible(false);
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

			@Override
			public void onError() {
				mMenuPoll.setVisible(false);
				setNick(null);
			}
		};
	}

	private void loadPreferences() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		try {
			mScrollback = Integer.parseInt(settings.getString(MainActivity.KEY_SCROLLBACK, "1000"));
		} catch (NumberFormatException e) {
			mScrollback = 1000;
		}

		if (mScrollback <= 0)
			mScrollback = 1000;

		if (mBinder != null)
			mBinder.getService().setChatMsgBufferSize(mScrollback);

		try {
			mFlair = Integer.parseInt(settings.getString(MainActivity.KEY_FLAIR, "0"));
		} catch (NumberFormatException e) {
			mFlair = 0;
		}

		if (settings.getBoolean(MainActivity.KEY_SQUEE, false) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mNotification = mNotiHelper.getMessageNotification();

			Intent intent = new Intent(this, ChatActivity.class);
			intent.putExtra(MainActivity.KEY_USERNAME, mUsername);
			intent.putExtra(MainActivity.KEY_PASSWORD, mPassword);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
					| Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
					| Intent.FLAG_ACTIVITY_NO_HISTORY);

			mNotification.setContentIntent(PendingIntent.getActivity(this, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT));

			String squee = settings.getString(MainActivity.KEY_SQUEE_RINGTONE, null);
			if (!"".equals(squee)) {
				Uri squeeUri;
				if (squee == null) {
					squeeUri = Settings.System.DEFAULT_NOTIFICATION_URI;
				} else {
					squeeUri = Uri.parse(squee);
				}
				mNotification.setSound(squeeUri, AudioManager.STREAM_NOTIFICATION);
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

		mServer = settings.getString(MainActivity.KEY_SERVER, "");

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

			mTextDrinks.setText(Integer.toString(mMyDrinkCount) + "/" +
					Integer.toString(mDrinkCount) + " " + ((mDrinkCount == 1) ? getString(R.string.drink_count_single) : getString(R.string.drink_count_plural)));
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

	private void ignoreUser() {

		final ArrayList<String> userNicks = getUserList(null);

		if (userNicks.size() > 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_ignore_user);

			builder.setItems(userNicks.toArray(new String[userNicks.size()]),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String nick = userNicks.get(which);
							ContentValues values = new ContentValues();
							values.put(UserProvider.IgnoredUserColumns.COLUMN_NAME, nick);

							getContentResolver().insert(UserProvider.CONTENT_URI_IGNOREDUSER, values);
							dialog.dismiss();

							Toast.makeText(ChatActivity.this,
									String.format(getString(R.string.toast_ignoring_user), nick),
									Toast.LENGTH_SHORT).show();
						}
					});

			AlertDialog alert = builder.create();
			alert.show();
		} else {
			Toast.makeText(this, R.string.toast_no_users, Toast.LENGTH_SHORT).show();
		}
	}

	private ArrayList<String> getUserList(String filter) {ArrayList<ChatUser> userList = new ArrayList<>();
		for (ChatUser chatUser : mBinder.getService().getUsers()) {
			try {
				userList.add(chatUser.clone());
			} catch (CloneNotSupportedException e) {
				Log.w(TAG, e);
			}
		}
		Collections.sort(userList, new Comparator<ChatUser>() {

			@Override
			public int compare(ChatUser lhs, ChatUser rhs) {
				if (lhs.getType() == rhs.getType()) {
					return lhs.getNick().compareToIgnoreCase(rhs.getNick());
				} else if (lhs.getType() > rhs.getType()) {
					return -1;
				} else {
					return +1;
				}
			}
		});

		final ArrayList<String> userNicks = new ArrayList<>();
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

		return userNicks;
	}

	private void selectUser(String filter) {
		final ArrayList<String> userNicks = getUserList(filter);

		if (userNicks.size() > 1 || filter == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_user);

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
		} else if (userNicks.size() == 1) {
			replaceNick(userNicks.get(0));
		} else {
			Toast.makeText(this, R.string.toast_no_users, Toast.LENGTH_SHORT).show();
		}
	}

	private void replaceNick(String nick) {
		if (nick == null || "".equals(nick)) return;
		
		int selStart = Math.min(mEditChatMsg.getSelectionStart(), mEditChatMsg.getSelectionEnd());
		int selEnd = Math.max(mEditChatMsg.getSelectionStart(), mEditChatMsg.getSelectionEnd());
		String msg = mEditChatMsg.getText().toString();

		String insert = nick;
		
		if (selStart == 0 && (selEnd >= msg.length() || msg.charAt(selEnd) != ':')) {
			insert += ":";
		}
		
		if (selEnd == msg.length() || (selEnd < msg.length() + 1 && msg.charAt(selEnd) != ' ')) {
			insert += " ";
		}
		
		msg = msg.substring(0, selStart) + insert + msg.substring(selEnd);
		mEditChatMsg.setText(msg); // SetText to refresh suggestions from some keyboards
		mEditChatMsg.setSelection(selStart+ insert.length());
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

		setNick(mBinder.getService().getNick());
		mDrinkCount = mBinder.getService().getDrinkCount();
		updateDrinkCount();
		if (mBinder.getService().getPoll() != null && mMenuPoll != null) {
			mMenuPoll.setVisible(true);
		}

		if (!mBinder.getService().isConnected()) {
			try {
				// Only connect if we got Username and Password from
				// MainActivity, otherwise wait until BerryTube reconnect
				// normally
				if (mUsername != null && mPassword != null) {
					NotificationCompat.Builder noti = mNotiHelper.getServiceNotification();

					Intent intent = new Intent(this, ChatActivity.class);
					intent.setAction(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					intent.putExtra(MainActivity.KEY_USERNAME, mUsername);
					intent.putExtra(MainActivity.KEY_PASSWORD, mPassword);
					intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
							| Intent.FLAG_ACTIVITY_CLEAR_TOP
							| Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
							| Intent.FLAG_ACTIVITY_NO_HISTORY);

					noti.setContentIntent(PendingIntent.getActivity(this, 0,
							intent, PendingIntent.FLAG_UPDATE_CURRENT));
					if ("".equals(mServer)) {
						mBinder.getService().connect(mUsername, mPassword, noti);
					} else {
						mBinder.getService().connect(mServer, mUsername, mPassword, noti);
					}
				}
			} catch (MalformedURLException e) {
				Log.w(TAG, e);
				Toast.makeText(this, R.string.server_address_error, Toast.LENGTH_LONG).show();
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

	public void emoteClick(View view) {
		Intent intent = new Intent();
		intent.setAction(EmoteUtils.ACTION_GET_CODE);
		startActivityForResult(intent, REQUEST_CODE);
	}

	private void handleSharedText(String text) {
		if (text == null)
			return;

		if (mEditChatMsg.getText().length() > 0) {
			mEditChatMsg.append(" ");
		}
		mEditChatMsg.append(text);
	}

	private OnItemLongClickListener mChatListItemLongClickListener =  new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			Cursor cursor = (Cursor) parent.getItemAtPosition(position);
			ChatMessage msg = ((ChatMessageAdapter)parent.getAdapter()).getMessage(cursor);
			replaceNick(msg.getNick());
			if (mDrawerLayout.isDrawerOpen(mDrawerNotifications)) {
				mDrawerLayout.closeDrawer(mDrawerNotifications);
			}
			return false;
		}
	};

	private AdapterView.OnItemClickListener mChatListItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Cursor cursor = (Cursor) parent.getItemAtPosition(position);
			ChatMessage msg = ((ChatMessageAdapter)parent.getAdapter()).getMessage(cursor);
			if (msg.isHidden()) {
				final ContentValues values = new ContentValues();
				values.put(ChatMessageProvider.MessageColumns.COLUMN_HIDDEN, false);

				getContentResolver().update(
						ChatMessageProvider.CONTENT_URI_MESSAGES.buildUpon().
								appendPath(String.valueOf(msg.getID())).build(),
						values,
						null,
						null);
			}
		}
	};
}
