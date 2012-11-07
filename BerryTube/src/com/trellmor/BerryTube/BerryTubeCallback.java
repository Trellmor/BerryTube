package com.trellmor.BerryTube;

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
	 * A new chat message was received
	 * 
	 * @param chatMsg
	 *            The chat message
	 */
	void onChatMessage(ChatMessage chatMsg);

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
	 * @param poll
	 */
	void onNewPoll(Poll poll);

	/**
	 * A poll has been updated
	 * 
	 * This happens when any users casts a vote
	 * 
	 * @param poll
	 */
	void onUpatePoll(Poll poll);

	/**
	 * The poll has been closed
	 */
	void onClearPoll();

	/**
	 * The user was kicked from the server
	 */
	void onKicked();

	/**
	 * The connection to the sever was lost
	 */
	void onDisconnect();
}
