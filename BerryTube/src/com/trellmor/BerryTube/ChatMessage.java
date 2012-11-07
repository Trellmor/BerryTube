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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class encapsulates a chat message
 * 
 * @author Daniel Triendl
 */
public class ChatMessage {
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

	private String nick;
	private String msg;
	private int emote = EMOTE_FALSE;
	private int flair = 0;
	private int multi;

	/**
	 * Constructs a <code>ChatMessage</code>
	 * 
	 * @param nick Sender name
	 * @param msg Text content
	 * @param emote Emote type
	 * @param flair Sender flair
	 * @param multi Multiplier for <code>EMOTE_DRINK</code> notifications
	 */
	public ChatMessage(String nick, String msg, int emote, int flair, int multi) {
		this.nick = nick;
		this.msg = msg;
		this.emote = emote;
		this.flair = flair;
	}

	/**
	 * Constructs a <code>ChatMessage</code> from an <code>JSONObject</code>
	 * 
	 * @param message <code>JSONObject<code> containing all the required fields to form a chat message
	 * @throws JSONException
	 */
	public ChatMessage(JSONObject message) throws JSONException {
		nick = message.getString("nick");
		msg = message.getString("msg");
		multi = message.getInt("multi");

		// check emote
		if (message.has("emote") && message.get("emote") instanceof String) {
			String emote = message.getString("emote");
			if (emote.compareTo("rcv") == 0)
				this.emote = EMOTE_RCV;
			else if (emote.compareTo("sweetiebot") == 0)
				this.emote = EMOTE_SWEETIEBOT;
			else if (emote.compareTo("spoiler") == 0)
				this.emote = EMOTE_SPOILER;
			else if (emote.compareTo("act") == 0)
				this.emote = EMOTE_ACT;
			else if (emote.compareTo("request") == 0)
				this.emote = EMOTE_REQUEST;
			else if (emote.compareTo("poll") == 0)
				this.emote = EMOTE_POLL;
			else if (emote.compareTo("drink") == 0)
				this.emote = EMOTE_DRINK;
		} else
			emote = 0;

		JSONObject metadata = message.getJSONObject("metadata");
		flair = metadata.getInt("flair");
	}

	/**
	 * Get the sender name
	 * 
	 * @return Sender name
	 */
	public String getNick() {
		return nick;
	}

	/**
	 * Get the chat messages content
	 * 
	 * @return Text content
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * Get the emote type
	 * 
	 * @return Emote type
	 */
	public int getEmote() {
		return emote;
	}

	/**
	 * Check if the message is an emote
	 * 
	 * @return
	 */
	public boolean isEmpote() {
		return emote != EMOTE_FALSE;
	}
	
	/**
	 * Check if a notification should be displayed if the users nick is mentioned in the message
	 * 
	 * @return
	 */
	public boolean isHighlightable() {
		switch (emote) {
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
		return flair;
	}

	/**
	 * Get the multiplication number for <code>EMOTE_DRINK</code> notifications
	 * 
	 * @return Multiplier
	 */
	public int getMulti() {
		return multi;
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

			return this.nick.toLowerCase().equals(msg.getNick().toLowerCase())
					&& this.msg.equals(msg.getMsg());
		}
		return false;
	}

	/**
	 * Converts the chat message to a String containing the sender name and the text
	 */
	@Override
	public String toString() {
		return nick + ": " + msg;
	}
}