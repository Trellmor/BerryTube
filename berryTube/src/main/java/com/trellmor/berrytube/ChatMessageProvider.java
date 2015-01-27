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

package com.trellmor.berrytube;


import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class ChatMessageProvider extends ContentProvider {
	private MessageDatabase mDatabase;

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd.berrytube.chatmessages";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "vnd.berrytube.chatmessage";
	public static final String CONTENT_AUTHORITY = "com.trellmor.berrytube";
	public static final String PATH_MESSAGES = "messages";
	private static final Uri CONTENT_URI_BASE = Uri.parse("content://" + CONTENT_AUTHORITY);
	public static final Uri CONTENT_URI_MESSAGES = CONTENT_URI_BASE.buildUpon().appendPath(PATH_MESSAGES).build();

	private static final int ROUTE_MESSAGES = 1;
	private static final int ROUTE_MESSAGE = 2;

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_MESSAGES, ROUTE_MESSAGES);
		sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_MESSAGES + "/*", ROUTE_MESSAGE);
	}

	@Override
	public boolean onCreate() {
		mDatabase = new MessageDatabase(getContext());

		return true;
	}

	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case ROUTE_MESSAGES:
				return CONTENT_TYPE;
			case ROUTE_MESSAGE:
				return CONTENT_ITEM_TYPE;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteDatabase db = mDatabase.getReadableDatabase();
		final int match = sUriMatcher.match(uri);
		Cursor c;
		switch (match) {
			case ROUTE_MESSAGES:
				c = db.query(MessageColumns.TABLE_MESSAGES, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case ROUTE_MESSAGE:
				String id = uri.getLastPathSegment();
				c = db.query(MessageColumns.TABLE_MESSAGES, projection, MessageColumns._ID + " =?", new String[] {id}, null, null, sortOrder);
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mDatabase.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case ROUTE_MESSAGES:
				long id = db.insertOrThrow(MessageColumns.TABLE_MESSAGES, null, values);
				Context context = getContext();
				context.getContentResolver().notifyChange(uri, null);
				return Uri.parse(CONTENT_URI_MESSAGES + "/" + id);
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = mDatabase.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		int rowsAffected;
		switch (match) {
			case ROUTE_MESSAGE:
				String id = uri.getLastPathSegment();
				selection = MessageColumns._ID + " =?";
				selectionArgs = new String[] {id};
				rowsAffected = db.delete(MessageColumns.TABLE_MESSAGES, selection, selectionArgs);
				break;
			case ROUTE_MESSAGES:
				rowsAffected = db.delete(MessageColumns.TABLE_MESSAGES, selection, selectionArgs);
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return rowsAffected;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = mDatabase.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		int rowsAffected;
		switch (match) {
			case ROUTE_MESSAGE:
				String id = uri.getLastPathSegment();
				selection = MessageColumns._ID + " =?";
				selectionArgs = new String[] {id};
				rowsAffected = db.update(MessageColumns.TABLE_MESSAGES, values, selection, selectionArgs);
				break;
			case ROUTE_MESSAGES:
				rowsAffected = db.update(MessageColumns.TABLE_MESSAGES, values, selection, selectionArgs);
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return rowsAffected;
	}

	static class MessageDatabase extends SQLiteOpenHelper {
		public static final int DATABASE_VERSION = 4;

		private static final String DATABASE_NAME = "messages.db";
		private static final String IDX_MESSAGES_TIMESTAMP = "idx_" +
				MessageColumns.TABLE_MESSAGES + "_" +
				MessageColumns.COLUMN_TIMESTAMP;

		private static final String SQL_CREATE_MESSAGES = "CREATE TABLE " +
				MessageColumns.TABLE_MESSAGES + " (" +
				MessageColumns._ID + " INTEGER PRIMARY KEY," +
				MessageColumns.COLUMN_NICK + " TEXT," +
				MessageColumns.COLUMN_MESSAGE + " TEXT," +
				MessageColumns.COLUMN_EMOTE + " INTEGER," +
				MessageColumns.COLUMN_FLAIR + " INTEGER," +
				MessageColumns.COLUMN_MULTI + " INTEGER," +
				MessageColumns.COLUMN_TIMESTAMP + " INTEGER," +
				MessageColumns.COLUMN_FLAUNT + " INTEGER," +
				MessageColumns.COLUMN_TYPE + " INTEGER," +
				MessageColumns.COLUMN_NOTIFICATION + " INTEGER," +
				MessageColumns.COLUMN_HIDDEN + " INTEGER)";
		private static final String SQL_DROP_MESSAGES = "DROP TABLE IF EXISTS " + MessageColumns.TABLE_MESSAGES;

		private static final String SQL_CREATE_IDX_ENTRIES_TIMESTAMP = "CREATE INDEX " +
				IDX_MESSAGES_TIMESTAMP +
				" ON " +
				MessageColumns.TABLE_MESSAGES +
				"(" + MessageColumns.COLUMN_TIMESTAMP + " DESC)";
		private static final String SQL_DROP_IDX_ENTRIES_TIMESTAMP = "DROP INDEX IF EXISTS " + IDX_MESSAGES_TIMESTAMP;

		public MessageDatabase(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_MESSAGES);
			db.execSQL(SQL_CREATE_IDX_ENTRIES_TIMESTAMP);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(SQL_DROP_IDX_ENTRIES_TIMESTAMP);
			db.execSQL(SQL_DROP_MESSAGES);
			onCreate(db);
		}
	}

	public static final class MessageColumns implements BaseColumns {
		public static final String TABLE_MESSAGES = "messages";

		public static final String COLUMN_NICK = "nick";
		public static final String COLUMN_MESSAGE = "message";
		public static final String COLUMN_EMOTE = "emote";
		public static final String COLUMN_FLAIR = "flair";
		public static final String COLUMN_MULTI = "multi";
		public static final String COLUMN_TIMESTAMP = "timestamp";
		public static final String COLUMN_FLAUNT = "flaunt";
		public static final String COLUMN_TYPE = "type";
		public static final String COLUMN_NOTIFICATION = "notification";
		public static final String COLUMN_HIDDEN = "hidden";
	}
}
