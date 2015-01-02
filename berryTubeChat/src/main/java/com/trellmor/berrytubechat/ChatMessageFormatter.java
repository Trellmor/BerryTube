/*
 * BerryTubeChat android client
 * Copyright (C) 2012-2013 Daniel Triendl <trellmor@trellmor.com>
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
package com.trellmor.berrytubechat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.trellmor.berrymotes.EmotesFormatter;
import com.trellmor.berrytube.ChatMessage;
import com.trellmor.berrytube.ChatUser;

/**
 * This class is used to inflate and populate a <code>View</code> to display a
 * <code>ChatMessage</code>
 * 
 * @author Daniel Triendl
 * @see com.trellmor.berrytube.ChatMessage
 */
class ChatMessageFormatter {
	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private FlairGetter mFlairGetter = null;
	private String mNick = null;
	private final ChatMessageAdapter mAdapter;
	private final EmotesFormatter mEmotesFormatter;

	public ChatMessageFormatter(ChatMessageAdapter adapter, Context context) {
		mAdapter = adapter;
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mFlairGetter = new FlairGetter(mContext);
		mEmotesFormatter = new EmotesFormatter(mContext);
	}

	public View format(View view, ChatMessage message) {

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_DRINK:
			return formatDrinks(view, message);
		case ChatMessage.EMOTE_ACT:
		case ChatMessage.EMOTE_REQUEST:
		case ChatMessage.EMOTE_POLL:
		case ChatMessage.EMOTE_RCV:
		case ChatMessage.EMOTE_SPOILER:
			return formatEmote(view, message);
		default:
			return formatDefault(view, message);
		}
	}

	public void setNick(String nick) {
		mNick = nick;
	}

	private View formatDrinks(View view, ChatMessage message) {
		if (view == null || view.getId() != R.id.chat_item_drink
				|| containsBerryMotes(message.getMsg())) {
			view = mInflater.inflate(R.layout.chat_item_drink, null);
			view.setTag(R.id.text_chat_message,
					view.findViewById(R.id.text_chat_message));
			view.setTag(R.id.text_chat_drink_multiple,
					view.findViewById(R.id.text_chat_drink_multiple));
		}

		TextView textMessage = (TextView) view.getTag(R.id.text_chat_message);
		TextView textMulti = (TextView) view
				.getTag(R.id.text_chat_drink_multiple);

		String m = message.getNick() + ": "
				+ formatBerryMotes(message.getMsg()) + " "
				+ mContext.getString(R.string.chat_drink);

		textMessage.setText(Html.fromHtml(m, mFlairGetter, null));

		if (message.getMulti() > 1) {
			textMulti.setText(Integer.toString(message.getMulti()) + "x");
			textMulti.setVisibility(View.VISIBLE);
		} else {
			textMulti.setVisibility(View.GONE);
		}

		return view;
	}

	private View inflateDefault(View view, String message) {
		if (view == null || view.getId() != R.id.text_chat_message || containsBerryMotes(message)) {
			view = mInflater.inflate(R.layout.chat_item, null);
			view.setTag(R.id.text_chat_message,
					view.findViewById(R.id.text_chat_message));
		} else {
			TextView textMessage = (TextView) view
					.getTag(R.id.text_chat_message);
			textMessage.setTextAppearance(mContext,
					android.R.style.TextAppearance_Small);
			textMessage.setGravity(Gravity.LEFT);
		}

		return view;
	}

	private View formatDefault(View view, ChatMessage message) {
		view = inflateDefault(view, message.getMsg());
		TextView textMessage = (TextView) view.getTag(R.id.text_chat_message);

		textMessage.setText(formatChatMsg(message));

		return view;
	}

	private View formatEmote(View view, ChatMessage message) {

		view = inflateDefault(view, message.getMsg());
		TextView textMessage = (TextView) view.getTag(R.id.text_chat_message);

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_REQUEST:
			textMessage.setTextColor(Color.BLUE);
			textMessage.setText(Html.fromHtml(createTimestamp(message
					.getTimestamp())
					+ "<b><i>"
					+ message.getNick()
					+ " requests "
					+ formatBerryMotes(message.getMsg())
					+ "</i></b>"));
			break;
		case ChatMessage.EMOTE_ACT:
			textMessage.setTextColor(Color.GRAY);
			textMessage.setText(Html.fromHtml(createTimestamp(message
					.getTimestamp())
					+ "<i>"
					+ message.getNick()
					+ " "
					+ formatBerryMotes(message.getMsg()) + "</i>"));
			break;
		case ChatMessage.EMOTE_POLL:
			textMessage.setTextColor(Color.parseColor("#008000"));
			textMessage.setTextSize(18);
			textMessage.setGravity(Gravity.CENTER_HORIZONTAL);
			textMessage.setText(Html.fromHtml("<b>" + message.getNick()
					+ " created a new poll \""
					+ formatBerryMotes(message.getMsg()) + "\"</b>"));
			break;
		case ChatMessage.EMOTE_RCV:
			textMessage.setTextColor(Color.RED);
			textMessage.setTextSize(18);
			textMessage.setText(Html.fromHtml(
					createTimestamp(message.getTimestamp()) + message.getNick()
							+ ": " + formatBerryMotes(message.getMsg()),
					mFlairGetter, null));
			break;
		case ChatMessage.EMOTE_SPOILER:
			textMessage.setText(formatChatMsg(message));
			view.setOnClickListener(new SpoilerClickListener(message));
			break;
		default:
			textMessage.setText(formatChatMsg(message));
			break;
		}

		return view;
	}

	private String highlightNick(String msg) {
		if (mNick == null)
			return msg;

		if (msg.contains(mNick)) {
			// Simple html parsing
			StringBuilder result = new StringBuilder();
			StringBuilder text = new StringBuilder();
			boolean inHtml = false;
			for (int i = 0; i < msg.length(); i++) {
				char c = msg.charAt(i);
				if (c == '<') {
					result.append(text.toString().replace(mNick,
							"<font color=\"#ff0000\">" + mNick + "</font>"));
					text.delete(0, text.length());
					inHtml = true;
				}

				if (inHtml) {
					result.append(c);
					if (c == '>') {
						inHtml = false;
					}
				} else {
					text.append(c);
				}
			}
			msg = result.append(
					text.toString().replace(mNick,
							"<font color=\"#ff0000\">" + mNick + "</font>"))
					.toString();
		}
		return msg;
	}

	private String createTimestamp(long timeStamp) {
		String result = "";
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		if (prefs.getBoolean(MainActivity.KEY_TIMESTAMP, false)
				&& timeStamp > 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ",
					Locale.ENGLISH);
			result = sdf.format(new Date(timeStamp));
		}

		return result;
	}

	private String flauntNick(ChatMessage message) {
		if (message.isFlaunt()) {
			switch (message.getType()) {
			case ChatUser.TYPE_ADMIN:
				return "<font color=\"#008000\">" + message.getNick()
						+ "</font>";
			case ChatUser.TYPE_MOD:
				return "<font color=\"#FF0000\">" + message.getNick()
						+ "</font>";
			default:
				return message.getNick();
			}
		} else
			return message.getNick();
	}

	private Spanned formatChatMsg(ChatMessage message) {
		StringBuilder sb = new StringBuilder();
		sb.append(createTimestamp(message.getTimestamp()));
		sb.append("<b>").append(flauntNick(message)).append("</b>");
		if (message.getFlair() > 0) {
			sb.append("<img src=\"").append(message.getFlair()).append("\" />");
		}
		sb.append(": ");

		// flutter shit
		String m = formatBerryMotes(message.getMsg()).replaceAll(
				"<span class=\"flutter\">(.*)</span>",
				"<font color=\"#FF5499\">$1</font>");

		StringBuilder fontModifiers = new StringBuilder(); // For the
															// full-string
															// modifiers (as
															// distinct from
															// yay)

		// Spoiler coloring (since background highlighting is hard)
		if (message.isHidden()) {
			int color = mContext.getResources().getColor(
					R.color.background_chat);

			fontModifiers.append(" color=\"#")
					.append(Integer.toHexString(color).substring(2))
					.append("\"");
		}
		// implying
		else if (m.startsWith("&gt;")) {
			fontModifiers.append(" color=\"#789922\"");
		}

		// SWEETIEBOT
		if (message.getEmote() == ChatMessage.EMOTE_SWEETIEBOT) {
			fontModifiers.append(" face=\"courier new\"");
		}

		if (fontModifiers.length() > 0) {
			m = fontModifiers.insert(0, "<font").append(">").append(m)
					.append("</font>").toString();
		}

		// Spoilers
		if (message.getEmote() == ChatMessage.EMOTE_SPOILER) {
			sb.append("SPOILER: ");
		}

		if (message.isHighlightable()) {
			sb.append(highlightNick(m));
		} else {
			sb.append(m);
		}

		return Html.fromHtml(sb.toString(), mFlairGetter, null);
	}

	private String formatBerryMotes(String msg) {
		return mEmotesFormatter.formatString(msg);
	}

	private boolean containsBerryMotes(String msg) {
		return mEmotesFormatter.containsEmotes(msg);
	}

	class SpoilerClickListener implements View.OnClickListener {
		private final ChatMessage mMsg;

		public SpoilerClickListener(ChatMessage message) {
			mMsg = message;
		}

		@Override
		public void onClick(View v) {
			mMsg.toggleHidden();
			mAdapter.notifyDataSetChanged();
		}
	}
}
