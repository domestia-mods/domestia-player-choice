package domestia_player_choice;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class ModBlocks {
	public static final PlayerChoiceBoardBlock PLAYER_CHOICE_BOARD = register(
			"player_choice_board",
			properties -> new PlayerChoiceBoardBlock(properties, false),
			BlockBehaviour.Properties.of()
					.strength(1.0f, 1.0f)
					.sound(SoundType.WOOD)
					.noOcclusion()
	);

	public static final PlayerChoiceBoardBlock PLAYER_CHOICE_WALL_BOARD = register(
			"player_choice_wall_board",
			properties -> new PlayerChoiceBoardBlock(properties, true),
			BlockBehaviour.Properties.of()
					.strength(1.0f, 1.0f)
					.sound(SoundType.WOOD)
					.noOcclusion()
	);

	private static <T extends Block> T register(
			String name,
			Function<BlockBehaviour.Properties, T> blockFactory,
			BlockBehaviour.Properties settings
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
				Registries.BLOCK,
				Identifier.fromNamespaceAndPath(DomestiaPlayerChoice.MOD_ID, name)
		);

		T block = blockFactory.apply(settings.setId(blockKey));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static void initialize() {
		DomestiaPlayerChoice.LOGGER.info("Registering Domestia Player Choice blocks.");
	}
}
