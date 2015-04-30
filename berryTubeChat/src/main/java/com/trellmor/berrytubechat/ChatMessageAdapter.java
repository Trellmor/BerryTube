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

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.trellmor.berrytube.ChatMessage;
import com.trellmor.berrytube.ChatMessageProvider;

/**
 * Custom ChatMessage array adapter to create suitable views for the different
 * <code>ChatMessage</code> emote types
 * 
 * @author Daniel Triendl
 * @see android.widget.ArrayAdapter
 */
class ChatMessageAdapter extends CursorAdapter {
	private ChatMessageFormatter mFormatter = null;

	private int mPosID;
	private int mPosNick;
	private int mPosMessage;
	private int mPosEmote;
	private int mPosFlair;
	private int mPosMulti;
	private int mPosTimestamp;
	private int mPosFlaunt;
	private int mPosType;
	private int mPosHidden;

	public ChatMessageAdapter(Context context) {
		super(context, null, 0);

		mFormatter = new ChatMessageFormatter(context);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mFormatter.inflate(getMessage(cursor));
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		mFormatter.format(view, getMessage(cursor));
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		Cursor c = (Cursor) getItem(position);
		ChatMessage msg = getMessage(c);
		return mFormatter.getViewType(msg);
	}

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		Cursor oldCursor = super.swapCursor(newCursor);
		if (oldCursor != newCursor && newCursor != null) {
			mPosID = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns._ID);
			mPosNick = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_NICK);
			mPosMessage = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_MESSAGE);
			mPosEmote = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_EMOTE);
			mPosFlair = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_FLAIR);
			mPosMulti = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_MULTI);
			mPosTimestamp = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_TIMESTAMP);
			mPosFlaunt = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_FLAUNT);
			mPosType = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_TYPE);
			mPosHidden = newCursor.getColumnIndex(ChatMessageProvider.MessageColumns.COLUMN_HIDDEN);
		}
		return oldCursor;
	}

	public ChatMessage getMessage(Cursor cursor) {
		ChatMessage msg = new ChatMessage(
				cursor.getLong(mPosID),
				cursor.getString(mPosNick),
				cursor.getString(mPosMessage),
				cursor.getInt(mPosEmote),
				cursor.getInt(mPosFlair),
				cursor.getInt(mPosMulti),
				cursor.getLong(mPosTimestamp),
				cursor.getInt(mPosFlaunt) != 0,
				cursor.getInt(mPosType),
				cursor.getInt(mPosHidden) != 0);

		return msg;
	}

	public void setNick(String nick) {
		mFormatter.setNick(nick);
	}

}
