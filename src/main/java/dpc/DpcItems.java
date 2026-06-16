package dpc;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class DpcItems {
	public static final Item INFO_PAD = register(
			"info_pad",
			InfoPadItem::new,
			new Item.Properties().stacksTo(1)
	);

	public static final Item INFO_STAND = register(
			"info_stand",
			InfoStandItem::new,
			new Item.Properties().stacksTo(1)
	);

	public static final Item INFO_FRAME = register(
			"info_frame",
			InfoFrameItem::new,
			new Item.Properties().stacksTo(1)
	);

	private static Item register(
			String name,
			Function<Item.Properties, Item> itemFactory,
			Item.Properties settings
	) {
		ResourceKey<Item> itemKey = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(DpcMod.MOD_ID, name)
		);

		Item item = itemFactory.apply(settings.setId(itemKey));

		return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(INFO_PAD);
			output.accept(INFO_FRAME);
			output.accept(INFO_STAND);
		});

		DpcMod.LOGGER.info("Registering Domestia Player Choice items.");
	}
}
