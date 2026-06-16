package domestia_player_choice.client.catalog;

import java.util.List;

public record PlayerChoiceIssue(
		String id,
		String title,
		String date,
		List<String> pages
) {
}
