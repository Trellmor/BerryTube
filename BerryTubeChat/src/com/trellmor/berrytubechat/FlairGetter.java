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

import java.util.HashMap;

import com.trellmor.berrytubechat.R;
import com.trellmor.berrymotes.EmoteGetter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;

/**
 * Image getter for user flair images
 * 
 * @author Toastdeib
 * @see Android.Text.Html.ImageGetter
 */
public class FlairGetter extends EmoteGetter implements ImageGetter {

	private Resources mResources;
	private HashMap<String, Drawable> mDrawables;

	public FlairGetter(Context context) {
		super(context);
		this.mResources = context.getResources();

		mDrawables = new HashMap<String, Drawable>();
	}

	@Override
	public Drawable getDrawable(String source) {
		Drawable d = null;

		if (mDrawables.containsKey(source)) {
			d = mDrawables.get(source);
		} else {
			if (source.equals("1"))
				d = mResources.getDrawable(R.drawable.wine);
			else if (source.equals("2"))
				d = mResources.getDrawable(R.drawable.cocktail);
			else if (source.equals("3"))
				d = mResources.getDrawable(R.drawable.cider);
			else if (source.equals("4"))
				d = mResources.getDrawable(R.drawable.liquor1);
			else if (source.equals("5"))
				d = mResources.getDrawable(R.drawable.liquor2);
			else if (source.equals("6"))
				d = mResources.getDrawable(R.drawable.beer);

			if (d != null)
				mDrawables.put(source, d);
		}

		if (d != null) {
			d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		} else {
			d = super.getDrawable(source);
		}

		return d;
	}

}
