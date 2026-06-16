package dpc.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dpc.DpcMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class PlayerChoiceServerContent {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_DIRECTORY_NAME = "domestia_player_choice";
	private static final String RESOURCEPACK_DIRECTORY_NAME = "resourcepack";
	private static final String CONFIG_FILE_NAME = "server_config.json";
	private static final String CONTENT_PACK_FILE_NAME = "domestia-player-choice-content.zip";
	private static final String CONTENT_PACK_ROUTE = "/" + CONTENT_PACK_FILE_NAME;
	private static final UUID CONTENT_PACK_ID = UUID.nameUUIDFromBytes(
			(DpcMod.MOD_ID + ":server_content_pack").getBytes(StandardCharsets.UTF_8)
	);

	private static ServerContentState state = ServerContentState.disabled();
	private static HttpServer httpServer;

	private PlayerChoiceServerContent() {
	}

	public static void initialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(PlayerChoiceServerContent::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopHttpServer());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendContentPack(handler));
	}

	private static void onServerStarted(MinecraftServer server) {
		if (!server.isDedicatedServer()) {
			state = ServerContentState.disabled();
			return;
		}

		Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIRECTORY_NAME);
		Path resourcePackDirectory = configDirectory.resolve(RESOURCEPACK_DIRECTORY_NAME);
		Path configPath = configDirectory.resolve(CONFIG_FILE_NAME);
		Path packPath = resourcePackDirectory.resolve(CONTENT_PACK_FILE_NAME);

		try {
			Files.createDirectories(resourcePackDirectory);
		} catch (IOException exception) {
			DpcMod.LOGGER.error("Failed to create Domestia Player Choice config directories.", exception);
			state = ServerContentState.disabled();
			return;
		}

		ServerContentConfig config = loadOrCreateConfig(configPath);

		if (!config.enabled) {
			DpcMod.LOGGER.info("Domestia Player Choice server content pack host is disabled.");
			state = ServerContentState.disabled();
			return;
		}

		if (!Files.isRegularFile(packPath)) {
			DpcMod.LOGGER.warn("Domestia Player Choice content pack not found: {}", packPath);
			state = ServerContentState.disabled();
			return;
		}

		try {
			String sha1 = calculateSha1(packPath);
			long size = Files.size(packPath);
			String publicHost = resolvePublicHost(config.publicHost, server);
			int port = Math.max(1, config.httpPort);
			String url = "http://" + publicHost + ":" + port + CONTENT_PACK_ROUTE;

			startHttpServer(config.httpHost, port, packPath, sha1);

			state = new ServerContentState(true, config.required, url, sha1, config.prompt, size);
			DpcMod.LOGGER.info("Domestia Player Choice content pack is available at {}", url);
			DpcMod.LOGGER.info("Domestia Player Choice content pack SHA-1: {}", sha1);
		} catch (Exception exception) {
			DpcMod.LOGGER.error("Failed to start Domestia Player Choice content pack host.", exception);
			state = ServerContentState.disabled();
			stopHttpServer();
		}
	}

	private static ServerContentConfig loadOrCreateConfig(Path configPath) {
		if (Files.isRegularFile(configPath)) {
			try (Reader reader = new InputStreamReader(Files.newInputStream(configPath), StandardCharsets.UTF_8)) {
				ServerContentConfig config = GSON.fromJson(reader, ServerContentConfig.class);
				return config != null ? config.normalized() : ServerContentConfig.defaults();
			} catch (IOException | JsonSyntaxException exception) {
				DpcMod.LOGGER.warn("Failed to read Domestia Player Choice server config. Using defaults.", exception);
			}
		}

		ServerContentConfig defaults = ServerContentConfig.defaults();

		try {
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, GSON.toJson(defaults), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			DpcMod.LOGGER.warn("Failed to write default Domestia Player Choice server config.", exception);
		}

		return defaults;
	}

	private static void startHttpServer(String configuredHost, int port, Path packPath, String sha1) throws IOException {
		stopHttpServer();

		String host = configuredHost == null || configuredHost.isBlank() ? "0.0.0.0" : configuredHost.trim();
		HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
		server.createContext(CONTENT_PACK_ROUTE, exchange -> serveResourcePack(exchange, packPath, sha1));
		server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "Domestia Player Choice Resource Pack Host");
			thread.setDaemon(true);
			return thread;
		}));
		server.start();
		httpServer = server;
	}

	private static void stopHttpServer() {
		if (httpServer != null) {
			httpServer.stop(0);
			httpServer = null;
		}
	}

	private static void serveResourcePack(HttpExchange exchange, Path packPath, String sha1) throws IOException {
		try (exchange) {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}

			if (!CONTENT_PACK_ROUTE.equals(exchange.getRequestURI().getPath())) {
				exchange.sendResponseHeaders(404, -1);
				return;
			}

			long size = Files.size(packPath);
			Headers requestHeaders = exchange.getRequestHeaders();
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/zip");
			responseHeaders.set("Content-Disposition", "attachment; filename=\"" + CONTENT_PACK_FILE_NAME + "\"");
			responseHeaders.set("ETag", "\"" + sha1 + "\"");
			responseHeaders.set("Cache-Control", "public, max-age=31536000, immutable");

			String ifNoneMatch = requestHeaders.getFirst("If-None-Match");
			if (ifNoneMatch != null && ifNoneMatch.replace("\"", "").equalsIgnoreCase(sha1)) {
				exchange.sendResponseHeaders(304, -1);
				return;
			}

			exchange.sendResponseHeaders(200, size);

			if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
				return;
			}

			try (OutputStream outputStream = exchange.getResponseBody()) {
				Files.copy(packPath, outputStream);
			}
		}
	}

	private static String calculateSha1(Path path) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] buffer = new byte[8192];

		try (var inputStream = Files.newInputStream(path)) {
			int read;
			while ((read = inputStream.read(buffer)) >= 0) {
				digest.update(buffer, 0, read);
			}
		}

		return HexFormat.of().formatHex(digest.digest());
	}

	private static String resolvePublicHost(String configuredPublicHost, MinecraftServer server) {
		if (configuredPublicHost != null && !configuredPublicHost.isBlank()) {
			return configuredPublicHost.trim();
		}

		String serverIp = readServerProperty("server-ip");
		if (serverIp != null && !serverIp.isBlank()) {
			return serverIp.trim();
		}

		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception exception) {
			DpcMod.LOGGER.warn("Failed to auto-detect server host for Domestia Player Choice content pack URL. Falling back to localhost.", exception);
			return "127.0.0.1";
		}
	}

	private static String readServerProperty(String name) {
		Path serverPropertiesPath = FabricLoader.getInstance().getGameDir().resolve("server.properties");

		if (!Files.isRegularFile(serverPropertiesPath)) {
			return null;
		}

		Properties properties = new Properties();

		try (Reader reader = new InputStreamReader(Files.newInputStream(serverPropertiesPath), StandardCharsets.UTF_8)) {
			properties.load(reader);
			return properties.getProperty(name);
		} catch (IOException exception) {
			DpcMod.LOGGER.warn("Failed to read server.properties for Domestia Player Choice.", exception);
			return null;
		}
	}

	private static void sendContentPack(ServerGamePacketListenerImpl handler) {
		ServerContentState currentState = state;

		if (!currentState.enabled) {
			return;
		}

		ClientboundResourcePackPushPacket packet = new ClientboundResourcePackPushPacket(
				CONTENT_PACK_ID,
				currentState.url,
				currentState.sha1,
				currentState.required,
				Optional.of(Component.literal(currentState.prompt))
		);

		handler.send(packet);
	}

	private static final class ServerContentConfig {
		boolean enabled = true;
		boolean required = true;
		String httpHost = "0.0.0.0";
		int httpPort = 8123;
		String publicHost = "";
		String prompt = "This server uses Domestia Player Choice content.";

		static ServerContentConfig defaults() {
			return new ServerContentConfig();
		}

		ServerContentConfig normalized() {
			if (this.httpHost == null || this.httpHost.isBlank()) {
				this.httpHost = "0.0.0.0";
			}
			if (this.httpPort <= 0) {
				this.httpPort = 8123;
			}
			if (this.publicHost == null) {
				this.publicHost = "";
			}
			if (this.prompt == null || this.prompt.isBlank()) {
				this.prompt = "This server uses Domestia Player Choice content.";
			}
			return this;
		}
	}

	private record ServerContentState(
			boolean enabled,
			boolean required,
			String url,
			String sha1,
			String prompt,
			long size
	) {
		static ServerContentState disabled() {
			return new ServerContentState(false, false, "", "", "", 0L);
		}
	}
}
