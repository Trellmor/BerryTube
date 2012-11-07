package com.trellmor.BerryTube;

import java.lang.ref.WeakReference;

import android.os.Binder;

/**
 * BerryTubeBinder is the <code>IBinder</code> implementations for applications to
 * communicate with the <code>BerryTube</code> service class.
 * 
 * @author Daniel Triendl
 * @see andorid.os.Binder
 * @see android.os.IBinder
 * @see android.app.Serice
 */
public class BerryTubeBinder extends Binder {
	private WeakReference<BerryTube> service;

	/**
	 * Constructs a Binder instance for the <code>BerryTube</code> service
	 * 
	 * @param service
	 *            <code>BerryTube</code> service instance
	 * @see BerryTube
	 */
	public BerryTubeBinder(BerryTube service) {
		this.service = new WeakReference<BerryTube>(service);
	}

	/**
	 * Get the <code>BerryTube</code> service instance
	 * 
	 * @return Service instance
	 */
	public BerryTube getService() {
		return service.get();
	}
}
