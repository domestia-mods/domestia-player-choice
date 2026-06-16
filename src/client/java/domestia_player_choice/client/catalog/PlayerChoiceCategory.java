package domestia_player_choice.client.catalog;

import java.util.List;

public record PlayerChoiceCategory(
		String id,
		String title,
		List<PlayerChoiceIssue> issues
) {
}
