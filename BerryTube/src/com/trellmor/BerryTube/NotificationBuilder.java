package com.trellmor.BerryTube;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationBuilder {
	private int mIcon;

	private String mTickerText;
	private Class<?> mCls;

	public NotificationBuilder() {
		this(0, "", null);
	}

	public NotificationBuilder(int icon, String tickerText) {
		this(icon, tickerText, null);
	}

	public NotificationBuilder(int icon, String tickerText, Class<?> cls) {
		mIcon = icon;
		mTickerText = tickerText;
		mCls = cls;
	}

	public int getIcon() {
		return mIcon;
	}

	public void setIcon(int icon) {
		this.mIcon = icon;
	}

	public String getTickerText() {
		return mTickerText;
	}

	public void setTickerText(String tickerText) {
		this.mTickerText = tickerText;
	}

	public Class<?> getCls() {
		return mCls;
	}

	public void setCls(Class<?> cls) {
		this.mCls = cls;
	}

	@SuppressWarnings("deprecation")
	Notification build(Context context) {
		Notification note = new Notification();
		note.icon = mIcon;
		Intent i = new Intent(context, mCls);
		
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
		
		note.setLatestEventInfo(context, mTickerText, mTickerText, pi);
		note.flags |= Notification.FLAG_NO_CLEAR;
		note.flags |= Notification.FLAG_ONGOING_EVENT;
		
		return note;
	}
}
