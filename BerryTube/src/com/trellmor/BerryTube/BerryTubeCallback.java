package com.trellmor.BerryTube;

public interface BerryTubeCallback {
	void onSetNick(String nick);
	
	void onChatMessage(ChatMessage chatMsg);

	void onDrinkCount(int count);
}
