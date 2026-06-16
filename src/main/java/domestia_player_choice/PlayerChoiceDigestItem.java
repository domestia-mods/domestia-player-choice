package domestia_player_choice;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class PlayerChoiceDigestItem extends Item {
	public PlayerChoiceDigestItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			PlayerChoiceScreenOpener.open();
		}

		return InteractionResult.SUCCESS;
	}
}
