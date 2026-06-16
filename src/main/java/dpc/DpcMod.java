package dpc;

import net.fabricmc.api.ModInitializer;
import dpc.server.PlayerChoiceServerContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DpcMod implements ModInitializer {
	public static final String MOD_ID = "domestia_player_choice";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		DpcBlocks.initialize();
		DpcItems.initialize();
		PlayerChoiceServerContent.initialize();

		LOGGER.info("Domestia Player Choice initialized.");
	}
}
