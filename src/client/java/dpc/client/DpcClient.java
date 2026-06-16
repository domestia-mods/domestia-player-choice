package dpc.client;

import dpc.DpcMod;
import dpc.DpcScreenOpener;
import dpc.client.gui.PlayerChoiceScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class DpcClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DpcScreenOpener.setOpener(() -> Minecraft.getInstance().setScreen(new PlayerChoiceScreen()));
		DpcMod.LOGGER.info("Domestia Player Choice client initialized.");
	}
}
