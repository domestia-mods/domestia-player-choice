package domestia_player_choice;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
	public static final Item PLAYER_CHOICE_DIGEST = register(
			"player_choice_digest",
			PlayerChoiceDigestItem::new,
			new Item.Properties().stacksTo(1)
	);

	public static final Item PLAYER_CHOICE_BOARD = register(
			"player_choice_board",
			PlayerChoiceBoardItem::new,
			new Item.Properties().stacksTo(1)
	);

	private static Item register(
			String name,
			Function<Item.Properties, Item> itemFactory,
			Item.Properties settings
	) {
		ResourceKey<Item> itemKey = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(DomestiaPlayerChoice.MOD_ID, name)
		);

		Item item = itemFactory.apply(settings.setId(itemKey));

		return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(PLAYER_CHOICE_DIGEST);
			output.accept(PLAYER_CHOICE_BOARD);
		});

		DomestiaPlayerChoice.LOGGER.info("Registering Domestia Player Choice items.");
	}
}
