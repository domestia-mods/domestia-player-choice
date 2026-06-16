package dpc.client.catalog;

import net.minecraft.resources.Identifier;

import java.util.List;

public record PlayerChoiceMenuNode(
		String title,
		String badge,
		List<PlayerChoiceMenuNode> children,
		List<Identifier> pages,
		String href,
		List<String> text
) {
	public boolean hasChildren() {
		return this.children != null && !this.children.isEmpty();
	}

	public boolean hasPages() {
		return this.pages != null && !this.pages.isEmpty();
	}

	public boolean hasHref() {
		return this.href != null && !this.href.isBlank();
	}

	public boolean hasText() {
		return this.text != null && !this.text.isEmpty();
	}

	public String displayTitle() {
		if (this.badge == null || this.badge.isBlank()) {
			return this.title;
		}

		return "[" + this.badge + "] " + this.title;
	}
}
