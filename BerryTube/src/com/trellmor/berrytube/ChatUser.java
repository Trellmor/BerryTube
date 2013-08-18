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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Encapsulates a chat user
 * 
 * @author Daniel Triendl
 */
public class ChatUser implements Cloneable {
	/**
	 * Not logged in
	 */
	public final static int TYPE_NOBODY = -2;
	/**
	 * Not logged in (anonymous)
	 */
	public final static int TYPE_ANON = -1;
	/**
	 * Logged in
	 */
	public final static int TYPE_USER = 0;
	/**
	 * Moderator
	 */
	public final static int TYPE_MOD = 1;
	/**
	 * Administrator
	 */
	public final static int TYPE_ADMIN = 2;

	private String nick;
	private int type;

	/**
	 * Convenience for <code>ChatUser(String, -2)</code>
	 * 
	 * @param nick
	 *            Username
	 */
	public ChatUser(String nick) {
		this(nick, TYPE_NOBODY);
	}

	/**
	 * Constructs a <code>ChatUser</code>
	 * 
	 * @param nick
	 *            Username
	 * @param type
	 *            type
	 */
	public ChatUser(String nick, int type) {
		this.nick = nick;
		this.type = type;
	}

	/**
	 * Constructs a <code>ChatUser</code> from a <code>JSONObject</code>
	 * 
	 * @param user
	 *            <code>JSONObject<code> containing user data
	 * @throws JSONException
	 */
	public ChatUser(JSONObject user) throws JSONException {
		this(user.getString("nick"));

		if (user.has("type"))
			this.type = user.getInt("type");
	}

	/**
	 * Get the users name
	 * 
	 * @return Username
	 */
	public String getNick() {
		return nick;
	}

	/**
	 * Get the users type
	 * 
	 * @return type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Compares to lower case nick
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this.getClass() == obj.getClass()) {
			ChatUser objUser = (ChatUser) obj;

			// only compare nick
			return nick.toLowerCase().equals(objUser.getNick().toLowerCase());
		} else
			return false;
	}

	/**
	 * Creates and returns a deep copy of this ChatUser
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ChatUser clone() {
		return new ChatUser(this.nick, this.type);
	}

	/**
	 * Converts the ChatUser to a String
	 * 
	 * @return User nick
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nick;
	}
}
