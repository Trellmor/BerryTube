package com.trellmor.berrytubechat;

import java.util.HashMap;

import com.trellmor.berrytubechat.R;

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
public class FlairGetter implements ImageGetter {

	private Resources mResources;
	private HashMap<String, Drawable> mDrawables;

	public FlairGetter(Resources r) {
		this.mResources = r;

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

		if (d != null)
			d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());

		return d;
	}

}
