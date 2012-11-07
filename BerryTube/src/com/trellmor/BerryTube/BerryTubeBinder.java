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
