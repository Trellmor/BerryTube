/*
 * BerryTubeChat android client
 * Copyright (C) 2015 Daniel Triendl <trellmor@trellmor.com>
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

import android.app.LoaderManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.CursorAdapter;

import com.trellmor.berrytube.ChatMessageProvider;

class ChatMessageLoaderCallbacks extends ContextWrapper implements LoaderManager.LoaderCallbacks<Cursor> {
	private final CursorAdapter mAdapter;

	static final String KEY_NOTIFICATIONS = "Notifications";

	private static final String[] PROJECTION = new String[] {
			ChatMessageProvider.MessageColumns._ID,
			ChatMessageProvider.MessageColumns.COLUMN_NICK,
			ChatMessageProvider.MessageColumns.COLUMN_MESSAGE,
			ChatMessageProvider.MessageColumns.COLUMN_EMOTE,
			ChatMessageProvider.MessageColumns.COLUMN_FLAIR,
			ChatMessageProvider.MessageColumns.COLUMN_MULTI,
			ChatMessageProvider.MessageColumns.COLUMN_TIMESTAMP,
			ChatMessageProvider.MessageColumns.COLUMN_FLAUNT,
			ChatMessageProvider.MessageColumns.COLUMN_TYPE,
			ChatMessageProvider.MessageColumns.COLUMN_HIDDEN
	};

	ChatMessageLoaderCallbacks(Context ctx, CursorAdapter adapter) {
		super(ctx);
		mAdapter = adapter;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = " ASC";

		if (args != null && args.getBoolean(KEY_NOTIFICATIONS, false)) {
			selection = ChatMessageProvider.MessageColumns.COLUMN_NOTIFICATION + " = ?";
			selectionArgs = new String[] { String.valueOf(1) };
			sortOrder = " DESC";
		}

		return new CursorLoader(
				this,
				ChatMessageProvider.CONTENT_URI_MESSAGES,
				PROJECTION,
				selection,
				selectionArgs,
				ChatMessageProvider.MessageColumns._ID + sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.changeCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}
}
