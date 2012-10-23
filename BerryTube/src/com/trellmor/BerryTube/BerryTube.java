package com.trellmor.BerryTube;

import io.socket.SocketIO;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

public class BerryTube {
	private String username;

	public String getUserName() {
		return username;
	}

	public void setUserName(String username) {
		this.username = username;
	}

	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	private URL url;

	private SocketIO socket;
	
	private final Handler handler = new Handler();

	protected final Handler getHandler() {
		return handler;
	}

	private BerryTubeCallback callback;

	public URL getURL() {
		return url;
	}

	public void setURL(URL url) {
		this.url = url;
	}

	public BerryTube(String username, String password)
			throws MalformedURLException {
		this(new URL("http://96.127.152.99:8344"), username, password);
	}

	public BerryTube(String url, String username, String password)
			throws MalformedURLException {
		this(new URL(url), username, password);
	}

	public BerryTube(URL url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public void finalize() {
		this.disconnect();
	}

	public void connect(BerryTubeCallback callback) {
		if (socket != null) {
			if (socket.isConnected()) {
				throw new IllegalStateException("Already connected");
			}
			socket = null;
		}

		if (callback == null) {
			throw new IllegalArgumentException();
		}

		this.callback = callback;

		socket = new SocketIO(url);
		socket.connect(new BerryTubeIOCallback(this));
	}

	public void disconnect() {
		if (socket != null) {
			if (socket.isConnected()) {
				socket.disconnect();
			}
			socket = null;
		}
	}

	public boolean isConnected() {
		if (socket == null)
			return false;
		else
			return socket.isConnected();
	}

	public void sendChat(String message) {
		try {
			JSONObject msg = new JSONObject();
			msg.put("msg", message);

			JSONObject metadata = new JSONObject();
			metadata.put("flair", 0);
			msg.put("metadata", metadata);

			socket.emit("chat", msg);
		} catch (JSONException e) {
			Log.w(this.getClass().toString(), e);
		}
	}

	class ConnectTask implements Runnable {
		public void run() {
			socket.emit("chatOnly");

			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] digest = md.digest(password.getBytes());

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < digest.length; ++i) {
					sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100)
							.substring(1, 3));
				}

				String pass = sb.toString();

				JSONObject login = new JSONObject();
				login.put("nick", username);
				login.put("pass", pass);

				socket.emit("setNick", login);
			} catch (NoSuchAlgorithmException e) {
				Log.w(this.getClass().toString(), e.getMessage());
			} catch (JSONException e) {
				Log.w(this.getClass().toString(), e.getMessage());
			}
		}
	}

	class ChatMsgTask implements Runnable {
		private ChatMessage chatMsg;

		public ChatMsgTask(ChatMessage chatMsg) {
			this.chatMsg = chatMsg;
		}

		public void run() {
			callback.onChatMessage(chatMsg);
		}

	}

	class SetNickTask implements Runnable {
		private String nick;

		public SetNickTask(String nick) {
			this.nick = nick;
		}

		public void run() {
			callback.onSetNick(nick);
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
			switch(action) {
			case ACTION_JOIN:
				callback.onUserJoin(user);
				break;
			case ACTION_PART:
				callback.onUserPart(user);
			}
		}
	}
	
	class UserResetTask implements Runnable {		
		public void run() {
			callback.onUserReset();
		}
	}
	
	class DrinkCountTask implements Runnable {
		private int count;
		
		public DrinkCountTask(int count) {
			this.count = count;
		}

		public void run() {
			callback.onDrinkCount(this.count);
		}
	}
}
