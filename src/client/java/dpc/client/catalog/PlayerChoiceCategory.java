package dpc.client.catalog;

import java.util.List;

public record PlayerChoiceCategory(
		String id,
		String title,
		List<PlayerChoiceIssue> issues
) {
}
