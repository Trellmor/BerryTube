/*
 * BerryTube Service
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
package com.trellmor.berrytube;

import io.socket.SocketIO;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

/**
 * BerryTube handles to communication with the BerryTube Server and provides
 * synchronous callbacks for an UI application.
 * 
 * This is an android service intended to run in the same process as the main
 * application
 * 
 * @author Daniel Triendl
 * @see android.app.Service
 */
public class BerryTube extends Service implements Loader.OnLoadCompleteListener<Cursor> {
	private static final String TAG = BerryTube.class.getName();

	private SocketIO mSocket = null;
	private WeakReference<BerryTubeCallback> mCallback = new WeakReference<>(null);

	private URL mUrl;
	private String mUsername;
	private String mPassword;
	private String mNick = null;
	private Poll mPoll = null;
	private final ArrayList<ChatUser> mUsers = new ArrayList<>();
	private final ArrayList<String> mIgnoredUsers = new ArrayList<>();
	private CursorLoader mIgnoredUsersLoader;
	private int mDrinkCount = 0;
	private final Handler mHandler = new Handler();
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mServiceNotification = null;
	private NotificationCompat.Builder mMessageNotification = null;
	private final ArrayList<String> mMessageNotificationText = new ArrayList<>(5);
	private int mMessageNotificationCount = 0;
	private AsyncQueryHandler mQueryHandler;
	private boolean mReconnecting = false;
	private int mMaxReconnect = 20; // 20 * 30s = 10 minutes
	private int mReconnectCount = 0;

	private static final int KEY_NOTIFICATION_SERVICE = 1000;
	private static final int KEY_NOTIFICATION_MESSAGE = 2000;

	private static final int LOADER_IGNOREDUSERS = 1000;

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		mQueryHandler = new BerryTubeQueryHandler(getContentResolver());

		mIgnoredUsersLoader = new CursorLoader(this,
				UserProvider.CONTENT_URI_IGNOREDUSER,
				new String[] {UserProvider.IgnoredUserColumns.COLUMN_NAME},
				null, null, null);
		mIgnoredUsersLoader.registerListener(LOADER_IGNOREDUSERS, this);
		mIgnoredUsersLoader.startLoading();

		try {
			SocketIO.setDefaultSSLSocketFactory(new TLSSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Error getting SSL Context", e);
		} catch (KeyManagementException e) {
			Log.e(TAG, "Error getting SSL Context", e);
		}
	}

