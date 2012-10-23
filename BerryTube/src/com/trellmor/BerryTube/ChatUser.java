package com.trellmor.BerryTube;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatUser implements Cloneable {
	public final static int TYPE_NOBODY = -2;
	public final static int TYPE_ANON = -1;
	public final static int TYPE_USER = 0;
	public final static int TYPE_MOD = 1;
	public final static int TYPE_ADMIN = 2;

	private String nick;

	public String getNick() {
		return nick;
	}

	private int type;

	public int getType() {
		return type;
	}

	public ChatUser(String nick) {
		this(nick, TYPE_NOBODY);
	}
	
	public ChatUser(String nick, int type) {
		this.nick = nick;
		this.type = type;
	}
	
	public ChatUser(JSONObject user) throws JSONException {
		this(user.getString("nick"));
		
		if (user.has("type"))
			this.type = user.getInt("type");			
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this.getClass() == obj.getClass()) {
			ChatUser objUser = (ChatUser)obj;
			
			return nick.equals(objUser.getNick()); //only compare nick
		} else
			return false;
	}
	
	@Override
	public ChatUser clone() {
		return new ChatUser(this.nick, this.type); 
	}
	
	@Override
	public String toString() {
		return nick;
	}
}
