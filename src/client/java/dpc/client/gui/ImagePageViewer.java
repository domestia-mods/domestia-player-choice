package dpc.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ImagePageViewer {
	private static final int DEFAULT_PAGE_WIDTH = 1024;
	private static final int DEFAULT_PAGE_HEIGHT = 1536;
	private static final Map<Identifier, PageSize> PAGE_SIZE_CACHE = new HashMap<>();

	private ImagePageViewer() {
	}

	public static PageLayout calculateLayout(
			Identifier pageTexture,
			int areaX,
			int areaY,
			int areaWidth,
			int areaHeight
	) {
		PageSize pageSize = getPageSize(pageTexture);
		double scale = Math.min(
				(double) areaWidth / Math.max(1, pageSize.width()),
				(double) areaHeight / Math.max(1, pageSize.height())
		);

		int drawWidth = Math.max(1, (int) Math.round(pageSize.width() * scale));
		int drawHeight = Math.max(1, (int) Math.round(pageSize.height() * scale));
		int drawX = areaX + (areaWidth - drawWidth) / 2;
		int drawY = areaY + (areaHeight - drawHeight) / 2;
		return new PageLayout(drawX, drawY, drawWidth, drawHeight, pageSize.width(), pageSize.height());
	}

	public static void renderPage(
			GuiGraphicsExtractor graphics,
			Identifier pageTexture,
			int areaX,
			int areaY,
			int areaWidth,
			int areaHeight
	) {
		renderPage(graphics, pageTexture, calculateLayout(pageTexture, areaX, areaY, areaWidth, areaHeight));
	}

	public static void renderPage(
			GuiGraphicsExtractor graphics,
			Identifier pageTexture,
			PageLayout layout
	) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				pageTexture,
				layout.x(),
				layout.y(),
				0.0F,
				0.0F,
				layout.width(),
				layout.height(),
				layout.textureWidth(),
				layout.textureHeight(),
				layout.textureWidth(),
				layout.textureHeight()
		);
	}

	private static PageSize getPageSize(Identifier textureId) {
		PageSize cached = PAGE_SIZE_CACHE.get(textureId);

		if (cached != null) {
			return cached;
		}

		PageSize loaded = loadPageSize(textureId);
		PAGE_SIZE_CACHE.put(textureId, loaded);
		return loaded;
	}

	private static PageSize loadPageSize(Identifier textureId) {
		try {
			Minecraft minecraft = Minecraft.getInstance();

			if (minecraft == null || minecraft.getResourceManager() == null) {
				return new PageSize(DEFAULT_PAGE_WIDTH, DEFAULT_PAGE_HEIGHT);
			}

			Optional<Resource> resource = minecraft.getResourceManager().getResource(textureId);

			if (resource.isEmpty()) {
				return new PageSize(DEFAULT_PAGE_WIDTH, DEFAULT_PAGE_HEIGHT);
			}

			try (InputStream input = resource.get().open(); NativeImage image = NativeImage.read(input)) {
				return new PageSize(image.getWidth(), image.getHeight());
			}
		} catch (Exception ignored) {
			return new PageSize(DEFAULT_PAGE_WIDTH, DEFAULT_PAGE_HEIGHT);
		}
	}

	public record PageLayout(int x, int y, int width, int height, int textureWidth, int textureHeight) {
		public int right() {
			return this.x + this.width;
		}

		public int bottom() {
			return this.y + this.height;
		}

		public boolean contains(double mouseX, double mouseY) {
			return mouseX >= this.x
					&& mouseX < this.right()
					&& mouseY >= this.y
					&& mouseY < this.bottom();
		}
	}

	private record PageSize(int width, int height) {
	}
}