	@Override
	public void onDestroy() {
		if (mSocket != null && mSocket.isConnected())
			mSocket.disconnect();

		mSocket = null;

		if (mIgnoredUsersLoader != null) {
			mIgnoredUsersLoader.unregisterListener(this);
			mIgnoredUsersLoader.cancelLoad();
			mIgnoredUsersLoader.stopLoading();
		}

		stopForeground(true);

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new BerryTubeBinder(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flag, int startId) {
		return START_NOT_STICKY;
	}

	/**
	 * Connect to default BerryTube Server
	 * 
	 * @param username
	 *            Username to set
	 * @param password
	 *            Password to set, may be null if the user doesn't have an
	 *            account
	 * @throws MalformedURLException
	 * @throws IllegalStateException
	 */
	public void connect(String username, String password, NotificationCompat.Builder notification)
			throws MalformedURLException, IllegalStateException {
		connect(new URL("https://socket.berrytube.tv"), username, password,
				notification);
	}

	/**
	 * Connect to a BerryTube (compatible) server
	 * 
	 * @param url
	 *            Socket.io endpoint
	 * @param username
	 *            Username to set
	 * @param password
	 *            Password to set, may be null if the user doesn't have an
	 *            account
	 * @throws MalformedURLException
	 * @throws IllegalStateException
	 */
	public void connect(String url, String username, String password, NotificationCompat.Builder notification)
			throws MalformedURLException, IllegalStateException {
		connect(new URL(url), username, password, notification);
	}

	/**
	 * Connect to a BerryTube (combatible) server
	 * 
	 * @param url
	 *            Socket.io endpoint
	 * @param username
	 *            Username to set
	 * @param password
	 *            Password to set, may be null if the user doesn't have an
	 *            account
	 * @throws IllegalStateException
	 */
	public void connect(URL url, String username, String password, NotificationCompat.Builder notification)
			throws IllegalStateException {
		if (mSocket != null && mSocket.isConnected())
			throw new IllegalStateException("Already connected");

		if (username == null)
			throw new NullPointerException("username == null");

		mUsername = username;
		mPassword = password;
		mUrl = url;

		openSocket();

		mServiceNotification = notification;
		notification.setContentText(getText(R.string.connecting));
		notification.setTicker(getText(R.string.connecting));
		startForeground(KEY_NOTIFICATION_SERVICE, notification.build());
	}

	private void openSocket() {
		if (mSocket != null) {
			mReconnecting = true;
			mSocket.disconnect();
		}

		mSocket = new SocketIO(mUrl);
		mSocket.connect(new BerryTubeIOCallback(this));
	}

	/**
	 * Check if the socket is currently connected to the BerryTube server
	 * 
	 * @return Connection status
	 */
	public boolean isConnected() {
		return mSocket != null && mSocket.isConnected();
	}

	/**
	 * Set the callback handler
	 * 
	 * @param callback
	 *            Callback implementation instance
	 */
	public void setCallback(BerryTubeCallback callback) {
		mCallback = new WeakReference<>(callback);

		// Clear old notifications
		mMessageNotificationText.clear();
		mMessageNotificationCount = 0;
	}

	/**
	 * Convenience for <code>sendChat(String, 0)</code>
	 * 
	 * @param message
	 *            Chat message to send
	 */
	public void sendChat(String message) {
		sendChat(message, 0);
	}

	/**
	 * Submits a chat message to the server
	 * 
	 * @param message
	 *            Chat message to send
	 * @param flair
	 *            Number of flair to display, use 0 to show now flair
	 */
	public void sendChat(String message, int flair) {
		try {
			JSONObject msg = new JSONObject();
			msg.put("msg", message);

			JSONObject metadata = new JSONObject();
			metadata.put("flair", flair);
			msg.put("metadata", metadata);

			mSocket.emit("chat", msg);
		} catch (JSONException e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * Submit a vote on the current poll
	 * 
	 * @param option
	 *            Index of the poll option to vote for
	 */
	public void votePoll(int option) {
		try {
			JSONObject msg = new JSONObject();
			msg.put("op", option);

			mSocket.emit("votePoll", msg);
		} catch (JSONException e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * Get the current running poll
	 * 
	 * @return Current Poll or null if no poll is open
	 */
	public Poll getPoll() {
		return mPoll;
	}

	/**
	 * Set how many ChatMessages are kept in the ChatMsgBuffer
	 *
	 * Use 0 to disable the ChatMsgBuffer Use -1 to set it to unlimited
	 *
	 * @param chatMsgBufferSize	Size of the chat message buffer
	 */
	public void setChatMsgBufferSize(int chatMsgBufferSize) {
		//Remove stuff from DB
		mQueryHandler.startDelete(0, null, ChatMessageProvider.CONTENT_URI_MESSAGES,
				ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION + " = 0 AND " +
				ChatMessageProvider.MessageColumns._ID + " <= (" +
				"	SELECT " + ChatMessageProvider.MessageColumns._ID +
				"	FROM (" +
				"		SELECT " + ChatMessageProvider.MessageColumns._ID +
				"		FROM " + ChatMessageProvider.MessageColumns.TABLE_MESSAGES +
				"		WHERE " + ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION + " = 0 " +
				"		ORDER BY " + ChatMessageProvider.MessageColumns._ID + " DESC " +
				"		LIMIT 1 OFFSET " + chatMsgBufferSize +
				"	) foo" +
				")", null);


		mQueryHandler.startDelete(0, null, ChatMessageProvider.CONTENT_URI_MESSAGES,
				ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION + " = 1 AND " +
				ChatMessageProvider.MessageColumns._ID + " <= (" +
				"	SELECT " + ChatMessageProvider.MessageColumns._ID +
				"	FROM (" +
				"		SELECT " + ChatMessageProvider.MessageColumns._ID +
				"		FROM " + ChatMessageProvider.MessageColumns.TABLE_MESSAGES +
				"		WHERE " + ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION + " = 1 " +
				"		ORDER BY " + ChatMessageProvider.MessageColumns._ID + " DESC " +
				"		LIMIT 1 OFFSET " + chatMsgBufferSize +
				"	) foo" +
				")", null);
	}

	/**
	 * Get the nick that was returned from the server
	 * 
	 * @return Server side nick
	 */
	public String getNick() {
		return mNick;
	}

	/**
	 * Get the list of chat users
	 * 
	 * @return User list
	 */
	public ArrayList<ChatUser> getUsers() {
		return mUsers;
	}

	/**
	 * Get the number of drinks for the current video
	 * 
	 * @return Drink count
	 */
	public int getDrinkCount() {
		return mDrinkCount;
	}

	/**
	 * Set a notification that should be displayed if the users name is
	 * mentioned
	 * 
	 * ContentTitle, ContentText and Style will be set on demand
	 * 
	 * @param notification
	 *            NotificationCompate.Builder object initialized with default
	 *            values
	 */
	public void setNotification(NotificationCompat.Builder notification) {
		mMessageNotification = notification;
	}

	public static boolean isServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (BerryTube.class.getName()
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {
		mIgnoredUsers.clear();
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			final int POS_NAME = cursor.getColumnIndexOrThrow(UserProvider.IgnoredUserColumns.COLUMN_NAME);

			do {
				mIgnoredUsers.add(cursor.getString(POS_NAME));
			} while (cursor.moveToNext());
		}

		Collections.sort(mIgnoredUsers);
	}

	Handler getHandler() {
		return mHandler;
	}

	public void disableVideoMessages() {
		if (mSocket != null) {
			mSocket.emit("chatOnly");
		}
	}

	/**
	 * Clear notification messages
	 */
	public void clearNotifications() {
		ContentValues values = new ContentValues();
		values.put(ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION, false);
		mQueryHandler.startUpdate(0, null, ChatMessageProvider.CONTENT_URI_MESSAGES, values,
				ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION + " = 1", null);
	}

	class ConnectTask implements Runnable {
		public void run() {
			if (mSocket == null)
				return;

			mReconnecting = false;
			mReconnectCount = 0;

			try {
				JSONObject login = new JSONObject();
				login.put("nick", mUsername);
				login.put("pass", mPassword);

				mSocket.emit("setNick", login);
			} catch (JSONException e) {
				Log.w(TAG, e);
			}
			mServiceNotification.setContentText(getText(R.string.connected));
			mServiceNotification.setTicker(getText(R.string.connected));
			mNotificationManager.notify(KEY_NOTIFICATION_SERVICE,
					mServiceNotification.build());
		}
	}

	class DisconnectTask implements Runnable {
		@Override
		public void run() {
			if (mReconnecting) {
				return;
			}

			if (mCallback.get() != null) {
				mCallback.get().onDisconnect();
			}

			mServiceNotification.setContentText(getText(R.string.disconnected));
			mServiceNotification.setTicker(getText(R.string.disconnected));
			mNotificationManager.notify(KEY_NOTIFICATION_SERVICE,
					mServiceNotification.build());

			stopForeground(true);
			mNotificationManager.cancel(KEY_NOTIFICATION_SERVICE);
			mNotificationManager.cancel(KEY_NOTIFICATION_MESSAGE);
			BerryTube.this.stopSelf();
		}

	}

	class ErrorTask implements Runnable {
		@Override
		public void run() {
			if (mCallback.get() != null) {
				mCallback.get().onError();
			}

			mReconnectCount++;
			if (mReconnectCount > mMaxReconnect) {
				mReconnecting = false;
				if (mSocket != null) {
					mSocket.disconnect();
					return;
				}
			}

			mServiceNotification.setContentText(getText(R.string.connection_lost));
			mServiceNotification.setTicker(getText(R.string.connection_lost));
			mNotificationManager.notify(KEY_NOTIFICATION_SERVICE,
					mServiceNotification.build());

			mReconnecting = true;
			mSocket.disconnect();
			mSocket = null;

			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {

					openSocket();
				}
			}, 30 * 1000);
		}

	}

	class ChatMsgTask implements Runnable {
		private final ChatMessage mChatMsg;

		ChatMsgTask(ChatMessage chatMsg) {
			this.mChatMsg = chatMsg;
		}

		public void run() {
			if (Collections.binarySearch(mIgnoredUsers, mChatMsg.getNick()) >= 0) {
				return;
			}

			final boolean isNotification = mNick != null && mNick.length() > 0 &&
					mChatMsg.isHighlightable() &&
					!mChatMsg.getNick().equals(mNick) &&
					mChatMsg.getMsg().contains(mNick);

			ContentValues values = new ContentValues();
			values.put(ChatMessageProvider.MessageColumns.COLUMN_NICK, mChatMsg.getNick());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_MESSAGE, mChatMsg.getMsg());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_EMOTE, mChatMsg.getEmote());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_FLAIR, mChatMsg.getFlair());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_MULTI, mChatMsg.getMulti());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_TIMESTAMP, mChatMsg.getTimestamp());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_FLAUNT, mChatMsg.isFlaunt());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_TYPE, mChatMsg.getType());
			values.put(ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION, isNotification);
			values.put(ChatMessageProvider.MessageColumns.COLUMN_HIDDEN, mChatMsg.isHidden());
			mQueryHandler.startInsert(0, null, ChatMessageProvider.CONTENT_URI_MESSAGES, values);

			if (mCallback.get() == null && mMessageNotification != null) {
				if (isNotification) {
					String msg = mChatMsg.toString();
					mMessageNotificationCount++;

					while (mMessageNotificationText.size() > 5) {
						mMessageNotificationText.remove(0);
					}

					mMessageNotificationText.add(msg);

					String title = msg;
					if (mMessageNotificationCount > 1) {
						title = String.format(getString(R.string.new_messages),
								mMessageNotificationCount);
						NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
						inboxStyle.setBigContentTitle(title);
						for (String line : mMessageNotificationText) {
							inboxStyle.addLine(line);
						}
						mMessageNotification.setStyle(inboxStyle);
					} else {
						mMessageNotification.setStyle(null);
					}

					mMessageNotification.setTicker(msg);
					mMessageNotification.setContentTitle(title);
					mMessageNotification.setContentText(msg);
					mNotificationManager.notify(KEY_NOTIFICATION_MESSAGE,
							mMessageNotification.build());
				}
			}
		}

	}

	class SetNickTask implements Runnable {
		private final String nick;

		SetNickTask(String nick) {
			this.nick = nick;
		}

		public void run() {
			mNick = nick;

			if (mCallback.get() != null) {
				mCallback.get().onSetNick(nick);
			}
		}
	}

	class LoginErrorTask implements Runnable {
		private final String mError;

		LoginErrorTask(String error) {
			mError = error;
		}

		public void run() {
			if (mCallback.get() != null) {
				mCallback.get().onLoginError(mError);
			}
		}
	}

	class UserJoinPartTask implements Runnable {
		final static int ACTION_JOIN = 0;
		final static int ACTION_PART = 1;

		private final ChatUser user;
		private final int action;

		UserJoinPartTask(ChatUser user, int action) {
			this.user = user;
			this.action = action;
		}

		public void run() {
			switch (action) {
			case ACTION_JOIN:
				mUsers.add(user);
				break;
			case ACTION_PART:
				mUsers.remove(user);
				break;
			}
		}
	}

	class UserResetTask implements Runnable {
		public void run() {
			mUsers.clear();
		}
	}

	class DrinkCountTask implements Runnable {
		private final int count;

		DrinkCountTask(int count) {
			this.count = count;
		}

		public void run() {
			mDrinkCount = count;
			if (mCallback.get() != null) {
				mCallback.get().onDrinkCount(this.count);
			}
		}
	}

	class KickedTask implements Runnable {
		public void run() {
			mPoll = null;
			mUsers.clear();

			if (mCallback.get() != null) {
				mCallback.get().onKicked();
			}

			if (mSocket.isConnected())
				mSocket.disconnect();

			BerryTube.this.stopSelf();
		}
	}

	class NewPollTask implements Runnable {
		private final Poll mPoll;

		NewPollTask(Poll poll) {
			this.mPoll = poll;
		}

		public void run() {
			BerryTube.this.mPoll = mPoll;

			if (mCallback.get() != null) {
				mCallback.get().onNewPoll(mPoll);
			}
		}
	}

	class UpdatePollTask implements Runnable {
		private final int[] mVotes;

		UpdatePollTask(int[] votes) {
			mVotes = votes;
		}

		public void run() {
			if (mPoll != null) {
				mPoll.update(mVotes);
			}

			if (mCallback.get() != null) {
				mCallback.get().onUpdatePoll(mPoll);
			}
		}
	}

	class ClearPollTask implements Runnable {
		public void run() {
			mPoll = null;

			if (mCallback.get() != null) {
				mCallback.get().onClearPoll();
			}
		}
	}

	class NewVideoTask implements Runnable {
		private final String mName;
		private final String mId;
		private final String mType;

		NewVideoTask(String name, String id, String type) {
			this.mName = name;
			this.mId = id;
			this.mType = type;
		}

		public void run() {
			if (mCallback.get() != null) {
				mCallback.get().onVideoUpdate(mName, mId, mType);
			}
		}
	}

	private static class BerryTubeQueryHandler extends AsyncQueryHandler {
		BerryTubeQueryHandler(ContentResolver contentResolver) {
			super(contentResolver);
		}
	}
}
