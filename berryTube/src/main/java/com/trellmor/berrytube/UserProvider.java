/*
 * BerryTubeChat android client
 * Copyright (C) 2016 Daniel Triendl <trellmor@trellmor.com>
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class UserProvider extends ContentProvider {
	private UserDatabase mDatabase;

	public static final String CONTENT_TYPE_IGNOREDUSERS = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd.berrytube.ignoredusers";
	public static final String CONTENT_TYPE_IGNOREDUSER = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd.berrytube.ignoreduser";
	public static final String CONTENT_AUTHORITY = "com.trellmor.berrytube.user";
	public static final String PATH_IGNOREDUSER = "ignoreduser";
	private static final Uri  CONTENT_URI_BASE = Uri.parse("content://" + CONTENT_AUTHORITY);
	public static final Uri CONTENT_URI_IGNOREDUSER = CONTENT_URI_BASE.buildUpon().appendPath(PATH_IGNOREDUSER).build();

	private static final int ROUTE_IGNOREDUSERS = 1;
	private static final int ROUTE_IGNOREDUSER = 2;

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_IGNOREDUSER, ROUTE_IGNOREDUSERS);
		sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_IGNOREDUSER + "/*", ROUTE_IGNOREDUSER);
	}

	@Override
	public boolean onCreate() {
		mDatabase = new UserDatabase(getContext());

		return true;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case ROUTE_IGNOREDUSERS:
				return CONTENT_TYPE_IGNOREDUSERS;
			case ROUTE_IGNOREDUSER:
				return CONTENT_TYPE_IGNOREDUSER;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteDatabase db = mDatabase.getReadableDatabase();
		switch (sUriMatcher.match(uri)) {
			case ROUTE_IGNOREDUSER:
				selection = IgnoredUserColumns._ID + " =?";
				selectionArgs = new String[] {uri.getLastPathSegment()};
			case ROUTE_IGNOREDUSERS:
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		Cursor c = db.query(IgnoredUserColumns.TABLE_IGNOREDUSERS, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
		final SQLiteDatabase db = mDatabase.getWritableDatabase();
		switch (sUriMatcher.match(uri)) {
			case ROUTE_IGNOREDUSERS:
				long id = db.insertWithOnConflict(IgnoredUserColumns.TABLE_IGNOREDUSERS, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
				getContext().getContentResolver().notifyChange(uri, null);
				return Uri.parse(CONTENT_URI_IGNOREDUSER + "/" + id);
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = mDatabase.getWritableDatabase();
		switch (sUriMatcher.match(uri)) {
			case ROUTE_IGNOREDUSER:
				selection = IgnoredUserColumns._ID + " =?";
				selectionArgs = new String[] {uri.getLastPathSegment()};
			case ROUTE_IGNOREDUSERS:
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		int rowsAffected = db.delete(IgnoredUserColumns.TABLE_IGNOREDUSERS, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsAffected;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues contentValues, String s, String[] strings) {
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}

	private static class UserDatabase extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 1;

		private static final String DATABASE_NAME = "users.db";

		private static final String SQL_CREATE_IGNOREDUSERS = "CREATE TABLE "
				+ IgnoredUserColumns.TABLE_IGNOREDUSERS + " ("
				+ IgnoredUserColumns._ID + " INTEGER PRIMARY KEY,"
				+ IgnoredUserColumns.COLUMN_NAME + " TEXT UNIQUE)";

		UserDatabase(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_IGNOREDUSERS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
	}

	public static final class IgnoredUserColumns implements BaseColumns {
		static final String TABLE_IGNOREDUSERS = "ignored_users";

		public static final String COLUMN_NAME = "name";
	}
}
