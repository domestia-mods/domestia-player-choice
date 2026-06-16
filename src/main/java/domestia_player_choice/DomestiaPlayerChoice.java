package domestia_player_choice;

import net.fabricmc.api.ModInitializer;
import domestia_player_choice.server.PlayerChoiceServerContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomestiaPlayerChoice implements ModInitializer {
	public static final String MOD_ID = "domestia_player_choice";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();
		PlayerChoiceServerContent.initialize();

		LOGGER.info("Domestia Player Choice initialized.");
	}
}
