/*
 * BerryTube Service
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
package com.trellmor.BerryTube;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class encapsulates a poll
 * 
 * @author Daniel Triendl
 */
public class Poll {
	final ArrayList<String> mOptions = new ArrayList<String>();
	final ArrayList<Integer> mVotes = new ArrayList<Integer>();
	private String mTitle;
	private String mCreator;

	/**
	 * Constructs a <code>Poll</code> from a <code>JSONObject<code>
	 * 
	 * @param poll <code>JSONObject<code> containing the poll data
	 * @throws JSONException
	 */
	public Poll(JSONObject poll) throws JSONException {
		mTitle = poll.getString("title");
		mCreator = poll.getString("title");

		JSONArray options = poll.getJSONArray("options");
		for (int i = 0; i < options.length(); i++) {
			mOptions.add(options.getString(i));
		}

		JSONArray votes = poll.getJSONArray("votes");
		for (int i = 0; i < votes.length(); i++) {
			mVotes.add(votes.getInt(i));
		}
	}

	/**
	 * Constructs a <code>Poll</code>
	 * 
	 * @param title Title of this poll 
	 * @param options Array containing the options
	 * @param votes Array containing the vote count
	 * @param creator Poll creators nick
	 */
	public Poll(String title, final String[] options, final int[] votes,
			String creator) {
		mTitle = title;
		mCreator = creator;

		for (String option : options) {
			mOptions.add(option);
		}

		for (int i : votes) {
			mVotes.add(i);
		}
	}

	/**
	 * Get the poll options
	 * 
	 * @return List of options
	 */
	public ArrayList<String> getOptions() {
		return mOptions;
	}

	/**
	 * Get the vote count
	 * 
	 * @return List of vote counts
	 */
	public ArrayList<Integer> getVotes() {
		return mVotes;
	}

	/**
	 * Get the poll title
	 * 
	 * @return
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * Get the poll creators nick
	 * 
	 * @return Username
	 */
	public String getCreator() {
		return mCreator;
	}

	/**
	 * Update the vote count
	 * 
	 * @param votes Array of vote counts
	 */
	public void update(final int[] votes) {
		mVotes.clear();

		for (int i : votes) {
			mVotes.add(i);
		}
	}
}
