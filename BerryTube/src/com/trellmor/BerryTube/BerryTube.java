package com.trellmor.BerryTube;

import io.socket.SocketIO;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Daniel
 * 
 */
public class BerryTube extends Service {
	private SocketIO mSocket = null;
	private final ArrayList<BerryTubeCallback> mCallbacks = new ArrayList<BerryTubeCallback>();

	private URL mUrl;
	private String mUsername;
	private String mPassword;
	private String mNick = null;
	private Poll mPoll = null;

	public Poll getPoll() {
		return mPoll;
	}

	private ArrayList<ChatMessage> mChatMsgBuffer = new ArrayList<ChatMessage>();

	public ArrayList<ChatMessage> getChatMsgBuffer() {
		return mChatMsgBuffer;
	}

	private int mChatMsgBufferSize = 100;

	public int getChatMsgBufferSize() {
		return mChatMsgBufferSize;
	}

	/**
	 * Set how many ChatMessages are kept in the ChatMsgBuffer
	 * 
	 * Use 0 to disable the ChatMsgBuffer Use -1 to set it to unlimited
	 * 
	 * @param chatMsgBufferSize
	 *            Set size of the ChatMsgBuffer
	 */
	public void setChatMsgBufferSize(int chatMsgBufferSize) {
		mChatMsgBufferSize = chatMsgBufferSize;

		if (mChatMsgBufferSize == 0)
			mChatMsgBuffer.clear();
		while (mChatMsgBuffer.size() > mChatMsgBufferSize)
			mChatMsgBuffer.remove(0);
	}

	public String getNick() {
		return mNick;
	}

	private final ArrayList<ChatUser> mUsers = new ArrayList<ChatUser>();

	public ArrayList<ChatUser> getUsers() {
		return mUsers;
	}

	private int mDrinkCount = 0;

	public int getDrinkCount() {
		return mDrinkCount;
	}

	private Handler mHandler = new Handler();

	Handler getHandler() {
		return mHandler;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		if (mSocket != null && mSocket.isConnected())
			mSocket.disconnect();

		mSocket = null;

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new BerryTubeBinder(this);
	}

	public void connect(String username, String password)
			throws MalformedURLException, IllegalStateException {
		connect(new URL("http://96.127.152.99:8344"), username, password);
	}

	public void connect(String url, String username, String password)
			throws MalformedURLException, IllegalStateException {
		connect(new URL(url), username, password);
	}

	public void connect(URL url, String username, String password)
			throws IllegalStateException {
		if (mSocket != null && mSocket.isConnected())
			throw new IllegalStateException("Already connected");

		mUrl = url;
		mUsername = username;
		mPassword = password;

		mSocket = new SocketIO(mUrl);
		mSocket.connect(new BerryTubeIOCallback(this));
	}

	public boolean isConnected() {
		if (mSocket == null)
			return false;
		else
			return mSocket.isConnected();
	}

	public void registerCallback(BerryTubeCallback callback) {
		mCallbacks.add(callback);
	}

	public void unregisterCallback(BerryTubeCallback callback) {
		mCallbacks.remove(callback);
	}

	public void sendChat(String message) {
		sendChat(message, 0);
	}
	
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
	
	public void votePoll(int option) {
		try {
			JSONObject msg = new JSONObject();
			msg.put("op", option);
			
			mSocket.emit("votePoll", msg);
		} catch (JSONException e) {
			Log.w(this.getClass().toString(), e);
		}
	}

	class ConnectTask implements Runnable {
		public void run() {
			if (mSocket == null)
				return;

			mSocket.emit("chatOnly");

			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] digest = md.digest(mPassword.getBytes());

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < digest.length; ++i) {
					sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100)
							.substring(1, 3));
				}

				String pass = sb.toString();

				JSONObject login = new JSONObject();
				login.put("nick", mUsername);
				login.put("pass", pass);

				mSocket.emit("setNick", login);
			} catch (NoSuchAlgorithmException e) {
				Log.w(this.getClass().toString(), e.getMessage());
			} catch (JSONException e) {
				Log.w(this.getClass().toString(), e.getMessage());
			}
		}
	}
	
	class DisconnectTask implements Runnable {
		@Override
		public void run() {
			for (BerryTubeCallback callback : mCallbacks) {
				callback.onDisconnect();
			}
		}
		
	}

	class ChatMsgTask implements Runnable {
		private ChatMessage mChatMsg;

		public ChatMsgTask(ChatMessage chatMsg) {
			this.mChatMsg = chatMsg;
		}

		public void run() {
			for (BerryTubeCallback callback : mCallbacks) {
				if (mChatMsgBufferSize != 0)
					mChatMsgBuffer.add(mChatMsg);

				if (mChatMsgBufferSize > 0) {
					while (mChatMsgBuffer.size() > mChatMsgBufferSize)
						mChatMsgBuffer.remove(0);
				}

				callback.onChatMessage(mChatMsg);
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
			for (BerryTubeCallback callback : mCallbacks) {
				callback.onSetNick(nick);
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
			for (BerryTubeCallback callback : mCallbacks) {
				callback.onDrinkCount(this.count);
			}
		}
	}

	class KickedTask implements Runnable {
		public void run() {
			mPoll = null;
			mUsers.clear();

			for (BerryTubeCallback callback : mCallbacks) {
				callback.onKicked();
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

			for (BerryTubeCallback callback : mCallbacks) {
				callback.onNewPoll(mPoll);
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

			for (BerryTubeCallback callback : mCallbacks) {
				callback.onUpatePoll(mPoll);
			}
		}
	}

	class ClearPollTask implements Runnable {
		public void run() {
			mPoll = null;

			for (BerryTubeCallback callback : mCallbacks) {
				callback.onClearPoll();
			}
		}
	}
}
