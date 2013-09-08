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
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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
public class BerryTube extends Service {
	private SocketIO mSocket = null;
	private WeakReference<BerryTubeCallback> mCallback = new WeakReference<BerryTubeCallback>(null);

	private URL mUrl;
	private String mUsername;
	private String mPassword;
	private String mNick = null;
	private Poll mPoll = null;
	private ArrayList<ChatMessage> mChatMsgBuffer = new ArrayList<ChatMessage>();
	private int mChatMsgBufferSize = 100;
	private final ArrayList<ChatUser> mUsers = new ArrayList<ChatUser>();
	private int mDrinkCount = 0;
	private Handler mHandler = new Handler();
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mServiceNotification = null;
	private NotificationCompat.Builder mMessageNotification = null;
	private ArrayList<String> mMessageNotificationText = new ArrayList<String>(
			5);
	private int mMessageNotificationCount = 0;

	public static final int KEY_NOTIFICATION_SERVICE = 1000;
	public static final int KEY_NOTIFICATION_MESSAGE = 2000;

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onDestroy() {
		if (mSocket != null && mSocket.isConnected())
			mSocket.disconnect();

		mSocket = null;

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
	public void connect(String username, String password,
			NotificationCompat.Builder notification)
			throws MalformedURLException, IllegalStateException {
		connect(new URL("http://96.127.152.99:8344"), username, password,
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
	public void connect(String url, String username, String password,
			NotificationCompat.Builder notification)
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
	 * @throws MalformedURLException
	 * @throws IllegalStateException
	 */
	public void connect(URL url, String username, String password,
			NotificationCompat.Builder notification)
			throws IllegalStateException {
		if (mSocket != null && mSocket.isConnected())
			throw new IllegalStateException("Already connected");

		mUrl = url;
		if (username == null)
			throw new NullPointerException("username == null");

		mUsername = username;

		mPassword = password;

		mSocket = new SocketIO(mUrl);
		mSocket.connect(new BerryTubeIOCallback(this));

		mServiceNotification = notification;
		notification.setContentText(getText(R.string.connecting));
		notification.setTicker(getText(R.string.connecting));
		startForeground(KEY_NOTIFICATION_SERVICE, notification.build());
	}

	/**
	 * Check if the socket is currently connected to the BerryTube server
	 * 
	 * @return Connection status
	 */
	public boolean isConnected() {
		if (mSocket == null)
			return false;
		else
			return mSocket.isConnected();
	}

	/**
	 * Set the callback handler
	 * 
	 * @param callback
	 *            Callback implementation instance
	 */
	public void setCallback(BerryTubeCallback callback) {
		mCallback = new WeakReference<BerryTubeCallback>(callback);

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
			Log.w(this.getClass().toString(), e);
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
			Log.w(this.getClass().toString(), e);
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
	 * Get the chat message list
	 * 
	 * To configure the size of this list use
	 * <code>setChatMsgBufferSize(int)</code>
	 * 
	 * @return ArrayList containing chat messages
	 * @see com.Trellmor.BerryTube.BerryTube.setChatMsgBufferSize(int)
	 */
	public ArrayList<ChatMessage> getChatMsgBuffer() {
		return mChatMsgBuffer;
	}

	/**
	 * Get the configured chat message buffer size
	 * 
	 * @return chat message buffer max size
	 */
	public int getChatMsgBufferSize() {
		return mChatMsgBufferSize;
	}

	/**
	 * Set how many ChatMessages are kept in the ChatMsgBuffer
	 * 
	 * Use 0 to disable the ChatMsgBuffer Use -1 to set it to unlimited
	 * 
	 * @param chatMsgBufferSize
	 */
	public void setChatMsgBufferSize(int chatMsgBufferSize) {
		mChatMsgBufferSize = chatMsgBufferSize;

		if (mChatMsgBufferSize == 0)
			mChatMsgBuffer.clear();
		while (mChatMsgBuffer.size() > mChatMsgBufferSize)
			mChatMsgBuffer.remove(0);
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

	Handler getHandler() {
		return mHandler;
	}

	public void disableVideoMessages() {
		if (mSocket != null) {
			mSocket.emit("chatOnly");
		}
	}

	class ConnectTask implements Runnable {
		public void run() {
			if (mSocket == null)
				return;

			try {
				JSONObject login = new JSONObject();
				login.put("nick", mUsername);
				login.put("pass", mPassword);

				mSocket.emit("setNick", login);
			} catch (JSONException e) {
				Log.w(this.getClass().toString(), e.getMessage());
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
			if (mCallback.get() != null) {
				mCallback.get().onDisconnect();
			}

			mServiceNotification.setContentText(getText(R.string.disconnected));
			mServiceNotification.setTicker(getText(R.string.disconnected));
			mNotificationManager.notify(KEY_NOTIFICATION_SERVICE,
					mServiceNotification.build());

			stopForeground(true);
			mNotificationManager.cancel(KEY_NOTIFICATION_SERVICE);
			BerryTube.this.stopSelf();
		}

	}

	class ChatMsgTask implements Runnable {
		private ChatMessage mChatMsg;

		public ChatMsgTask(ChatMessage chatMsg) {
			this.mChatMsg = chatMsg;
		}

		public void run() {
			if (mChatMsgBufferSize != 0)
				mChatMsgBuffer.add(mChatMsg);

			if (mChatMsgBufferSize > 0) {
				while (mChatMsgBuffer.size() > mChatMsgBufferSize)
					mChatMsgBuffer.remove(0);
			}

			if (mCallback.get() != null) {
				mCallback.get().onChatMessage(mChatMsg);
			}

			if (mCallback.get() == null && mMessageNotification != null) {
				if (mNick != null && mNick.length() > 0
						&& mChatMsg.isHighlightable()
						&& !mChatMsg.getNick().equals(mNick)
						&& mChatMsg.getMsg().contains(mNick)) {

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
		private String nick;

		public SetNickTask(String nick) {
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
		private String mError;

		public LoginErrorTask(String error) {
			mError = error;
		}

		public void run() {
			if (mCallback.get() != null) {
				mCallback.get().onLoginError(mError);
			}
		}
	}

	class UserJoinPartTask implements Runnable {
		public final static int ACTION_JOIN = 0;
		public final static int ACTION_PART = 1;

		private ChatUser user;
		private int action;

		public UserJoinPartTask(ChatUser user, int action) {
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
		private int count;

		public DrinkCountTask(int count) {
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
		private Poll mPoll;

		public NewPollTask(Poll poll) {
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
		private int[] mVotes;

		public UpdatePollTask(int[] votes) {
			mVotes = votes;
		}

		public void run() {
			if (mPoll != null) {
				mPoll.update(mVotes);
			}

			if (mCallback.get() != null) {
				mCallback.get().onUpatePoll(mPoll);
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
		private String mName;
		private String mId;
		private String mType;

		public NewVideoTask(String name, String id, String type) {
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
}
