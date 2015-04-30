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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * This class encapsulates a chat message
 * 
 * @author Daniel Triendl
 */
public class ChatMessage {
	private static final String TAG = ChatMessage.class.getName();
	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(
			"EEE, d MMM yyyy HH:mm:ss z", Locale.US);

	/**
	 * Message is not an emote
	 */
	public final static int EMOTE_FALSE = 0;
	/**
	 * Message is a royal Canterlot voice emote
	 */
	public final static int EMOTE_RCV = 1;
	/**
	 * Message is a sweetiebot emote
	 */
	public final static int EMOTE_SWEETIEBOT = 2;
	/**
	 * Message is a spoiler
	 */
	public final static int EMOTE_SPOILER = 3;
	/**
	 * Message is a /me activity
	 */
	public final static int EMOTE_ACT = 4;
	/**
	 * Message is a video request
	 */
	public final static int EMOTE_REQUEST = 5;
	/**
	 * Message is new poll notification
	 */
	public final static int EMOTE_POLL = 6;
	/**
	 * Message is a drink notification
	 */
	public final static int EMOTE_DRINK = 7;

	private long mID = -1;
	private String mNick;
	private String mMsg;
	private int mEmote = EMOTE_FALSE;
	private int mFlair = 0;
	private int mMulti;
	private long mTimeStamp;
	private boolean mHidden;
	private boolean mFlaunt = false;
	private int mType = 0;

	/**
	 * Constructs a <code>ChatMessage</code>
	 *
	 * @param nick 		Sender name
	 * @param msg 		Text content
	 * @param emote 	Emote type
	 */
	public ChatMessage(String nick, String msg, int emote) {
		this(-1, nick, msg, emote, 0, 0, System.currentTimeMillis(), false, ChatUser.TYPE_USER, emote != EMOTE_SPOILER);
	}

	/**
	 * Constructs a <code>ChatMessage</code>
	 *
	 * @param id		Message ID
	 * @param nick 		Sender name
	 * @param msg 		Text content
	 * @param emote 	Emote type
	 * @param flair 	Sender flair
	 * @param multi		Multiplier for <code>EMOTE_DRINK</code> notifications
	 * @param timeStamp	Message timestamp
	 * @param type		User level
	 * @param flaunt	User is flaunting
	 * @param hidden	Spoiler message is hidden
	 */
	public ChatMessage(long id, String nick, String msg, int emote, int flair, int multi, long timeStamp,
					   boolean flaunt, int type, boolean hidden) {
		this.mID = id;
		this.mNick = nick;
		this.mMsg = msg;
		this.mEmote = emote;
		this.mFlair = flair;
		this.mMulti = multi;
		this.mTimeStamp = timeStamp;
		this.mFlaunt = flaunt;
		this.mType = type;
		this.mHidden = hidden;
	}

	/**
	 * Constructs a <code>ChatMessage</code> from an <code>JSONObject</code>
	 * 
	 * @param message
	 *            <code>JSONObject<code> containing all the required fields to form a chat message
	 * @throws JSONException
	 */
	public ChatMessage(JSONObject message) throws JSONException {
		mNick = message.getString("nick");
		mMsg = message.getString("msg");
		mMulti = message.getInt("multi");
		mType = message.getInt("type");

		// check emote
		if (message.has("emote") && message.get("emote") instanceof String) {
			String emote = message.getString("emote");
			if (emote.compareTo("rcv") == 0)
				this.mEmote = EMOTE_RCV;
			else if (emote.compareTo("sweetiebot") == 0)
				this.mEmote = EMOTE_SWEETIEBOT;
			else if (emote.compareTo("spoiler") == 0)
				this.mEmote = EMOTE_SPOILER;
			else if (emote.compareTo("act") == 0)
				this.mEmote = EMOTE_ACT;
			else if (emote.compareTo("request") == 0)
				this.mEmote = EMOTE_REQUEST;
			else if (emote.compareTo("poll") == 0)
				this.mEmote = EMOTE_POLL;
			else if (emote.compareTo("drink") == 0)
				this.mEmote = EMOTE_DRINK;
		} else
			mEmote = 0;

		JSONObject metadata = message.getJSONObject("metadata");
		mFlair = metadata.optInt("flair");
		mFlaunt = metadata.optBoolean("nameflaunt");

		try {
			this.mTimeStamp = TIMESTAMP_FORMAT.parse(message.getString("timestamp")).getTime();
		} catch (ParseException pe) {
			Log.w(TAG, "Error parsing timestamp string");
			this.mTimeStamp = System.currentTimeMillis();
		}
		this.mHidden = this.mEmote == EMOTE_SPOILER || containsInlineSpoiler();
	}

	/**
	 * Get the messages database row ID
	 *
	 * @return ID or -1 if not loaded from database
	 */
	public long getID() {
		return mID;
	}

	/**
	 * Get the sender name
	 * 
	 * @return Sender name
	 */
	public String getNick() {
		return mNick;
	}

	/**
	 * Get the chat messages content
	 * 
	 * @return Text content
	 */
	public String getMsg() {
		return mMsg;
	}

	/**
	 * Get the emote type
	 * 
	 * @return Emote type
	 */
	public int getEmote() {
		return mEmote;
	}

	/**
	 * Check if the message is an emote
	 * 
	 * @return true if the message is an emote
	 */
	public boolean isEmote() {
		return mEmote != EMOTE_FALSE;
	}

	/**
	 * Check if a notification should be displayed if the users nick is
	 * mentioned in the message
	 * 
	 * @return true if the message can be highlighted
	 */
	public boolean isHighlightable() {
		switch (mEmote) {
		case EMOTE_ACT:
		case EMOTE_FALSE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Get the senders flair
	 * 
	 * @return Flair index
	 */
	public int getFlair() {
		return mFlair;
	}

	/**
	 * Get the multiplication number for <code>EMOTE_DRINK</code> notifications
	 * 
	 * @return Multiplier
	 */
	public int getMulti() {
		return mMulti;
	}

	/**
	 * Get the timestamp for this message
	 * 
	 * @return The timestamp, in milliseconds since 1970
	 */
	public long getTimestamp() {
		return mTimeStamp;
	}

	/**
	 * Get the user level
	 * 
	 * @return user lever
	 * @see com.trellmor.berrytube.ChatUser
	 */
	public int getType() {
		return mType;
	}

	/**
	 * User is flaunting and nick should be colored according to type
	 * 
	 * @return flaunting
	 */
	public boolean isFlaunt() {
		return mFlaunt;
	}

	/**
	 * Get whether or not this message is hidden. This is only applicable if the
	 * message's emote type is <code>EMOTE_SPOILER</code> or if the message
	 * contains inline spoiler tags.
	 * 
	 * @return <b>true</b> if this message is a <code>EMOTE_SPOILER</code> and
	 *         if its hidden flag is true; <b>false</b> otherwise
	 */
	public boolean isHidden() {
		return mHidden;
	}

	public boolean containsInlineSpoiler() {
		return mMsg.contains("<span class=\"spoiler\">");
	}

	/**
	 * Compares the lower case nick and the message
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this.getClass() == obj.getClass()) {
			ChatMessage msg = (ChatMessage) obj;

			return this.mNick.toLowerCase(Locale.ENGLISH).equals(
					msg.getNick().toLowerCase(Locale.ENGLISH))
					&& this.mMsg.equals(msg.getMsg());
		}
		return false;
	}

	/**
	 * Converts the chat message to a String containing the sender name and the
	 * text
	 */
	@Override
	public String toString() {
		return mNick + ": " + mMsg;
	}
}