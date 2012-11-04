package com.trellmor.BerryTube;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {
	public final static int EMOTE_FALSE = 0;
	public final static int EMOTE_RCV = 1;
	public final static int EMOTE_SWEETIEBOT = 2;
	public final static int EMOTE_SPOILER = 3;
	public final static int EMOTE_ACT = 4;
	public final static int EMOTE_REQUEST = 5;
	public final static int EMOTE_POLL = 6;
	public final static int EMOTE_DRINK = 7;

	private String nick;

	public String getNick() {
		return nick;
	};

	private String msg;

	public String getMsg() {
		return msg;
	}

	private int emote = EMOTE_FALSE;

	public int getEmote() {
		return emote;
	}

	public boolean isEmpote() {
		return emote != EMOTE_FALSE;
	}
	
	public boolean isHighlightable() {
		switch(emote) {
			case EMOTE_ACT:
			case EMOTE_FALSE:
				return true;
			default:
				return false;
		}
	}

	private int flair;

	public int getFlair() {
		return flair;
	}

	private int multi;

	public int getMulti() {
		return multi;
	}

	public ChatMessage(String nick, String msg, int emote, int flair, int multi) {
		this.nick = nick;
		this.msg = msg;
		this.emote = emote;
		this.flair = flair;
	}

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

	@Override
	public boolean equals(Object obj) {
		if (this.getClass() == obj.getClass()) {
			ChatMessage msg = (ChatMessage) obj;

			return this.nick.toLowerCase().equals(msg.getNick().toLowerCase())
					&& this.msg.equals(msg.getMsg());
		}
		return false;
	}

	@Override
	public String toString() {
		return nick + ": " + msg;
	}
}