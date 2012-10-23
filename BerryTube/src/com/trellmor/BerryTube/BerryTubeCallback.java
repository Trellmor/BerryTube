package com.trellmor.BerryTube;

public interface BerryTubeCallback {
	void onSetNick(String nick);
	
	void onChatMessage(ChatMessage chatMsg);
	
	void onUserJoin(ChatUser user);
	
	void onUserPart(ChatUser user);

	void onUserReset();

	void onDrinkCount(int count);
}
