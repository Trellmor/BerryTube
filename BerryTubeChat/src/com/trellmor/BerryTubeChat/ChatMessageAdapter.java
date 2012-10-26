package com.trellmor.BerryTubeChat;

import java.util.List;

import com.trellmor.BerryTube.ChatMessage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {

	private LayoutInflater inflator;
	private List<ChatMessage> chatMsgList;

	public ChatMessageAdapter(Context context, int textViewResourceId,
			List<ChatMessage> objects) {
		super(context, textViewResourceId, objects);

		chatMsgList = objects;

		inflator = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatMessage msg = chatMsgList.get(position);

		return ChatMessageFormatter.format(inflator, convertView, msg);
	}
}
