package domestia_player_choice.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ScrollableTextBlock {
	private static final int LINE_HEIGHT = 12;
	private static final int SCROLL_STEP_LINES = 3;
	private static final int COLOR_TEXT = 0xFF303030;
	private static final int COLOR_SCROLL_TRACK = 0x55303030;
	private static final int COLOR_SCROLL_THUMB = 0xAA303030;

	private int scrollOffset;

	public void reset() {
		this.scrollOffset = 0;
	}

	public boolean scroll(double mouseX, double mouseY, double scrollY, int x, int y, int width, int height, Font font, List<String> paragraphs) {
		if (!isInside(mouseX, mouseY, x, y, width, height)) {
			return false;
		}

		List<String> wrappedLines = wrapText(font, paragraphs, getTextWidth(width));
		int maxOffset = getMaxOffset(height, wrappedLines.size());

		if (maxOffset <= 0) {
			this.scrollOffset = 0;
			return false;
		}

		int direction = scrollY > 0.0D ? -1 : 1;
		this.scrollOffset = clamp(this.scrollOffset + direction * SCROLL_STEP_LINES, 0, maxOffset);
		return true;
	}

	public void render(
			GuiGraphicsExtractor graphics,
			Font font,
			int x,
			int y,
			int width,
			int height,
			List<String> paragraphs
	) {
		List<String> wrappedLines = wrapText(font, paragraphs, getTextWidth(width));
		int maxOffset = getMaxOffset(height, wrappedLines.size());
		this.scrollOffset = clamp(this.scrollOffset, 0, maxOffset);

		graphics.enableScissor(x, y, x + width, y + height);

		int visibleLines = Math.max(1, height / LINE_HEIGHT);

		for (int visibleIndex = 0; visibleIndex < visibleLines; visibleIndex++) {
			int lineIndex = this.scrollOffset + visibleIndex;

			if (lineIndex >= wrappedLines.size()) {
				break;
			}

			String line = wrappedLines.get(lineIndex);

			if (!line.isEmpty()) {
				graphics.text(font, line, x, y + visibleIndex * LINE_HEIGHT, COLOR_TEXT, false);
			}
		}

		graphics.disableScissor();
		this.renderScrollbar(graphics, x, y, width, height, wrappedLines.size());
	}

	private static int getTextWidth(int width) {
		return Math.max(1, width - 8);
	}

	private static List<String> wrapText(Font font, List<String> paragraphs, int maxWidth) {
		List<String> lines = new ArrayList<>();

		for (String paragraph : paragraphs) {
			if (paragraph == null || paragraph.isEmpty()) {
				lines.add("");
				continue;
			}

			wrapParagraph(font, paragraph, maxWidth, lines);
		}

		return lines;
	}

	private static void wrapParagraph(Font font, String paragraph, int maxWidth, List<String> output) {
		String[] words = paragraph.split(" ");
		StringBuilder line = new StringBuilder();

		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}

			if (line.isEmpty()) {
				appendWord(font, word, maxWidth, output, line);
				continue;
			}

			String candidate = line + " " + word;

			if (font.width(candidate) <= maxWidth) {
				line.append(' ').append(word);
			} else {
				output.add(line.toString());
				line.setLength(0);
				appendWord(font, word, maxWidth, output, line);
			}
		}

		if (!line.isEmpty()) {
			output.add(line.toString());
		}
	}

	private static void appendWord(Font font, String word, int maxWidth, List<String> output, StringBuilder line) {
		if (font.width(word) <= maxWidth) {
			line.append(word);
			return;
		}

		StringBuilder chunk = new StringBuilder();

		for (int offset = 0; offset < word.length(); ) {
			int codePoint = word.codePointAt(offset);
			String character = new String(Character.toChars(codePoint));
			String candidate = chunk + character;

			if (!chunk.isEmpty() && font.width(candidate) > maxWidth) {
				output.add(chunk.toString());
				chunk.setLength(0);
			}

			chunk.append(character);
			offset += Character.charCount(codePoint);
		}

		line.append(chunk);
	}

	private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int lineCount) {
		int visibleLines = Math.max(1, height / LINE_HEIGHT);

		if (lineCount <= visibleLines) {
			return;
		}

		int trackX = x + width - 4;
		int trackHeight = height;
		int thumbHeight = Math.max(12, trackHeight * visibleLines / lineCount);
		int maxOffset = getMaxOffset(height, lineCount);
		int movable = Math.max(1, trackHeight - thumbHeight);
		int thumbY = y + (maxOffset <= 0 ? 0 : movable * this.scrollOffset / maxOffset);

		graphics.fill(trackX, y, trackX + 3, y + trackHeight, COLOR_SCROLL_TRACK);
		graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, COLOR_SCROLL_THUMB);
	}

	private static int getMaxOffset(int height, int lineCount) {
		int visibleLines = Math.max(1, height / LINE_HEIGHT);
		return Math.max(0, lineCount - visibleLines);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
