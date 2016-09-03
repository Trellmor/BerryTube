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
package com.trellmor.berrytubechat;

import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.trellmor.berrytube.UserProvider;

import java.util.ArrayList;

public class IgnoredUsersActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = IgnoredUsersActivity.class.getName();

	private ListView mListIgnoredUsers;
	private SimpleCursorAdapter mAdatper;

	private static final int LOADER_IGNOREDUSERS = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ignored_users);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mListIgnoredUsers = (ListView) findViewById(R.id.list_ignoredusers);

		mAdatper = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_multiple_choice, null,
				new String[] {UserProvider.IgnoredUserColumns.COLUMN_NAME},
				new int[] {android.R.id.text1}, 0);
		mListIgnoredUsers.setAdapter(mAdatper);
		getLoaderManager().initLoader(LOADER_IGNOREDUSERS, null, this);
	}

	private void deleteSelected() {
		if (mListIgnoredUsers.getCheckedItemCount() == 0)
			return;

		long[] selected = mListIgnoredUsers.getCheckedItemIds();

		ArrayList<ContentProviderOperation> batch = new ArrayList<>();

		for (long id : selected) {
			batch.add(ContentProviderOperation.newDelete(
					UserProvider.CONTENT_URI_IGNOREDUSER.buildUpon().
							appendPath(String.valueOf(id)).build()).build());
		}

		try {
			getContentResolver().applyBatch(UserProvider.CONTENT_AUTHORITY, batch);
		} catch (RemoteException | OperationApplicationException e) {
			Log.e(TAG, "deleteSelected", e);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_delete:
				deleteSelected();
				return true;
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_ignored_users, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(this, UserProvider.CONTENT_URI_IGNOREDUSER, new String[]{
				UserProvider.IgnoredUserColumns._ID,
				UserProvider.IgnoredUserColumns.COLUMN_NAME}, null, null,
				UserProvider.IgnoredUserColumns.COLUMN_NAME + " COLLATE NOCASE ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdatper.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdatper.swapCursor(null);
	}
}
