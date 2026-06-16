package domestia_player_choice.client.gui;

import domestia_player_choice.client.catalog.PlayerChoiceCatalog;
import domestia_player_choice.client.catalog.PlayerChoiceCatalogLoader;
import domestia_player_choice.client.catalog.PlayerChoiceMenuNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class PlayerChoiceScreen extends Screen {
	private static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("domestia_player_choice", "textures/gui/player_choice/menu_background.png");
	private static final int MENU_TEXTURE_WIDTH = 680;
	private static final int MENU_TEXTURE_HEIGHT = 720;
	private static final int MENU_PRINT_X = 66;
	private static final int MENU_PRINT_Y = 30;
	private static final int MENU_PRINT_WIDTH = 246;
	private static final int MENU_PRINT_HEIGHT = 302;
	private static final int MENU_PRINT_PADDING = 12;
	private static final int COLOR_TITLE = 0xFF303030;
	private static final int COLOR_EMPTY = 0xFF777777;
	private static final int COLOR_PAGE_OVERLAY = 0xD5000000;
	private static final int COLOR_MENU_OVERLAY = 0x80000000;

	private static final int MENU_WIDTH = 340;
	private static final int MENU_MAX_HEIGHT = 360;
	private static final int MENU_TITLE_HEIGHT = 24;
	private static final int MENU_TITLE_UNDERLINE_OFFSET = 15;
	private static final int MENU_BACK_ROW_HEIGHT = 18;
	private static final int MENU_BACK_PADDING_X = 6;
	private static final int MENU_BACK_TEXT_OFFSET_Y = 5;
	private static final int MENU_BACK_TAIL_WIDTH = 6;
	private static final int COLOR_MENU_BACK_BACKGROUND = 0xFF303030;
	private static final int COLOR_MENU_BACK_TEXT = 0xFFFFFFFF;
	private static final int BUTTON_WIDTH = 70;
	private static final int BUTTON_HEIGHT = 15;
	private static final int ARROW_BUTTON_WIDTH = 28;
	private static final int PAGE_MARGIN = 16;
	private static final int PAGE_VERTICAL_MARGIN = 20;

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
	private Button backButton;
	private Button previousPageButton;
	private Button nextPageButton;

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
	protected void init() {
		super.init();
		this.rebuildNavigationWidgets();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
			graphics.fill(0, 0, this.width, this.height, COLOR_PAGE_OVERLAY);
			return;
		}

		graphics.fill(0, 0, this.width, this.height, COLOR_MENU_OVERLAY);

		int menuX = this.getMenuX();
		int menuY = this.getMenuY();
		int menuWidth = this.getMenuWidth();
		int menuHeight = this.getMenuHeight();

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				MENU_BACKGROUND_TEXTURE,
				menuX,
				menuY,
				0.0F,
				0.0F,
				menuWidth,
				menuHeight,
				MENU_TEXTURE_WIDTH,
				MENU_TEXTURE_HEIGHT,
				MENU_TEXTURE_WIDTH,
				MENU_TEXTURE_HEIGHT
		);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
			this.renderPageViewer(graphics);
		} else {
			this.renderMenu(graphics, mouseX, mouseY);
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
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if ((this.mode != PlayerChoiceScreenMode.MENU && this.mode != PlayerChoiceScreenMode.TEXT) || event.button() != 0) {
			return false;
		}

		if (this.isMenuBackEntryClicked(event.x(), event.y())) {
			this.goBack();
			return true;
		}

		if (this.mode == PlayerChoiceScreenMode.TEXT) {
			return false;
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
			return false;
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
		if (this.mode == PlayerChoiceScreenMode.PAGE_VIEWER) {
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

	private void renderMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int contentX = this.getContentX();
		int contentY = this.getContentY();
		int contentWidth = this.getContentWidth();

		if (this.hasMenuBackEntry()) {
			this.renderMenuBackEntry(graphics, mouseX, mouseY);
		}

		this.renderMenuTitle(graphics, this.currentMenuTitle, contentX, this.getTitleY(), contentWidth, COLOR_TITLE);

		if (this.mode == PlayerChoiceScreenMode.TEXT) {
			this.scrollableTextBlock.render(
					graphics,
					this.font,
					this.getListX(),
					this.getListY(),
					this.getListWidth(),
					this.getListHeight(),
					this.currentText
			);
			return;
		}

		if (this.currentMenu.isEmpty()) {
			graphics.centeredText(this.font, Component.literal("No entries available."), contentX + contentWidth / 2, this.getListY() + 20, COLOR_EMPTY);
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

	private void renderPageViewer(GuiGraphicsExtractor graphics) {
		if (this.currentPages.isEmpty()) {
			graphics.centeredText(this.font, Component.literal("No pages available."), this.width / 2, this.height / 2, 0xFFFFFFFF);
			return;
		}

		Identifier currentPage = this.currentPages.get(this.currentPageIndex);
		ImagePageViewer.PageLayout layout = this.getCurrentPageLayout();
		ImagePageViewer.renderPage(
				graphics,
				currentPage,
				this.getPageAreaX(),
				this.getPageAreaY(),
				this.getPageAreaWidth(),
				this.getPageAreaHeight()
		);

		int bottomBandHeight = Math.max(0, this.height - layout.bottom());
		int pageNumberY = layout.bottom() + Math.max(2, (bottomBandHeight - this.font.lineHeight) / 2);
		graphics.centeredText(
				this.font,
				Component.literal((this.currentPageIndex + 1) + " / " + this.currentPages.size()),
				layout.x() + layout.width() / 2,
				pageNumberY,
				0xFFFFFFFF
		);
	}

	private void openNode(PlayerChoiceMenuNode node) {
		if (node.hasChildren()) {
			this.menuHistory.push(new MenuState(this.currentMenuTitle, this.currentMenu));
			this.currentMenuTitle = node.title();
			this.currentMenu = node.children();
			this.scrollableMenu.reset();
			this.rebuildNavigationWidgets();
			return;
		}

		if (node.hasPages()) {
			this.mode = PlayerChoiceScreenMode.PAGE_VIEWER;
			this.currentPages = node.pages();
			this.currentPageIndex = 0;
			this.rebuildNavigationWidgets();
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
			this.rebuildNavigationWidgets();
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
			this.mode = PlayerChoiceScreenMode.MENU;
			this.currentPages = List.of();
			this.currentPageIndex = 0;
			this.rebuildNavigationWidgets();
			return;
		}

		if (this.mode == PlayerChoiceScreenMode.TEXT) {
			this.currentText = List.of();
			this.mode = PlayerChoiceScreenMode.MENU;
		}

		if (!this.menuHistory.isEmpty()) {
			MenuState state = this.menuHistory.pop();
			this.currentMenuTitle = state.title();
			this.currentMenu = state.nodes();
			this.scrollableMenu.reset();
			this.rebuildNavigationWidgets();
		}
	}

	private void previousPage() {
		if (this.currentPages.isEmpty()) {
			return;
		}

		this.currentPageIndex = Math.max(0, this.currentPageIndex - 1);
		this.rebuildNavigationWidgets();
	}

	private void nextPage() {
		if (this.currentPages.isEmpty()) {
			return;
		}

		this.currentPageIndex = Math.min(this.currentPages.size() - 1, this.currentPageIndex + 1);
		this.rebuildNavigationWidgets();
	}

	private void rebuildNavigationWidgets() {
		this.clearWidgets();

		ImagePageViewer.PageLayout pageLayout = this.getCurrentPageLayout();
		int backButtonX = pageLayout.x();
		int backButtonY = Math.max(2, (pageLayout.y() - BUTTON_HEIGHT) / 2);

		this.backButton = this.addRenderableWidget(Button.builder(
				Component.literal("Back"),
				button -> this.goBack()
		).bounds(
				backButtonX,
				backButtonY,
				BUTTON_WIDTH,
				BUTTON_HEIGHT
		).build());

		this.backButton.visible = this.mode == PlayerChoiceScreenMode.PAGE_VIEWER;

		int bottomButtonY = this.mode == PlayerChoiceScreenMode.PAGE_VIEWER
				? pageLayout.bottom() + Math.max(2, (this.height - pageLayout.bottom() - BUTTON_HEIGHT) / 2)
				: this.height / 2 - BUTTON_HEIGHT / 2;

		this.previousPageButton = this.addRenderableWidget(Button.builder(
				Component.literal("<"),
				button -> this.previousPage()
		).bounds(
				this.mode == PlayerChoiceScreenMode.PAGE_VIEWER ? pageLayout.x() : PAGE_MARGIN,
				bottomButtonY,
				ARROW_BUTTON_WIDTH,
				BUTTON_HEIGHT
		).build());

		this.nextPageButton = this.addRenderableWidget(Button.builder(
				Component.literal(">"),
				button -> this.nextPage()
		).bounds(
				this.mode == PlayerChoiceScreenMode.PAGE_VIEWER ? pageLayout.right() - ARROW_BUTTON_WIDTH : this.width - PAGE_MARGIN - ARROW_BUTTON_WIDTH,
				bottomButtonY,
				ARROW_BUTTON_WIDTH,
				BUTTON_HEIGHT
		).build());

		this.updatePageButtons();
	}

	private void updatePageButtons() {
		boolean pageMode = this.mode == PlayerChoiceScreenMode.PAGE_VIEWER;

		if (this.previousPageButton != null) {
			this.previousPageButton.visible = pageMode;
			this.previousPageButton.active = pageMode && this.currentPageIndex > 0;
		}

		if (this.nextPageButton != null) {
			this.nextPageButton.visible = pageMode;
			this.nextPageButton.active = pageMode && this.currentPageIndex < this.currentPages.size() - 1;
		}

		if (this.backButton != null) {
			this.backButton.visible = pageMode;
		}
	}

	private ImagePageViewer.PageLayout getCurrentPageLayout() {
		Identifier pageTexture = this.currentPages.isEmpty()
				? Identifier.fromNamespaceAndPath("domestia_player_choice", "textures/gui/player_choice/fallback/page_001.png")
				: this.currentPages.get(this.currentPageIndex);

		return ImagePageViewer.calculateLayout(
				pageTexture,
				this.getPageAreaX(),
				this.getPageAreaY(),
				this.getPageAreaWidth(),
				this.getPageAreaHeight()
		);
	}

	private void renderMenuBackEntry(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = this.getMenuBackEntryX();
		int y = this.getMenuBackEntryY();
		int textY = y + MENU_BACK_TEXT_OFFSET_Y;
		String text = " Back";
		int bodyWidth = this.font.width(text);
		int bodyHeight = this.font.lineHeight + 1;
		boolean hovered = this.isMenuBackEntryClicked(mouseX, mouseY);
		int color = hovered ? 0xFF454545 : COLOR_MENU_BACK_BACKGROUND;

		this.renderMenuBackTail(graphics, x, textY - 1, MENU_BACK_TAIL_WIDTH, bodyHeight, color);
		graphics.fill(x + MENU_BACK_TAIL_WIDTH, textY - 1, x + MENU_BACK_TAIL_WIDTH + bodyWidth, textY - 1 + bodyHeight, color);
		graphics.text(this.font, text, x + MENU_BACK_TAIL_WIDTH, textY, COLOR_MENU_BACK_TEXT, false);
	}

	private void renderMenuBackTail(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		for (int column = 0; column < width; column++) {
			int inset = (width - column - 1) * height / (width * 2);
			graphics.fill(x + column, y + inset, x + column + 1, y + height - inset, color);
		}
	}

	private void renderMenuTitle(GuiGraphicsExtractor graphics, String text, int x, int y, int width, int color) {
		String visibleText = this.trimToWidth(text, Math.max(1, width));
		int titleX = x + Math.max(0, (width - this.font.width(visibleText)) / 2);
		graphics.text(this.font, visibleText, titleX, y, color, false);

		int lineY = y + MENU_TITLE_UNDERLINE_OFFSET;
		graphics.fill(x, lineY, x + width, lineY + 1, color);
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

	private boolean hasMenuBackEntry() {
		return (this.mode == PlayerChoiceScreenMode.MENU || this.mode == PlayerChoiceScreenMode.TEXT) && !this.menuHistory.isEmpty();
	}

	private boolean isMenuBackEntryClicked(double mouseX, double mouseY) {
		if (!this.hasMenuBackEntry()) {
			return false;
		}

		int x = this.getMenuBackEntryX();
		int y = this.getMenuBackEntryY();
		int width = MENU_BACK_TAIL_WIDTH + this.font.width(" Back");
		int height = this.font.lineHeight + 1;
		int hitY = y + MENU_BACK_TEXT_OFFSET_Y - 1;
		return mouseX >= x && mouseX < x + width && mouseY >= hitY && mouseY < hitY + height;
	}

	private int getMenuBackEntryX() {
		return this.getContentX() + MENU_BACK_PADDING_X;
	}

	private int getMenuBackEntryY() {
		return this.getContentY();
	}

	private int getTitleY() {
		return this.getContentY() + (this.hasMenuBackEntry() ? MENU_BACK_ROW_HEIGHT : 0) + 4;
	}

	private int getMenuHeaderHeight() {
		return MENU_TITLE_HEIGHT + (this.hasMenuBackEntry() ? MENU_BACK_ROW_HEIGHT : 0);
	}

	private int getPageAreaX() {
		return PAGE_MARGIN;
	}

	private int getPageAreaY() {
		return PAGE_VERTICAL_MARGIN;
	}

	private int getPageAreaWidth() {
		return Math.max(1, this.width - PAGE_MARGIN * 2);
	}

	private int getPageAreaHeight() {
		return Math.max(1, this.height - PAGE_VERTICAL_MARGIN * 2);
	}

	private boolean canGoBack() {
		return this.mode == PlayerChoiceScreenMode.PAGE_VIEWER || this.mode == PlayerChoiceScreenMode.TEXT || !this.menuHistory.isEmpty();
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

	private int getContentX() {
		return this.getMenuX() + this.scaleMenuX(MENU_PRINT_X + MENU_PRINT_PADDING);
	}

	private int getContentY() {
		return this.getMenuY() + this.scaleMenuY(MENU_PRINT_Y + MENU_PRINT_PADDING);
	}

	private int getContentWidth() {
		return Math.max(1, this.scaleMenuX(MENU_PRINT_WIDTH - MENU_PRINT_PADDING * 2));
	}

	private int getContentHeight() {
		return Math.max(1, this.scaleMenuY(MENU_PRINT_HEIGHT - MENU_PRINT_PADDING * 2));
	}

	private int getListX() {
		return this.getContentX();
	}

	private int getListY() {
		return this.getContentY() + this.getMenuHeaderHeight();
	}

	private int getListWidth() {
		return this.getContentWidth();
	}

	private int getListHeight() {
		return Math.max(1, this.getContentHeight() - this.getMenuHeaderHeight());
	}

	private int scaleMenuX(int value) {
		return Math.round((float) value * this.getMenuWidth() / MENU_WIDTH);
	}

	private int scaleMenuY(int value) {
		return Math.round((float) value * this.getMenuHeight() / MENU_MAX_HEIGHT);
	}

	private record MenuState(String title, List<PlayerChoiceMenuNode> nodes) {
	}
}
