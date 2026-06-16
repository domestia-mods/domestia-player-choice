package dpc.client.gui;

import dpc.client.catalog.PlayerChoiceMenuNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ScrollableTextMenu {
	private static final int ROW_HEIGHT = 18;
	private static final int ROW_PADDING_X = 6;
	private static final int TEXT_OFFSET_Y = 5;
	private static final int SCROLL_STEP_ROWS = 3;
	private static final int COLOR_TEXT = 0xFF303030;
	private static final int COLOR_ROW_HOVER = 0xFFE6E0D0;
	private static final int COLOR_ROW_NORMAL = 0x00FFFFFF;
	private static final int COLOR_SCROLL_TRACK = 0x55303030;
	private static final int COLOR_SCROLL_THUMB = 0xAA303030;
	private static final int COLOR_BADGE_BACKGROUND = 0xFF303030;
	private static final int COLOR_BADGE_TEXT = 0xFFFFFFFF;
	private static final int BADGE_TAIL_WIDTH = 6;

	private int scrollOffset;

	public void reset() {
		this.scrollOffset = 0;
	}

	public int getHoveredIndex(double mouseX, double mouseY, int x, int y, int width, int height, List<PlayerChoiceMenuNode> nodes) {
		if (!isInside(mouseX, mouseY, x, y, width, height)) {
			return -1;
		}

		int visibleIndex = ((int) mouseY - y) / ROW_HEIGHT;
		int actualIndex = this.scrollOffset + visibleIndex;

		if (actualIndex < 0 || actualIndex >= nodes.size()) {
			return -1;
		}

		return actualIndex;
	}

	public boolean scroll(double mouseX, double mouseY, double scrollY, int x, int y, int width, int height, int itemCount) {
		if (!isInside(mouseX, mouseY, x, y, width, height)) {
			return false;
		}

		int maxOffset = getMaxOffset(height, itemCount);

		if (maxOffset <= 0) {
			this.scrollOffset = 0;
			return false;
		}

		int direction = scrollY > 0.0D ? -1 : 1;
		this.scrollOffset = clamp(this.scrollOffset + direction * SCROLL_STEP_ROWS, 0, maxOffset);
		return true;
	}

	public void render(
			GuiGraphicsExtractor graphics,
			Font font,
			int mouseX,
			int mouseY,
			int x,
			int y,
			int width,
			int height,
			List<PlayerChoiceMenuNode> nodes
	) {
		int maxOffset = getMaxOffset(height, nodes.size());
		this.scrollOffset = clamp(this.scrollOffset, 0, maxOffset);

		graphics.enableScissor(x, y, x + width, y + height);

		int visibleRows = Math.max(1, height / ROW_HEIGHT);

		for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++) {
			int nodeIndex = this.scrollOffset + visibleIndex;

			if (nodeIndex >= nodes.size()) {
				break;
			}

			PlayerChoiceMenuNode node = nodes.get(nodeIndex);
			int rowY = y + visibleIndex * ROW_HEIGHT;
			boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

			graphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT - 1, hovered ? COLOR_ROW_HOVER : COLOR_ROW_NORMAL);
			renderNodeTitle(graphics, font, node, x + ROW_PADDING_X, rowY + TEXT_OFFSET_Y, width - ROW_PADDING_X * 2);
		}

		graphics.disableScissor();
		this.renderScrollbar(graphics, x, y, width, height, nodes.size());
	}

	public void renderHoveredTooltip(
			GuiGraphicsExtractor graphics,
			Font font,
			int mouseX,
			int mouseY,
			int x,
			int y,
			int width,
			int height,
			List<PlayerChoiceMenuNode> nodes
	) {
		int hoveredIndex = this.getHoveredIndex(mouseX, mouseY, x, y, width, height, nodes);

		if (hoveredIndex < 0) {
			return;
		}

		String fullTitle = nodes.get(hoveredIndex).displayTitle();

		if (font.width(fullTitle) <= width - ROW_PADDING_X * 2) {
			return;
		}

		graphics.setTooltipForNextFrame(font, Component.literal(fullTitle), mouseX, mouseY);
	}

	private void renderNodeTitle(GuiGraphicsExtractor graphics, Font font, PlayerChoiceMenuNode node, int x, int y, int maxWidth) {
		String badge = node.badge();

		if (badge == null || badge.isBlank()) {
			graphics.text(font, trimToWidth(font, node.title(), maxWidth), x, y, COLOR_TEXT, false);
			return;
		}

		String badgeText = " " + badge;
		int badgeWidth = font.width(badgeText);
		int badgeHeight = font.lineHeight;
		int titleGap = font.width(" ");
		int badgeY = y - 1;
		int badgeDrawHeight = badgeHeight + 1;

		graphics.fill(x, badgeY, x + badgeWidth, badgeY + badgeDrawHeight, COLOR_BADGE_BACKGROUND);
		renderBadgeTail(graphics, x + badgeWidth, badgeY, BADGE_TAIL_WIDTH, badgeDrawHeight);
		graphics.text(font, badgeText, x, y, COLOR_BADGE_TEXT, false);

		int badgeTotalWidth = badgeWidth + BADGE_TAIL_WIDTH;
		int titleX = x + badgeTotalWidth + titleGap;
		int titleMaxWidth = Math.max(0, maxWidth - badgeTotalWidth - titleGap);

		if (titleMaxWidth > 0) {
			graphics.text(font, trimToWidth(font, node.title(), titleMaxWidth), titleX, y, COLOR_TEXT, false);
		}
	}

	private void renderBadgeTail(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		for (int column = 0; column < width; column++) {
			int inset = column * height / (width * 2);
			graphics.fill(
					x + column,
					y + inset,
					x + column + 1,
					y + height - inset,
					COLOR_BADGE_BACKGROUND
			);
		}
	}

	private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int itemCount) {
		int visibleRows = Math.max(1, height / ROW_HEIGHT);

		if (itemCount <= visibleRows) {
			return;
		}

		int trackX = x + width - 4;
		int trackHeight = height;
		int thumbHeight = Math.max(12, trackHeight * visibleRows / itemCount);
		int maxOffset = getMaxOffset(height, itemCount);
		int movable = Math.max(1, trackHeight - thumbHeight);
		int thumbY = y + (maxOffset <= 0 ? 0 : movable * this.scrollOffset / maxOffset);

		graphics.fill(trackX, y, trackX + 3, y + trackHeight, COLOR_SCROLL_TRACK);
		graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, COLOR_SCROLL_THUMB);
	}

	private static String trimToWidth(Font font, String value, int maxWidth) {
		if (font.width(value) <= maxWidth) {
			return value;
		}

		String ellipsis = "...";
		int ellipsisWidth = font.width(ellipsis);
		StringBuilder builder = new StringBuilder(value);

		while (!builder.isEmpty() && font.width(builder.toString()) + ellipsisWidth > maxWidth) {
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder + ellipsis;
	}

	private static int getMaxOffset(int height, int itemCount) {
		int visibleRows = Math.max(1, height / ROW_HEIGHT);
		return Math.max(0, itemCount - visibleRows);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
