package net.localscape.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Standalone entry point for the bundled, pinned RSProx player distribution. */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        boolean check = Arrays.asList(args).contains("--check");
        JFrame progress = null;
        try {
            if (Runtime.version().feature() < 21) {
                throw new IOException("LocalScape needs Java 21 or newer. Install Java 21, then reopen this JAR.");
            }
            if (args.length == 1 && args[0].equals("--proxy")) {
                Class.forName("net.rsprox.gui.ProxyToolGuiKt")
                    .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
                return;
            }
            Path home = Path.of(System.getProperty("user.home"), ".localscape");
            String suppliedUrl = null;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--check": break;
                    case "--server":
                        if (++i == args.length) throw new IOException("--server needs a server URL.");
                        suppliedUrl = args[i];
                        break;
                    case "--data-dir":
                        if (++i == args.length) throw new IOException("--data-dir needs a directory.");
                        home = Path.of(args[i]).toAbsolutePath();
                        break;
                    default: throw new IOException("Usage: java -jar LocalScape.jar [--server URL] [--check]");
                }
            }
            JsonObject bundled;
            try (Reader reader = new InputStreamReader(
                    Launcher.class.getResourceAsStream("/localscape-launcher.json"), StandardCharsets.UTF_8)) {
                bundled = JsonParser.parseReader(reader).getAsJsonObject();
            }
            Files.createDirectories(home);
            Properties settings = new Properties();
            Path settingsFile = home.resolve("launcher.properties");
            if (Files.isRegularFile(settingsFile)) {
                try (Reader reader = Files.newBufferedReader(settingsFile)) {
                    settings.load(reader);
                }
            }
            String url = suppliedUrl != null ? suppliedUrl
                : settings.getProperty("server_url", bundled.get("server_url").getAsString());
            if (url.isBlank()) {
                if (check || GraphicsEnvironment.isHeadless()) {
                    throw new IOException("No server address configured. Pass --server http://YOUR_SERVER_IP:8080");
                }
                AtomicReference<String> answer = new AtomicReference<>();
                SwingUtilities.invokeAndWait(() -> answer.set(JOptionPane.showInputDialog(
                    null, "Enter the server URL supplied by your host:", "LocalScape", JOptionPane.QUESTION_MESSAGE)));
                url = answer.get();
                if (url == null) return;
            }
            url = httpUrl(url.trim());
            if (!check && !GraphicsEnvironment.isHeadless()) {
                AtomicReference<JFrame> window = new AtomicReference<>();
                SwingUtilities.invokeAndWait(() -> {
                    JFrame frame = new JFrame("LocalScape");
                    frame.add(new JLabel("Connecting to your server…", SwingConstants.CENTER));
                    frame.setSize(380, 110);
                    frame.setLocationRelativeTo(null);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setVisible(true);
                    window.set(frame);
                });
                progress = window.get();
            }
            JsonObject target = loadTarget(url, bundled.get("revision").getAsString());
            JsonObject document = new JsonObject();
            JsonArray targets = new JsonArray();
            targets.add(target);
            document.add("config", targets);
            Path targetPath = home.resolve(".rsprox/proxy-targets.yaml");
            Files.createDirectories(targetPath.getParent());
            atomicWrite(targetPath, document.toString());
            settings.setProperty("server_url", url);
            try (Writer writer = Files.newBufferedWriter(settingsFile)) {
                settings.store(writer, "LocalScape player launcher");
            }
            // Resolve the GUI without initializing it: check mode also catches missing bundled classes.
            Class.forName("net.rsprox.gui.ProxyToolGuiKt", false, Launcher.class.getClassLoader());
            if (check) {
                System.out.println("Ready: " + target.get("name").getAsString());
                System.out.println("Player data: " + home);
                return;
            }
            if (GraphicsEnvironment.isHeadless()) {
                throw new IOException("Launch the JAR from a desktop session to open the game.");
            }
            Path jar = Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
            List<String> command = new ArrayList<>(List.of(java.toString(), "-Duser.home=" + home,
                "-jar", jar.toString(), "--proxy"));
            ProcessBuilder builder = new ProcessBuilder(command).directory(home.toFile());
            // The same home must reach RuneLite's child JVMs, for sockets and isolated settings.
            builder.environment().put("JAVA_TOOL_OPTIONS",
                builder.environment().getOrDefault("JAVA_TOOL_OPTIONS", "") + " -Duser.home=\"" + home + "\"");
            builder.environment().put("RUNELITE_ARGS", "--disable-telemetry");
            builder.environment().remove("RSPS_JAVCONFIG_URL");
            builder.environment().remove("RSPS_RSA");
            Path log = home.resolve("launcher.log");
            builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
            Process process = builder.start();
            close(progress);
            progress = null;
            if (process.waitFor() != 0) {
                throw new IOException("The game launcher stopped unexpectedly. Details: " + log);
            }
        } catch (Exception error) {
            close(progress);
            error.printStackTrace();
            if (!check && !GraphicsEnvironment.isHeadless()) {
                String message = error.getMessage();
                if (message == null || message.isBlank()) {
                    message = "Could not start LocalScape. Check the server address and your connection.\n" + error;
                }
                JOptionPane.showMessageDialog(null, message, "LocalScape could not start", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }
    }

    private static void close(JFrame frame) {
        if (frame != null) SwingUtilities.invokeLater(frame::dispose);
    }

    private static String httpUrl(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            if ((!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
            return value.replaceAll("/+$", "");
        } catch (IllegalArgumentException error) {
            throw new IOException("Use an HTTP(S) server URL, such as http://YOUR_SERVER_IP:8080", error);
        }
    }

    private static String fetch(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(httpUrl(url)))
            .timeout(Duration.ofSeconds(30)).header("User-Agent", "LocalScape-player/1").build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException error) {
            throw new IOException("Could not connect to " + url + ". Check that your host's server is running.", error);
        }
        try (InputStream input = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("Server returned HTTP " + response.statusCode() + " for " + url);
            }
            byte[] body = input.readNBytes(1024 * 1024 + 1);
            if (body.length > 1024 * 1024) throw new IOException("Server configuration is too large.");
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private static JsonObject loadTarget(String base, String revision) throws IOException, InterruptedException {
        try {
            JsonArray targets = JsonParser.parseString(fetch(base + "/proxy-targets.json"))
                .getAsJsonObject().getAsJsonArray("config");
            if (targets.size() != 1) throw new IllegalArgumentException();
            JsonObject remote = targets.get(0).getAsJsonObject();
            if (!revision.equals(remote.get("revision").getAsString())) {
                throw new IOException("This launcher needs server revision " + revision + ". Ask your host for the matching launcher.");
            }
            String modulus = remote.get("modulus").getAsString();
            if (!modulus.matches("[0-9a-fA-F]{128,}")) throw new IllegalArgumentException();
            String name = remote.get("name").getAsString();
            if (name.isBlank()) throw new IllegalArgumentException();
            String portText = remote.get("game_server_port").getAsString();
            if (!portText.matches("[0-9]+")) throw new IllegalArgumentException();
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw new IllegalArgumentException();
            JsonObject target = new JsonObject();
            for (String key : List.of("jav_config_url", "runelite_bootstrap_url")) {
                target.addProperty(key, httpUrl(remote.get(key).getAsString()));
            }
            // Check the hosted launch files before replacing a previously working local target.
            fetch(target.get("jav_config_url").getAsString());
            fetch(target.get("runelite_bootstrap_url").getAsString());
            target.addProperty("name", name);
            target.addProperty("revision", revision);
            target.addProperty("modulus", modulus);
            target.addProperty("game_server_port", port);
            target.addProperty("binary_folder", "LocalScape");
            return target;
        } catch (RuntimeException error) {
            throw new IOException("The server did not return a valid LocalScape player configuration.", error);
        }
    }

    private static void atomicWrite(Path destination, String content) throws IOException {
        Path temporary = Files.createTempFile(destination.getParent(), "player-target-", ".tmp");
        try {
            Files.writeString(temporary, content);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
