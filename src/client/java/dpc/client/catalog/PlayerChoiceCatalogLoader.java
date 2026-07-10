package dpc.client.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dpc.DpcMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlayerChoiceCatalogLoader {
	private static final Identifier CATALOG_ID = Identifier.fromNamespaceAndPath(
			DpcMod.MOD_ID,
			"player_choice/catalog.json"
	);

	private PlayerChoiceCatalogLoader() {
	}

	public static PlayerChoiceCatalog loadCurrentCatalog() {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft == null || minecraft.getResourceManager() == null) {
			return PlayerChoiceCatalog.empty();
		}

		return loadCatalog(minecraft.getResourceManager());
	}

	public static PlayerChoiceCatalog loadCatalog(ResourceManager resourceManager) {
		try {
			Optional<Resource> resource = resourceManager.getResource(CATALOG_ID);

			if (resource.isEmpty()) {
				DpcMod.LOGGER.warn("Domestia Player Choice catalog not found: {}", CATALOG_ID);
				return PlayerChoiceCatalog.empty();
			}

			try (Reader reader = resource.get().openAsReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				return parseCatalog(root);
			}
		} catch (Exception exception) {
			DpcMod.LOGGER.error("Failed to load Domestia Player Choice catalog.", exception);
			return PlayerChoiceCatalog.empty();
		}
	}

	private static PlayerChoiceCatalog parseCatalog(JsonObject root) {
		int schema = getInt(root, "schema", 1);

		if (schema != 1) {
			throw new IllegalArgumentException("Unsupported catalog schema: " + schema);
		}

		String title = getRequiredString(root, "title");
		JsonArray menuArray = getRequiredArray(root, "menu");
		List<PlayerChoiceMenuNode> menu = parseMenuNodes(menuArray, "menu");

		return new PlayerChoiceCatalog(schema, title, menu);
	}

	private static List<PlayerChoiceMenuNode> parseMenuNodes(JsonArray array, String path) {
		List<PlayerChoiceMenuNode> nodes = new ArrayList<>();

		for (int index = 0; index < array.size(); index++) {
			JsonElement element = array.get(index);

			if (!element.isJsonObject()) {
				throw new IllegalArgumentException(path + "[" + index + "] must be an object.");
			}

			nodes.add(parseMenuNode(element.getAsJsonObject(), path + "[" + index + "]"));
		}

		return List.copyOf(nodes);
	}

	private static PlayerChoiceMenuNode parseMenuNode(JsonObject object, String path) {
		boolean separator = getOptionalBoolean(object, "separator", false);

		if (separator) {
			if (object.size() != 1) {
				throw new IllegalArgumentException(path + " separator must not contain other fields.");
			}

			return PlayerChoiceMenuNode.createSeparator();
		}

		String title = getRequiredString(object, "title");
		String badge = getOptionalString(object, "badge");
		boolean hasChildren = object.has("children") && !object.get("children").isJsonNull();
		boolean hasGallery = object.has("gallery") && !object.get("gallery").isJsonNull();
		String href = getOptionalString(object, "href");
		boolean hasHref = href != null && !href.isBlank();
		boolean hasText = object.has("text") && !object.get("text").isJsonNull();

		int actionCount = 0;
		if (hasChildren) {
			actionCount++;
		}
		if (hasGallery) {
			actionCount++;
		}
		if (hasHref) {
			actionCount++;
		}
		if (hasText) {
			actionCount++;
		}

		if (actionCount != 1) {
			throw new IllegalArgumentException(path + " must contain exactly one of: children, gallery, href, text.");
		}

		List<PlayerChoiceMenuNode> children = List.of();
		List<Identifier> gallery = List.of();
		List<String> text = List.of();

		if (hasChildren) {
			children = parseMenuNodes(getRequiredArray(object, "children"), path + ".children");
		}

		if (hasGallery) {
			gallery = parseGalleryIdentifiers(getRequiredArray(object, "gallery"), path + ".gallery");
		}

		if (hasText) {
			text = parseTextLines(getRequiredArray(object, "text"), path + ".text");
		}

		return new PlayerChoiceMenuNode(title, badge, children, gallery, href, text, false);
	}

	private static List<String> parseTextLines(JsonArray array, String path) {
		List<String> lines = new ArrayList<>();

		for (int index = 0; index < array.size(); index++) {
			JsonElement element = array.get(index);

			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException(path + "[" + index + "] must be a string.");
			}

			lines.add(element.getAsString());
		}

		if (lines.isEmpty()) {
			throw new IllegalArgumentException(path + " must contain at least one text line.");
		}

		return List.copyOf(lines);
	}

	private static List<Identifier> parseGalleryIdentifiers(JsonArray array, String path) {
		List<Identifier> identifiers = new ArrayList<>();

		for (int index = 0; index < array.size(); index++) {
			JsonElement element = array.get(index);

			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException(path + "[" + index + "] must be a texture identifier string.");
			}

			Identifier identifier = Identifier.tryParse(element.getAsString());

			if (identifier == null) {
				throw new IllegalArgumentException(path + "[" + index + "] is not a valid identifier: " + element.getAsString());
			}

			identifiers.add(identifier);
		}

		if (identifiers.isEmpty()) {
			throw new IllegalArgumentException(path + " must contain at least one gallery image.");
		}

		return List.copyOf(identifiers);
	}

	private static String getRequiredString(JsonObject object, String name) {
		String value = getOptionalString(object, name);

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing required string field: " + name);
		}

		return value;
	}

	private static String getOptionalString(JsonObject object, String name) {
		if (!object.has(name) || object.get(name).isJsonNull()) {
			return null;
		}

		JsonElement element = object.get(name);

		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("Field must be a string: " + name);
		}

		return element.getAsString();
	}

	private static boolean getOptionalBoolean(JsonObject object, String name, boolean fallback) {
		if (!object.has(name) || object.get(name).isJsonNull()) {
			return fallback;
		}

		JsonElement element = object.get(name);

		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException("Field must be a boolean: " + name);
		}

		return element.getAsBoolean();
	}

	private static int getInt(JsonObject object, String name, int fallback) {
		if (!object.has(name) || object.get(name).isJsonNull()) {
			return fallback;
		}

		return object.get(name).getAsInt();
	}

	private static JsonArray getRequiredArray(JsonObject object, String name) {
		if (!object.has(name) || !object.get(name).isJsonArray()) {
			throw new IllegalArgumentException("Missing required array field: " + name);
		}

		return object.getAsJsonArray(name);
	}
}
