package com.trellmor.BerryTubeChat;

import com.trellmor.BerryTube.ChatMessage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ChatMessageFormatter {
	public static View format(LayoutInflater inflator, View view,
			ChatMessage message) {

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_DRINK:
			return formatDefault(inflator, view, message);
		case ChatMessage.EMOTE_ACT:
		case ChatMessage.EMOTE_REQUEST:
			return formatDefault(inflator, view, message);
		default:
			return formatDefault(inflator, view, message);
		}
	}

	protected static View formatDefault(LayoutInflater inflator, View view,
			ChatMessage message) {

		view = inflator.inflate(R.layout.chat_item, null);
		TextView textChatMessage = (TextView) view
				.findViewById(R.id.text_chat_message);

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_REQUEST:
			textChatMessage.setTextColor(Color.BLUE);
			textChatMessage.setTypeface(null, Typeface.BOLD);
			textChatMessage.setText(message.getNick() + " requests "
					+ message.getMsg());
			break;
		case ChatMessage.EMOTE_ACT:
			textChatMessage.setTextColor(Color.GRAY);
			textChatMessage.setTypeface(null, Typeface.ITALIC);
			textChatMessage.setText(message.getNick() + " " + message.getMsg());
			break;
		default:
			Spanned msg = Html.fromHtml("<b>" + message.getNick() + "</b>: "
					+ message.getMsg());
			textChatMessage.setText(msg);
			break;

		}

		return view;
	}
}
