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

import com.trellmor.BerryTube.ChatMessage;

/**
 * This class is used to inflate and populate a <code>View</code> to display a
 * <code>ChatMessage</code>
 * 
 * @author Daniel Triendl
 * @see com.trellmor.BerryTube.ChatMessage
 */
public class ChatMessageFormatter {
	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private FlairGetter mFlairGetter = null;
	private String mNick = null;

	public ChatMessageFormatter(Context contest, LayoutInflater inflater) {
		mContext = contest;
		mInflater = inflater;

		mFlairGetter = new FlairGetter(mContext.getResources());
	}

	static class ViewHolder {
		public TextView textChatMessage;
		public TextView textChatMultiple;
	}

	public View format(View view, ChatMessage message) {

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_DRINK:
			return formatDrinks(view, message);
		case ChatMessage.EMOTE_ACT:
		case ChatMessage.EMOTE_REQUEST:
		case ChatMessage.EMOTE_POLL:
		case ChatMessage.EMOTE_RCV:
			return formatEmote(view, message);
		default:
			return formatDefault(view, message);
		}
	}

	public void setNick(String nick) {
		mNick = nick;
	}

	private View formatDrinks(View view, ChatMessage message) {
		ViewHolder holder;

		if (view == null || view.getId() != R.id.chat_item_drink) {
			view = mInflater.inflate(R.layout.chat_item_drink, null);
			holder = new ViewHolder();
			holder.textChatMessage = (TextView) view
					.findViewById(R.id.text_chat_message);
			holder.textChatMultiple = (TextView) view
					.findViewById(R.id.text_chat_drink_multiple);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		holder.textChatMessage.setText(message.getNick() + ": "
				+ message.getMsg() + " "
				+ mContext.getString(R.string.chat_drink));

		if (message.getMulti() > 1) {
			holder.textChatMultiple
					.setText(Integer.toString(message.getMulti()) + "x");
			holder.textChatMultiple.setVisibility(View.VISIBLE);
		} else {
			holder.textChatMultiple.setVisibility(View.GONE);
		}

		return view;
	}

	private View inflateDefault(View view) {
		if (view == null || view.getId() != R.id.text_chat_message) {
			view = mInflater.inflate(R.layout.chat_item, null);
			ViewHolder holder = new ViewHolder();
			holder.textChatMessage = (TextView) view
					.findViewById(R.id.text_chat_message);
			view.setTag(holder);
		} else {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.textChatMessage.setTextAppearance(mContext,
					android.R.style.TextAppearance_Small);
			holder.textChatMessage.setGravity(Gravity.LEFT);
		}

		return view;
	}

	private View formatDefault(View view, ChatMessage message) {
		view = inflateDefault(view);
		ViewHolder holder = (ViewHolder) view.getTag();

		holder.textChatMessage.setText(formatChatMsg(message));

		return view;
	}

	private View formatEmote(View view, ChatMessage message) {

		view = inflateDefault(view);
		ViewHolder holder = (ViewHolder) view.getTag();

		switch (message.getEmote()) {
		case ChatMessage.EMOTE_REQUEST:
			holder.textChatMessage.setTextColor(Color.BLUE);
			holder.textChatMessage.setText(Html
					.fromHtml(createTimestamp(message.getTimestamp())
							+ "<b><i>" + message.getNick() + " requests "
							+ message.getMsg() + "</i></b>"));
			break;
		case ChatMessage.EMOTE_ACT:
			holder.textChatMessage.setTextColor(Color.GRAY);
			holder.textChatMessage.setText(Html
					.fromHtml(createTimestamp(message.getTimestamp()) + "<i>"
							+ message.getNick() + " " + message.getMsg()
							+ "</i>"));
			break;
		case ChatMessage.EMOTE_POLL:
			holder.textChatMessage.setTextColor(Color.parseColor("#008000"));
			holder.textChatMessage.setTextSize(18);
			holder.textChatMessage.setGravity(Gravity.CENTER_HORIZONTAL);
			holder.textChatMessage.setText(Html.fromHtml("<b>"
					+ message.getNick() + " created a new poll \""
					+ message.getMsg() + "\"</b>"));
			break;
		case ChatMessage.EMOTE_RCV:
			holder.textChatMessage.setTextColor(Color.RED);
			holder.textChatMessage.setTextSize(18);
			holder.textChatMessage.setText(createTimestamp(message
					.getTimestamp())
					+ message.getNick()
					+ ": "
					+ message.getMsg());
		default:
			holder.textChatMessage.setText(formatChatMsg(message));
			break;

		}

		return view;
	}

	private String highlightNick(String msg) {
		if (mNick == null)
			return msg;
		return msg.replace(mNick, "<font color=\"#ff0000\">" + mNick
				+ "</font>");
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

	private Spanned formatChatMsg(ChatMessage message) {
		StringBuffer sb = new StringBuffer();
		sb.append(createTimestamp(message.getTimestamp()));
		sb.append("<b>").append(message.getNick()).append("</b>");
		if (message.getFlair() > 0) {
			sb.append("<img src=\"").append(message.getFlair()).append("\" />");
		}
		sb.append(": ");

		// flutter shit
		String m = message.getMsg().replaceAll(
				"<span class=\"flutter\">(.*)</span>",
				"<font color=\"#FF5499\">$1</font>");

		// implying
		if (m.startsWith("&gt;"))
			m = "<font color=\"#789922\">" + m + "</font>";

		if (message.isHighlightable()) {
			sb.append(highlightNick(m));
		} else {
			sb.append(m);
		}

		return Html.fromHtml(sb.toString(), mFlairGetter, null);
	}
}
