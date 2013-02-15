/*
 * BerryTubeChat android client
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
package com.trellmor.BerryTubeChat;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.trellmor.BerryTube.ChatMessage;

/**
 * Custom ChatMessage array adapter to create suitable views for the different
 * <code>ChatMessage</code> emote types
 * 
 * @author Daniel Triendl
 * @see android.widget.ArrayAdapter
 */
public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {

	private List<ChatMessage> mChatMsgList;
	private String mNick = null;
	private ChatMessageFormatter mFormatter = null;

	public ChatMessageAdapter(Context context, int textViewResourceId,
			List<ChatMessage> objects) {
		super(context, textViewResourceId, objects);

		mChatMsgList = objects;

		mFormatter = new ChatMessageFormatter(this, context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatMessage msg = mChatMsgList.get(position);

		return mFormatter.format(convertView, msg);
	}

	public void setNick(String nick) {
		mNick = nick;
		mFormatter.setNick(mNick);
	}

}
