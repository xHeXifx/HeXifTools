package org.hexif.hexiftools;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class DiscordWebhook {

    private final String webhookUrl;
    private final Logger logger;

    private final JavaPlugin plugin;

    public DiscordWebhook(String webhookUrl, Logger logger, JavaPlugin plugin) {
        this.webhookUrl = webhookUrl;
        this.logger = logger;
        this.plugin = plugin;
    }

    public void sendEmbed(String title, String description, int color) {
        sendEmbedWithContent(title, description, color, null);
    }
    
    public void sendEmbedWithContent(String title, String description, int color, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warning("Cannot send webhook: url is null or empty");
            return;
        }

        try {
            URL url = new URI(webhookUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String timestamp = Instant.now().toString();
            String json;
            
            if (content != null && !content.isEmpty()) {
                json = String.format(
                    "{\"content\":\"%s\",\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d,\"timestamp\":\"%s\"}]}",
                    escapeJson(content),
                    escapeJson(title),
                    escapeJson(description),
                    color,
                    timestamp
                );
            } else {
                json = String.format(
                    "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d,\"timestamp\":\"%s\"}]}",
                    escapeJson(title),
                    escapeJson(description),
                    color,
                    timestamp
                );
            }

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                logger.warning("webhook returned error code: " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            logger.severe("Failed to send webhook: " + e.getMessage());
            logger.severe("Webhook url: " + webhookUrl);
            e.printStackTrace();
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
