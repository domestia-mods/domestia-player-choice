package dpc;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class DpcBlocks {
	public static final InfoStandBlock INFO_STAND = register(
			"info_stand",
			properties -> new InfoStandBlock(properties, false),
			BlockBehaviour.Properties.of()
					.strength(5.0f, 6.0f)
					.sound(SoundType.METAL)
					.noOcclusion()
	);

	public static final InfoStandBlock INFO_WALL_STAND = register(
			"info_wall_stand",
			properties -> new InfoStandBlock(properties, true),
			BlockBehaviour.Properties.of()
					.strength(5.0f, 6.0f)
					.sound(SoundType.METAL)
					.noOcclusion()
	);

	private static <T extends Block> T register(
			String name,
			Function<BlockBehaviour.Properties, T> blockFactory,
			BlockBehaviour.Properties settings
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
				Registries.BLOCK,
				Identifier.fromNamespaceAndPath(DpcMod.MOD_ID, name)
		);

		T block = blockFactory.apply(settings.setId(blockKey));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static void initialize() {
		DpcMod.LOGGER.info("Registering Domestia Player Choice blocks.");
	}
}
