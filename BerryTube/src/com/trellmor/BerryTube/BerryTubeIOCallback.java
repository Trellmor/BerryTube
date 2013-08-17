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
package com.trellmor.BerryTube;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

/**
 * Internal class to handle callbacks from <code>SocketIO</code>
 * 
 * @author Daniel Triendl
 * @see IOCallback
 * @see SocketIO
 */
class BerryTubeIOCallback implements IOCallback {
	private BerryTube berryTube;

	public BerryTubeIOCallback(BerryTube berryTube) {
		this.berryTube = berryTube;
	}

	/**
	 * @see io.socket.IOCallback#onDisconnect()
	 */
	public void onDisconnect() {
		berryTube.getHandler().post(berryTube.new DisconnectTask());
		Log.i(this.getClass().toString(), "Disconnected");
	}

	/**
	 * @see io.socket.IOCallback#onConnect()
	 */
	public void onConnect() {
		berryTube.getHandler().post(berryTube.new ConnectTask());
	}

	/**
	 * @see io.socket.IOCallback#onMessage(java.lang.String,
	 *      io.socket.IOAcknowledge)
	 */
	public void onMessage(String data, IOAcknowledge ack) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see io.socket.IOCallback#onMessage(org.json.JSONObject,
	 *      io.socket.IOAcknowledge)
	 */
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see io.socket.IOCallback#on(java.lang.String, io.socket.IOAcknowledge,
	 *      java.lang.Object[])
	 */
	public void on(String event, IOAcknowledge ack, Object... args) {
		if (event.compareTo("chatMsg") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject jsonMsg = (JSONObject) args[0];

				try {
					berryTube.getHandler().post(
							berryTube.new ChatMsgTask(new ChatMessage(jsonMsg
									.getJSONObject("msg"))));
				} catch (JSONException e) {
					Log.e(this.getClass().toString(), "chatMsg", e);
				}
			} else
				Log.w(this.getClass().toString(),
						"chatMsg message is not a JSONObject");
		} else if (event.compareTo("setNick") == 0) {
			if (args.length >= 1 && args[0] instanceof String) {
				berryTube.getHandler().post(
						berryTube.new SetNickTask((String) args[0]));
			} else
				Log.w(this.getClass().toString(),
						"setNick message is not a String");
		} else if (event.compareTo("loginError") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject error = (JSONObject) args[0];
				berryTube.getHandler().post(berryTube.new LoginErrorTask(error.optString("message", "Login failed")));
			}
		} else if (event.compareTo("userJoin") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject user = (JSONObject) args[0];
				try {
					berryTube.getHandler().post(
							berryTube.new UserJoinPartTask(new ChatUser(user),
									BerryTube.UserJoinPartTask.ACTION_JOIN));
				} catch (JSONException e) {
					Log.e(this.getClass().toString(), "userJoin", e);
				}
			} else
				Log.w(this.getClass().toString(),
						"userJoin message is not a JSONObject");
		} else if (event.compareTo("newChatList") == 0) {
			berryTube.getHandler().post(berryTube.new UserResetTask());
			if (args.length >= 1 && args[0] instanceof JSONArray) {
				JSONArray users = (JSONArray) args[0];
				for (int i = 0; i < users.length(); i++) {
					JSONObject user = users.optJSONObject(i);
					if (user != null) {
						try {
							berryTube
									.getHandler()
									.post(berryTube.new UserJoinPartTask(
											new ChatUser(user),
											BerryTube.UserJoinPartTask.ACTION_JOIN));
						} catch (JSONException e) {
							Log.e(this.getClass().toString(), "newChatList", e);
						}
					}
				}
			} else
				Log.w(this.getClass().toString(),
						"newChatList message is not a JSONArray");
		} else if (event.compareTo("userPart") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject user = (JSONObject) args[0];
				try {
					berryTube.getHandler().post(
							berryTube.new UserJoinPartTask(new ChatUser(user),
									BerryTube.UserJoinPartTask.ACTION_PART));
				} catch (JSONException e) {
					Log.e(this.getClass().toString(), "userPart", e);
				}
			} else
				Log.w(this.getClass().toString(),
						"userPart message is not a JSONObject");
		} else if (event.compareTo("drinkCount") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject drinks = (JSONObject) args[0];
				berryTube.getHandler().post(
						berryTube.new DrinkCountTask(drinks.optInt("drinks")));
			}
		} else if (event.compareTo("kicked") == 0) {
			berryTube.getHandler().post(berryTube.new KickedTask());
		} else if (event.compareTo("newPoll") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject poll = (JSONObject) args[0];
				ChatMessage msg;

				// Send chat message for new poll
				try {
					msg = new ChatMessage(poll.getString("creator"),
							poll.getString("title"), ChatMessage.EMOTE_POLL, 0,
							0, false, 1);
					berryTube.getHandler().post(berryTube.new ChatMsgTask(msg));
				} catch (JSONException e) {
					Log.e(this.getClass().toString(), "newPoll", e);
				}

				// Create new poll
				try {
					berryTube.getHandler().post(
							berryTube.new NewPollTask(new Poll(poll)));
				} catch (JSONException e) {
					Log.e(this.getClass().toString(), "newPoll", e);
				}
			}
		} else if (event.compareTo("updatePoll") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject poll = (JSONObject) args[0];
				try {
					JSONArray votes = poll.getJSONArray("votes");
					int[] voteArray = new int[votes.length()];
					for (int i = 0; i < votes.length(); i++) {
						voteArray[i] = votes.optInt(i, -1);
					}
					berryTube.getHandler().post(
							berryTube.new UpdatePollTask(voteArray));
				} catch (JSONException e) {
					Log.e(this.getClass().toString(), "updatePoll", e);
				}
			}
		} else if (event.compareTo("clearPoll") == 0) {
			berryTube.getHandler().post(berryTube.new ClearPollTask());
		} else if (event.compareTo("forceVideoChange") == 0
				|| event.compareTo("hbVideoDetail") == 0) {
			if (args.length >= 1 && args[0] instanceof JSONObject) {
				JSONObject videoMsg = (JSONObject) args[0];
				try {
					JSONObject video = videoMsg.getJSONObject("video");
					String name = URLDecoder.decode(
							video.getString("videotitle"), "UTF-8");
					String id = video.getString("videoid");
					String type = video.getString("videotype");
					berryTube.getHandler().post(
							berryTube.new NewVideoTask(name, id, type));
				} catch (JSONException e) {
					Log.w(this.getClass().toString(), e);
				} catch (UnsupportedEncodingException e) {
					Log.w(this.getClass().toString(), e);
				}
			}
		}
	}

	/**
	 * @see io.socket.IOCallback#onError(io.socket.SocketIOException)
	 */
	public void onError(SocketIOException socketIOException) {
		Log.wtf(this.getClass().toString(), socketIOException);
	}
}
