package com.trellmor.berrytubechat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class NotificationHelper extends ContextWrapper {
	private static final String CHANNEL_SERVICE = "berryTubeService";
	private static final String CHANNEL_MESSAGE = "berryTubeChatMessage";
	private NotificationManager mManager;

	public NotificationHelper(Context ctx) {
		super(ctx);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel service = new NotificationChannel(CHANNEL_SERVICE, getString(R.string.noti_channel_service),
					NotificationManager.IMPORTANCE_LOW);
			service.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
			getManager().createNotificationChannel(service);

			NotificationChannel message = new NotificationChannel(CHANNEL_MESSAGE, getString(R.string.noti_channel_message),
					NotificationManager.IMPORTANCE_HIGH);
			message.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			message.setLightColor(Color.MAGENTA);
			message.enableLights(true);
			message.setVibrationPattern(new long[] { 0, 100 });
			message.enableVibration(true);
			getManager().createNotificationChannel(message);

		}
	}

	public NotificationCompat.Builder getServiceNotification() {
		NotificationCompat.Builder noti = new NotificationCompat.Builder(this, CHANNEL_MESSAGE);
		noti.setSmallIcon(R.drawable.ic_stat_notify_berrytube);
		noti.setLargeIcon(BitmapFactory.decodeResource(
				getResources(), R.drawable.ic_launcher));
		noti.setContentTitle(getString(R.string.title_activity_chat));

		return noti;
	}

	public NotificationCompat.Builder getMessageNotification() {
		NotificationCompat.Builder noti = new NotificationCompat.Builder(this, CHANNEL_MESSAGE);
		noti.setSmallIcon(R.drawable.ic_stat_notify_chat);
		noti.setLights(0xFF0000FF, 100, 2000);
		noti.setAutoCancel(true);

		return noti;
	}

	private NotificationManager getManager() {
		if (mManager == null) {
			mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		return  mManager;
	}
}
