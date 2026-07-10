package dpc.client.catalog;

import net.minecraft.resources.Identifier;

import java.util.List;

public record PlayerChoiceMenuNode(
		String title,
		String badge,
		List<PlayerChoiceMenuNode> children,
		List<Identifier> gallery,
		String href,
		List<String> text,
		boolean separator
) {
	public static PlayerChoiceMenuNode createSeparator() {
		return new PlayerChoiceMenuNode(null, null, List.of(), List.of(), null, List.of(), true);
	}

	public boolean hasChildren() {
		return this.children != null && !this.children.isEmpty();
	}

	public boolean hasGallery() {
		return this.gallery != null && !this.gallery.isEmpty();
	}

	public boolean hasHref() {
		return this.href != null && !this.href.isBlank();
	}

	public boolean hasText() {
		return this.text != null && !this.text.isEmpty();
	}

	public boolean isSeparator() {
		return this.separator;
	}

	public String displayTitle() {
		if (this.separator) {
			return "";
		}

		if (this.badge == null || this.badge.isBlank()) {
			return this.title;
		}

		return "[" + this.badge + "] " + this.title;
	}
}
