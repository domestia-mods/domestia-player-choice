package dpc.client.catalog;

import java.util.List;

public record PlayerChoiceIssue(
		String id,
		String title,
		String date,
		List<String> gallery
) {
}
