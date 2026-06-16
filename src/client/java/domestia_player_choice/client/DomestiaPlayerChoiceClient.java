package domestia_player_choice.client;

import domestia_player_choice.DomestiaPlayerChoice;
import domestia_player_choice.PlayerChoiceScreenOpener;
import domestia_player_choice.client.gui.PlayerChoiceScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class DomestiaPlayerChoiceClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PlayerChoiceScreenOpener.setOpener(() -> Minecraft.getInstance().setScreen(new PlayerChoiceScreen()));
		DomestiaPlayerChoice.LOGGER.info("Domestia Player Choice client initialized.");
	}
}
