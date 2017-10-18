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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

class BerryTubeUtils {
	private BerryTubeUtils() {
	}
	
	static void openDonatePage(Context context) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://berrytube.tv/donate.php"));
		context.startActivity(browserIntent);
	}
	
	static void openAboutDialog(Context context) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.dialog_about, null);
		
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		alertDialog.setTitle(R.string.title_dialog_about);
		alertDialog.setView(view);
		alertDialog.setPositiveButton(android.R.string.ok, null);
		alertDialog.create().show();
	}
}
