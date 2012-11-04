package com.trellmor.BerryTubeChat;

import java.util.List;

import com.trellmor.BerryTube.ChatMessage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {

	private List<ChatMessage> mChatMsgList;
	private String mNick = null;
	private ChatMessageFormatter mFormatter = null;
	
	public String getNick() {
		return mNick;
	}
	
	public void setNick(String nick) {
		mNick = nick;
		mFormatter.setNick(mNick);
	}

	public ChatMessageAdapter(Context context, int textViewResourceId,
			List<ChatMessage> objects) {
		super(context, textViewResourceId, objects);

		mChatMsgList = objects;

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mFormatter = new ChatMessageFormatter(context, inflater);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatMessage msg = mChatMsgList.get(position);

		return mFormatter.format(convertView, msg);
	}
}
