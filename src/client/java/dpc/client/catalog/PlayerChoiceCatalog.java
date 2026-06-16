package dpc.client.catalog;

import java.util.List;

public record PlayerChoiceCatalog(
		int schema,
		String title,
		List<PlayerChoiceMenuNode> menu
) {
	public static PlayerChoiceCatalog empty() {
		return new PlayerChoiceCatalog(1, "Domestia Player Choice", List.of());
	}
}
