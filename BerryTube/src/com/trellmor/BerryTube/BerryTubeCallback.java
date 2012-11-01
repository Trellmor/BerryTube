package com.trellmor.BerryTube;

public interface BerryTubeCallback {
	void onSetNick(String nick);
	
	void onChatMessage(ChatMessage chatMsg);

	void onDrinkCount(int count);
	
	void onNewPoll(Poll poll);
	
	void onUpatePoll(Poll poll);
	
	void onClearPoll();

	void onKicked();
	
	void onDisconnect();
}
