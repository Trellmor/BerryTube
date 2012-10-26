package com.trellmor.BerryTube;

import java.lang.ref.WeakReference;

import android.os.Binder;

public class BerryTubeBinder extends Binder {
	private WeakReference<BerryTube> service;
	
	public BerryTubeBinder(BerryTube service) {
		this.service = new WeakReference<BerryTube>(service);
	}
	
	public BerryTube getService() {
		return service.get(); 
	}
}
