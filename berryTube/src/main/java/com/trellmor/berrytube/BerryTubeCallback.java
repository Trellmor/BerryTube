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
package com.trellmor.berrytube;

/**
 * This interface allows the service to inform the application about status and
 * data changes. Applications that want to receive those callbacks must provide
 * an implementation of this interface.
 * 
 * To register a callback instance with the <code>BerryTube</code> service, use
 * <code>BerryTube.registerCallback(BerryTubeCallback)</code>
 * 
 * To remove a callback from the service use
 * <code>BerryTube.unregisterCallback(BerryTubeCallback)</code>
 * 
 * @author Daniel Triendl
 * 
 */
public interface BerryTubeCallback {
	/**
	 * Server accepted the nick set
	 * 
	 * @param nick
	 *            Nickname
	 */
	void onSetNick(String nick);
	
	/**
	 * Login failed
	 * 
	 * @param error ErrorMessage
	 */
	void onLoginError(String error);

	/**
	 * The drink count for the current video has been changed
	 * 
	 * @param count
	 *            New drink count
	 */
	void onDrinkCount(int count);

	/**
	 * A new poll was started
	 * 
	 * @param poll Poll object
	 */
	void onNewPoll(Poll poll);

	/**
	 * A poll has been updated
	 * 
	 * This happens when any users casts a vote
	 * 
	 * @param poll Poll object
	 */
	void onUpdatePoll(Poll poll);

	/**
	 * The poll has been closed
	 */
	void onClearPoll();
	
	/**
	 * The currently playing video was updated
	 * 
	 * @param name The name of the video (e.g. Sh-Pony).
	 * @param id The 11-character YouTube ID of the video (e.g. JWa0kkIRumk).
	 */
	void onVideoUpdate(String name, String id, String type);

	/**
	 * The user was kicked from the server
	 */
	void onKicked();

	/**
	 * The connection to the sever was lost
	 */
	void onDisconnect();

	/**
	 * Socket Connection error
	 */
	void onError();
}
