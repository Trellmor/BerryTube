package com.trellmor.BerryTube;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Poll {
	final ArrayList<String> mOptions = new ArrayList<String>();

	public ArrayList<String> getOptions() {
		return mOptions;
	}

	final ArrayList<Integer> mVotes = new ArrayList<Integer>();

	public ArrayList<Integer> getVotes() {
		return mVotes;
	}

	private String mTitle;

	public String getTitle() {
		return mTitle;
	}

	private String mCreator;

	public String getCreator() {
		return mCreator;
	}

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

	public void update(final int[] votes) {
		mVotes.clear();

		for (int i : votes) {
			mVotes.add(i);
		}
	}
}
