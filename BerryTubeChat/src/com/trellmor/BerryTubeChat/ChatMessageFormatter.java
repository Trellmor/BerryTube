package com.trellmor.BerryTubeChat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.trellmor.BerryTube.ChatMessage;

public class ChatMessageFormatter {
	public static View format(LayoutInflater inflator, View view,
			ChatMessage message, String myNick, Context context) {

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_DRINK:
			return formatDrinks(inflator, view, message);
		case ChatMessage.EMOTE_ACT:
		case ChatMessage.EMOTE_REQUEST:
		case ChatMessage.EMOTE_POLL:
		case ChatMessage.EMOTE_RCV:
			return formatEmote(inflator, view, message, context);
		default:
			return formatDefault(inflator, view, message, myNick, context);
		}
	}

	private static View formatDrinks(LayoutInflater inflator, View view,
			ChatMessage message) {
		view = inflator.inflate(R.layout.chat_item_drink, null);
		TextView textChatMessage = (TextView) view
				.findViewById(R.id.text_chat_message);
		TextView textChatMultiple = (TextView) view
				.findViewById(R.id.text_chat_drink_multiple);

		textChatMessage.setText(message.getNick() + ": " + message.getMsg());

		if (message.getMulti() > 1) {
			textChatMultiple
					.setText(Integer.toString(message.getMulti()) + "x");
			textChatMultiple.setVisibility(View.VISIBLE);
		}

		return view;
	}

	private static View formatDefault(LayoutInflater inflator, View view,
			ChatMessage message, String myNick, Context context) {
		view = inflator.inflate(R.layout.chat_item, null);

		TextView textChatMessage = (TextView) view
				.findViewById(R.id.text_chat_message);

		textChatMessage.setText(formatChatMsg(message, myNick, context));

		return view;
	}

	protected static View formatEmote(LayoutInflater inflator, View view,
			ChatMessage message, Context context) {

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
		case ChatMessage.EMOTE_POLL:
			textChatMessage.setTextColor(Color.parseColor("#008000"));
			textChatMessage.setTypeface(null, Typeface.BOLD_ITALIC);
			textChatMessage.setTextSize(18);
			textChatMessage.setGravity(Gravity.CENTER_HORIZONTAL);
			textChatMessage.setText(message.getNick()
					+ " created a new poll \"" + message.getMsg() + "\"");
			break;
		case ChatMessage.EMOTE_RCV:
			textChatMessage.setTextColor(Color.RED);
			textChatMessage.setTextSize(18);
			textChatMessage
					.setText(message.getNick() + ": " + message.getMsg());
		default:
			textChatMessage.setText(formatChatMsg(message, null, context));
			break;

		}

		return view;
	}

	private static String highlightNick(String nick, String msg) {
		if (nick == null)
			return msg;
		return msg.replace(nick, "<font color=\"#ff0000\">" + nick + "</font>");
	}

	private static Spanned formatChatMsg(ChatMessage message, String myNick, Context context) {
		StringBuffer sb = new StringBuffer();
		sb.append("<b>").append(message.getNick()).append("</b>");
		if (message.getFlair() > 0) {
			sb.append("<img src=\"").append(message.getFlair()).append("\" />");
		}
		sb.append(": ");

		// flutter shit
		String m = message.getMsg().replaceAll("<span class=\"flutter\">(.*)</span>",
				"<font color=\"#FF5499\">$1</font>");

		// implying
		if (m.startsWith("&gt;"))
			m = "<font color=\"#789922\">" + m + "</font>";

		sb.append(highlightNick(myNick, m));

		return Html.fromHtml(sb.toString(), new FlairGetter(context.getResources()), null);
	}
}
