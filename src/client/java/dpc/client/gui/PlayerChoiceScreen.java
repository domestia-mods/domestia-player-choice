package dpc.client.gui;

import dpc.DpcCalendar;
import dpc.client.catalog.PlayerChoiceCatalog;
import dpc.client.catalog.PlayerChoiceCatalogLoader;
import dpc.client.catalog.PlayerChoiceMenuNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class PlayerChoiceScreen extends Screen {
	private static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(
			"domestia_player_choice",
			"textures/gui/player_choice/menu_background.png"
	);

	private static final int MENU_TEXTURE_WIDTH = 680;
	private static final int MENU_TEXTURE_HEIGHT = 720;
	private static final int MENU_WIDTH = 340;
	private static final int MENU_MAX_HEIGHT = 360;

	private static final int BAR_TEXTURE_X = 134;
	private static final int BAR_TEXTURE_WIDTH = 489;
	private static final int BAR_TEXTURE_HEIGHT = 32;
	private static final int TOP_BAR_TEXTURE_Y = 61;
	private static final int BOTTOM_BAR_TEXTURE_Y = 629;
	private static final int HOME_TEXTURE_SIZE = 32;
	private static final int BAR_TEXT_PADDING_TEXTURE_X = 12;

	private static final int CONTENT_PADDING_TEXTURE_X = 12;
	private static final int CONTENT_PADDING_TOP_TEXTURE_Y = 16;
	private static final int CONTENT_PADDING_BOTTOM_TEXTURE_Y = 0;
	private static final int TITLE_TEXTURE_HEIGHT = 48;
	private static final int TITLE_UNDERLINE_TEXTURE_OFFSET = 30;
	private static final int TITLE_UNDERLINE_TEXTURE_HEIGHT = 2;
	private static final int GALLERY_NAV_TEXTURE_HEIGHT = 28;
	private static final int GALLERY_IMAGE_GAP_TEXTURE = 8;

	private static final int PAGE_MARGIN = 16;
	private static final int PAGE_VERTICAL_MARGIN = 20;

	private static final int COLOR_TITLE = 0xFF303030;
	private static final int COLOR_EMPTY = 0xFF777777;
	private static final int COLOR_PAGE_OVERLAY = 0xD5000000;
	private static final int COLOR_MENU_OVERLAY = 0x80000000;
	private static final int COLOR_BAR_TEXT = 0xFFD4D4D4;
	private static final int COLOR_BAR_TEXT_HOVERED = 0xFFFFFFFF;
	private static final int COLOR_GALLERY_NAV = 0xFF202020;
	private static final int COLOR_GALLERY_NAV_HOVERED = 0xFF000000;

	private final PlayerChoiceCatalog catalog;
	private final ScrollableTextMenu scrollableMenu = new ScrollableTextMenu();
	private final ScrollableTextBlock scrollableTextBlock = new ScrollableTextBlock();
	private final Deque<MenuState> menuHistory = new ArrayDeque<>();

	private PlayerChoiceScreenMode mode = PlayerChoiceScreenMode.MENU;
	private List<PlayerChoiceMenuNode> currentMenu;
	private String currentMenuTitle;
	private List<Identifier> currentPages = List.of();
	private List<String> currentText = List.of();
	private int currentPageIndex;
	private long cachedCalendarMinute = Long.MIN_VALUE;
	private String cachedCalendarText = DpcCalendar.formatDisplay(0L);

	public PlayerChoiceScreen() {
		this(PlayerChoiceCatalogLoader.loadCurrentCatalog());
	}

	private PlayerChoiceScreen(PlayerChoiceCatalog catalog) {
		super(Component.literal(catalog.title()));
		this.catalog = catalog;
		this.currentMenu = catalog.menu();
		this.currentMenuTitle = catalog.title();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
			graphics.fill(0, 0, this.width, this.height, COLOR_PAGE_OVERLAY);
			return;
		}

		graphics.fill(0, 0, this.width, this.height, COLOR_MENU_OVERLAY);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				MENU_BACKGROUND_TEXTURE,
				this.getMenuX(),
				this.getMenuY(),
				0.0F,
				0.0F,
				this.getMenuWidth(),
				this.getMenuHeight(),
				MENU_TEXTURE_WIDTH,
				MENU_TEXTURE_HEIGHT,
				MENU_TEXTURE_WIDTH,
				MENU_TEXTURE_HEIGHT
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
			this.renderFullscreenPageViewer(graphics);
		} else {
			this.renderInterface(graphics, mouseX, mouseY);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (this.mode == PlayerChoiceScreenMode.MENU) {
			this.scrollableMenu.renderHoveredTooltip(
					graphics,
					this.font,
					mouseX,
					mouseY,
					this.getListX(),
					this.getListY(),
					this.getListWidth(),
					this.getListHeight(),
					this.currentMenu
			);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}

		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
			this.closeFullscreenPageViewer();
			return true;
		}

		if (this.isHomeClicked(event.x(), event.y())) {
			this.goHome();
			return true;
		}

		if (this.isBottomLeftActionClicked(event.x(), event.y())) {
			if (this.canGoBack()) {
				this.goBack();
			} else {
				this.onClose();
			}

			return true;
		}

		if (this.mode == PlayerChoiceScreenMode.GALLERY) {
			if (this.isPreviousPageClicked(event.x(), event.y())) {
				this.previousPage();
				return true;
			}

			if (this.isNextPageClicked(event.x(), event.y())) {
				this.nextPage();
				return true;
			}

			if (!this.currentPages.isEmpty()
					&& this.getEmbeddedPageLayout().contains(event.x(), event.y())) {
				this.mode = PlayerChoiceScreenMode.PAGE_VIEWER;
				return true;
			}

			return super.mouseClicked(event, doubleClick);
		}

		if (this.mode == PlayerChoiceScreenMode.TEXT) {
			return super.mouseClicked(event, doubleClick);
		}

		int hoveredIndex = this.scrollableMenu.getHoveredIndex(
				event.x(),
				event.y(),
				this.getListX(),
				this.getListY(),
				this.getListWidth(),
				this.getListHeight(),
				this.currentMenu
		);

		if (hoveredIndex < 0) {
			return super.mouseClicked(event, doubleClick);
		}

		this.openNode(this.currentMenu.get(hoveredIndex));
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.mode == PlayerChoiceScreenMode.MENU && this.scrollableMenu.scroll(
				mouseX,
				mouseY,
				scrollY,
				this.getListX(),
				this.getListY(),
				this.getListWidth(),
				this.getListHeight(),
				this.currentMenu.size()
		)) {
			return true;
		}

		if (this.mode == PlayerChoiceScreenMode.TEXT && this.scrollableTextBlock.scroll(
				mouseX,
				mouseY,
				scrollY,
				this.getListX(),
				this.getListY(),
				this.getListWidth(),
				this.getListHeight(),
				this.font,
				this.currentText
		)) {
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER && event.isEscape()) {
			this.closeFullscreenPageViewer();
			return true;
		}

		if (this.mode == PlayerChoiceScreenMode.GALLERY) {
			if (event.isLeft()) {
				this.previousPage();
				return true;
			}

			if (event.isRight()) {
				this.nextPage();
				return true;
			}
		}

		return super.keyPressed(event);
	}

	private void renderInterface(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		this.renderCalendar(graphics);
		this.renderBottomBarActions(graphics, mouseX, mouseY);
		this.renderMenuTitle(
				graphics,
				this.currentMenuTitle,
				this.getContentX(),
				this.getContentY(),
				this.getContentWidth(),
				COLOR_TITLE
		);

		switch (this.mode) {
			case MENU -> this.renderMenuContent(graphics, mouseX, mouseY);
			case TEXT -> this.renderTextContent(graphics);
			case GALLERY -> this.renderGalleryContent(graphics, mouseX, mouseY);
			case PAGE_VIEWER -> {
			}
		}
	}

	private void renderCalendar(GuiGraphicsExtractor graphics) {
		String calendarText = this.getCalendarText();
		int right = this.getTopBarRight() - this.scaleTextureXSize(BAR_TEXT_PADDING_TEXTURE_X);
		int y = this.getTopBarY() + Math.max(0, (this.getTopBarHeight() - this.font.lineHeight) / 2);
		graphics.text(this.font, calendarText, right - this.font.width(calendarText), y, COLOR_BAR_TEXT, false);
	}

	private void renderBottomBarActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		boolean hovered = this.isBottomLeftActionClicked(mouseX, mouseY);
		int color = hovered ? COLOR_BAR_TEXT_HOVERED : COLOR_BAR_TEXT;
		int triangleWidth = 5;
		int triangleHeight = 9;
		int triangleX = this.getBottomBarX() + this.scaleTextureXSize(BAR_TEXT_PADDING_TEXTURE_X);
		int centerY = this.getBottomBarY() + this.getBottomBarHeight() / 2 - 1;

		this.renderLeftTriangle(graphics, triangleX, centerY, triangleWidth, triangleHeight, color);

		int textX = triangleX + triangleWidth + this.scaleTextureXSize(8);
		int textY = this.getBottomBarY()
				+ Math.max(0, (this.getBottomBarHeight() - this.font.lineHeight) / 2)
				+ 1;
		String actionText = this.canGoBack() ? "Back" : "Exit";
		graphics.text(this.font, actionText, textX, textY, color, false);
	}

	private void renderMenuContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.currentMenu.isEmpty()) {
			graphics.centeredText(
					this.font,
					Component.literal("No entries available."),
					this.getContentX() + this.getContentWidth() / 2,
					this.getListY() + 20,
					COLOR_EMPTY
			);
			return;
		}

		this.scrollableMenu.render(
				graphics,
				this.font,
				mouseX,
				mouseY,
				this.getListX(),
				this.getListY(),
				this.getListWidth(),
				this.getListHeight(),
				this.currentMenu
		);
	}

	private void renderTextContent(GuiGraphicsExtractor graphics) {
		this.scrollableTextBlock.render(
				graphics,
				this.font,
				this.getListX(),
				this.getListY(),
				this.getListWidth(),
				this.getListHeight(),
				this.currentText
		);
	}

	private void renderGalleryContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.currentPages.isEmpty()) {
			graphics.centeredText(
					this.font,
					Component.literal("No gallery images available."),
					this.getContentX() + this.getContentWidth() / 2,
					this.getListY() + 20,
					COLOR_EMPTY
			);
			return;
		}

		Identifier currentPage = this.currentPages.get(this.currentPageIndex);
		ImagePageViewer.renderPage(graphics, currentPage, this.getEmbeddedPageLayout());
		this.renderGalleryNavigation(graphics, mouseX, mouseY);
	}

	private void renderGalleryNavigation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int textY = this.getGalleryNavigationY()
				+ Math.max(0, (this.getGalleryNavigationHeight() - this.font.lineHeight) / 2);

		if (this.currentPageIndex > 0) {
			int color = this.isPreviousPageClicked(mouseX, mouseY)
					? COLOR_GALLERY_NAV_HOVERED
					: COLOR_GALLERY_NAV;
			graphics.text(this.font, "< Prev", this.getContentX(), textY, color, false);
		}

		String pageNumber = (this.currentPageIndex + 1) + "/" + this.currentPages.size();
		int pageNumberX = this.getContentX()
				+ (this.getContentWidth() - this.font.width(pageNumber)) / 2;
		graphics.text(this.font, pageNumber, pageNumberX, textY, COLOR_GALLERY_NAV, false);

		if (this.currentPageIndex < this.currentPages.size() - 1) {
			String nextText = "Next >";
			int color = this.isNextPageClicked(mouseX, mouseY)
					? COLOR_GALLERY_NAV_HOVERED
					: COLOR_GALLERY_NAV;
			graphics.text(
					this.font,
					nextText,
					this.getContentRight() - this.font.width(nextText),
					textY,
					color,
					false
			);
		}
	}

	private void renderFullscreenPageViewer(GuiGraphicsExtractor graphics) {
		if (this.currentPages.isEmpty()) {
			graphics.centeredText(
					this.font,
					Component.literal("No gallery images available."),
					this.width / 2,
					this.height / 2,
					0xFFFFFFFF
			);
			return;
		}

		Identifier currentPage = this.currentPages.get(this.currentPageIndex);
		ImagePageViewer.renderPage(graphics, currentPage, this.getFullscreenPageLayout());
	}

	private void openNode(PlayerChoiceMenuNode node) {
		if (node.isSeparator()) {
			return;
		}

		if (node.hasChildren()) {
			this.menuHistory.push(new MenuState(this.currentMenuTitle, this.currentMenu));
			this.currentMenuTitle = node.title();
			this.currentMenu = node.children();
			this.scrollableMenu.reset();
			return;
		}

		if (node.hasGallery()) {
			this.menuHistory.push(new MenuState(this.currentMenuTitle, this.currentMenu));
			this.currentMenuTitle = node.title();
			this.currentPages = node.gallery();
			this.currentPageIndex = 0;
			this.mode = PlayerChoiceScreenMode.GALLERY;
			return;
		}

		if (node.hasHref()) {
			this.openHref(node.href());
			return;
		}

		if (node.hasText()) {
			this.menuHistory.push(new MenuState(this.currentMenuTitle, this.currentMenu));
			this.currentMenuTitle = node.title();
			this.currentText = node.text();
			this.mode = PlayerChoiceScreenMode.TEXT;
			this.scrollableTextBlock.reset();
		}
	}

	private void openHref(String href) {
		URI uri;

		try {
			uri = URI.create(href);
		} catch (IllegalArgumentException exception) {
			return;
		}

		this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
			if (confirmed) {
				this.openExternalUri(uri);
			}

			this.minecraft.setScreen(this);
		}, href, false));
	}

	private void openExternalUri(URI uri) {
		String url = uri.toString();
		String osName = System.getProperty("os.name", "").toLowerCase();

		try {
			if (osName.contains("win")) {
				new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
				return;
			}

			if (osName.contains("mac")) {
				new ProcessBuilder("open", url).start();
				return;
			}

			new ProcessBuilder("xdg-open", url).start();
		} catch (Exception ignored) {
		}
	}

	private void goBack() {
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
			this.closeFullscreenPageViewer();
			return;
		}

		if (this.menuHistory.isEmpty()) {
			return;
		}

		this.currentPages = List.of();
		this.currentText = List.of();
		this.currentPageIndex = 0;
		this.mode = PlayerChoiceScreenMode.MENU;

		MenuState state = this.menuHistory.pop();
		this.currentMenuTitle = state.title();
		this.currentMenu = state.nodes();
		this.scrollableMenu.reset();
		this.scrollableTextBlock.reset();
	}

	private void goHome() {
		this.mode = PlayerChoiceScreenMode.MENU;
		this.currentMenu = this.catalog.menu();
		this.currentMenuTitle = this.catalog.title();
		this.currentPages = List.of();
		this.currentText = List.of();
		this.currentPageIndex = 0;
		this.menuHistory.clear();
		this.scrollableMenu.reset();
		this.scrollableTextBlock.reset();
	}

	private void closeFullscreenPageViewer() {
		this.mode = PlayerChoiceScreenMode.GALLERY;
	}

	private void previousPage() {
		if (this.currentPages.isEmpty()) {
			return;
		}

		this.currentPageIndex = Math.max(0, this.currentPageIndex - 1);
	}

	private void nextPage() {
		if (this.currentPages.isEmpty()) {
			return;
		}

		this.currentPageIndex = Math.min(this.currentPages.size() - 1, this.currentPageIndex + 1);
	}

	private void renderMenuTitle(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int color) {
		String visibleText = this.trimToWidth(text, Math.max(1, width));
		int titleX = x + Math.max(0, (width - this.font.width(visibleText)) / 2);
		graphics.text(this.font, visibleText, titleX, y, color, false);

		int lineY = y + this.getTitleUnderlineOffset();
		graphics.fill(x, lineY, x + width, lineY + this.getTitleUnderlineHeight(), color);
	}

	private void renderLeftTriangle(
			GuiGraphicsExtractor graphics,
			int x,
			int centerY,
			int width,
			int height,
			int color
	) {
		int halfHeight = height / 2;

		for (int row = 0; row < height; row++) {
			int distanceFromCenter = Math.abs(row - halfHeight);
			int rowWidth = Math.max(1, width - distanceFromCenter);
			int rowX = x + width - rowWidth;
			int rowY = centerY - halfHeight + row;
			graphics.fill(rowX, rowY, x + width, rowY + 1, color);
		}
	}

	private String trimToWidth(String value, int maxWidth) {
		if (this.font.width(value) <= maxWidth) {
			return value;
		}

		String ellipsis = "...";
		int ellipsisWidth = this.font.width(ellipsis);
		StringBuilder builder = new StringBuilder(value);

		while (!builder.isEmpty() && this.font.width(builder.toString()) + ellipsisWidth > maxWidth) {
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder + ellipsis;
	}

	private String getCalendarText() {
		long gameTime = this.minecraft != null && this.minecraft.level != null
				? this.minecraft.level.getGameTime()
				: 0L;
		long calendarMinute = Math.max(0L, gameTime) / DpcCalendar.TICKS_PER_MINUTE;

		if (calendarMinute != this.cachedCalendarMinute) {
			this.cachedCalendarMinute = calendarMinute;
			this.cachedCalendarText = DpcCalendar.formatDisplay(gameTime);
		}

		return this.cachedCalendarText;
	}

	private boolean canGoBack() {
		return !this.menuHistory.isEmpty();
	}

	private boolean isHomeClicked(double mouseX, double mouseY) {
		return isInside(
				mouseX,
				mouseY,
				this.getHomeX(),
				this.getBottomBarY(),
				this.getHomeWidth(),
				this.getBottomBarHeight()
		);
	}

	private boolean isBottomLeftActionClicked(double mouseX, double mouseY) {
		return isInside(
				mouseX,
				mouseY,
				this.getBottomBarX(),
				this.getBottomBarY(),
				Math.max(1, this.getHomeX() - this.getBottomBarX()),
				this.getBottomBarHeight()
		);
	}

	private boolean isPreviousPageClicked(double mouseX, double mouseY) {
		if (this.mode != PlayerChoiceScreenMode.GALLERY || this.currentPageIndex <= 0) {
			return false;
		}

		return isInside(
				mouseX,
				mouseY,
				this.getContentX(),
				this.getGalleryNavigationY(),
				Math.max(1, this.getContentWidth() / 3),
				this.getGalleryNavigationHeight()
		);
	}

	private boolean isNextPageClicked(double mouseX, double mouseY) {
		if (this.mode != PlayerChoiceScreenMode.GALLERY
				|| this.currentPageIndex >= this.currentPages.size() - 1) {
			return false;
		}

		int third = Math.max(1, this.getContentWidth() / 3);
		return isInside(
				mouseX,
				mouseY,
				this.getContentRight() - third,
				this.getGalleryNavigationY(),
				third,
				this.getGalleryNavigationHeight()
		);
	}

	private ImagePageViewer.PageLayout getEmbeddedPageLayout() {
		Identifier pageTexture = this.getCurrentPageTexture();
		return ImagePageViewer.calculateLayout(
				pageTexture,
				this.getContentX(),
				this.getListY(),
				this.getContentWidth(),
				this.getGalleryImageAreaHeight()
		);
	}

	private ImagePageViewer.PageLayout getFullscreenPageLayout() {
		return ImagePageViewer.calculateLayout(
				this.getCurrentPageTexture(),
				PAGE_MARGIN,
				PAGE_VERTICAL_MARGIN,
				Math.max(1, this.width - PAGE_MARGIN * 2),
				Math.max(1, this.height - PAGE_VERTICAL_MARGIN * 2)
		);
	}

	private Identifier getCurrentPageTexture() {
		if (this.currentPages.isEmpty()) {
			return Identifier.fromNamespaceAndPath(
					"domestia_player_choice",
					"textures/gui/player_choice/fallback/page_001.png"
			);
		}

		return this.currentPages.get(this.currentPageIndex);
	}

	private int getMenuWidth() {
		return Math.min(MENU_WIDTH, Math.max(160, this.width - 40));
	}

	private int getMenuHeight() {
		return Math.min(MENU_MAX_HEIGHT, Math.max(140, this.height - 40));
	}

	private int getMenuX() {
		return (this.width - this.getMenuWidth()) / 2;
	}

	private int getMenuY() {
		return (this.height - this.getMenuHeight()) / 2;
	}

	private int getTopBarX() {
		return this.textureToScreenX(BAR_TEXTURE_X);
	}

	private int getTopBarRight() {
		return this.textureToScreenX(BAR_TEXTURE_X + BAR_TEXTURE_WIDTH);
	}

	private int getTopBarY() {
		return this.textureToScreenY(TOP_BAR_TEXTURE_Y);
	}

	private int getTopBarHeight() {
		return Math.max(1, this.textureToScreenY(TOP_BAR_TEXTURE_Y + BAR_TEXTURE_HEIGHT) - this.getTopBarY());
	}

	private int getBottomBarX() {
		return this.textureToScreenX(BAR_TEXTURE_X);
	}

	private int getBottomBarY() {
		return this.textureToScreenY(BOTTOM_BAR_TEXTURE_Y);
	}

	private int getBottomBarHeight() {
		return Math.max(1, this.textureToScreenY(BOTTOM_BAR_TEXTURE_Y + BAR_TEXTURE_HEIGHT) - this.getBottomBarY());
	}

	private int getHomeX() {
		int homeTextureX = BAR_TEXTURE_X + (BAR_TEXTURE_WIDTH - HOME_TEXTURE_SIZE) / 2;
		return this.textureToScreenX(homeTextureX);
	}

	private int getHomeWidth() {
		int homeTextureX = BAR_TEXTURE_X + (BAR_TEXTURE_WIDTH - HOME_TEXTURE_SIZE) / 2;
		return Math.max(1, this.textureToScreenX(homeTextureX + HOME_TEXTURE_SIZE) - this.getHomeX());
	}

	private int getContentX() {
		return this.textureToScreenX(BAR_TEXTURE_X + CONTENT_PADDING_TEXTURE_X);
	}

	private int getContentRight() {
		return this.textureToScreenX(BAR_TEXTURE_X + BAR_TEXTURE_WIDTH - CONTENT_PADDING_TEXTURE_X);
	}

	private int getContentY() {
		return this.textureToScreenY(TOP_BAR_TEXTURE_Y + BAR_TEXTURE_HEIGHT + CONTENT_PADDING_TOP_TEXTURE_Y);
	}

	private int getContentBottom() {
		return this.textureToScreenY(BOTTOM_BAR_TEXTURE_Y - CONTENT_PADDING_BOTTOM_TEXTURE_Y);
	}

	private int getContentWidth() {
		return Math.max(1, this.getContentRight() - this.getContentX());
	}

	private int getTitleHeight() {
		return Math.max(this.font.lineHeight + 4, this.scaleTextureYSize(TITLE_TEXTURE_HEIGHT));
	}

	private int getTitleUnderlineOffset() {
		return Math.min(
				this.getTitleHeight() - this.getTitleUnderlineHeight(),
				Math.max(this.font.lineHeight + 2, this.scaleTextureYSize(TITLE_UNDERLINE_TEXTURE_OFFSET))
		);
	}

	private int getTitleUnderlineHeight() {
		return Math.max(1, this.scaleTextureYSize(TITLE_UNDERLINE_TEXTURE_HEIGHT));
	}

	private int getListX() {
		return this.getContentX();
	}

	private int getListY() {
		return this.getContentY() + this.getTitleHeight();
	}

	private int getListWidth() {
		return this.getContentWidth();
	}

	private int getListHeight() {
		return Math.max(1, this.getContentBottom() - this.getListY());
	}

	private int getGalleryNavigationHeight() {
		return Math.max(this.font.lineHeight + 2, this.scaleTextureYSize(GALLERY_NAV_TEXTURE_HEIGHT));
	}

	private int getGalleryNavigationY() {
		return this.getContentBottom() - this.getGalleryNavigationHeight();
	}

	private int getGalleryImageAreaHeight() {
		int gap = this.scaleTextureYSize(GALLERY_IMAGE_GAP_TEXTURE);
		return Math.max(1, this.getGalleryNavigationY() - gap - this.getListY());
	}

	private int textureToScreenX(int textureX) {
		return this.getMenuX() + Math.round((float) textureX * this.getMenuWidth() / MENU_TEXTURE_WIDTH);
	}

	private int textureToScreenY(int textureY) {
		return this.getMenuY() + Math.round((float) textureY * this.getMenuHeight() / MENU_TEXTURE_HEIGHT);
	}

	private int scaleTextureXSize(int textureWidth) {
		return Math.max(1, Math.round((float) textureWidth * this.getMenuWidth() / MENU_TEXTURE_WIDTH));
	}

	private int scaleTextureYSize(int textureHeight) {
		return Math.max(1, Math.round((float) textureHeight * this.getMenuHeight() / MENU_TEXTURE_HEIGHT));
	}

	private static boolean isInside(
			double mouseX,
			double mouseY,
			int x,
			int y,
			int width,
			int height
	) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private record MenuState(String title, List<PlayerChoiceMenuNode> nodes) {
	}
}
