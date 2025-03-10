/*
 * Copyright (c) created class file on: 2016.
 * All rights reserved.
 * Copyright owner: brainsynder/Magnus498
 * To contact the developer go to:
 * - spigotmc.org and look up brainsynder
 * - email at: briansnyder498@gmail.com
 * - or Skype at live:starwars4393
 */

package simple.brainsynder.utils;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import simple.brainsynder.exceptions.TamperException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

public class SpigotPluginHandler implements Listener {
    private static final ExecutorService HANDLER_THREAD = new ThreadPoolExecutor(0, 2, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    private Plugin plugin;
    private boolean needsUpdate = false;
    private String version;
    private int id;
    public static Map<Plugin, SpigotPluginHandler> updaterMap = new HashMap<>();
    public static Map<Plugin, String> verionMap = new HashMap<>();
    public static Map<Plugin, Integer> IdMap = new HashMap<>();
    public static Map<Plugin, String> authorMap = new HashMap<>();
    public static Map<Plugin, String> pluginNameMap = new HashMap<>();
    private String downloads;
    private MetricType metricType = MetricType.NONE;

    public SpigotPluginHandler(Plugin plugin) {
        this(plugin, 0, true);
    }

    public SpigotPluginHandler(Plugin plugin, int id, boolean metrics) {
        this.plugin = plugin;
        if (id != 0) {
            this.id = id;
        }
        if (id != 0) {
            System.out.println('[' + plugin.getDescription().getName() + "] Checking for updates...");
            if (plugin.getServer().getPluginManager().getPlugin("PerWorldPlugins") == null) {
                if (metrics)
                    loadMetrics();
                runUpdateCheck();
            } else {
                System.out.println('[' + plugin.getDescription().getName() + "] Could not check for an update due to PerWorldPlugins being installed.");
                System.out.println('[' + plugin.getDescription().getName() + "] PerWorldPlugins could be conflicting with the plugin loading.");
            }
        }
    }

    public SpigotPluginHandler(Plugin plugin, int id) {
        this.plugin = plugin;
        if (id != 0) {
            this.id = id;
        }
        if (id != 0) {
            System.out.println('[' + plugin.getDescription().getName() + "] Checking for updates...");
            if (plugin.getServer().getPluginManager().getPlugin("PerWorldPlugins") == null) {
                loadMetrics();
                runUpdateCheck();
            } else {
                System.out.println('[' + plugin.getDescription().getName() + "] Could not check for an update due to PerWorldPlugins being installed.");
                System.out.println('[' + plugin.getDescription().getName() + "] PerWorldPlugins could be conflicting with the plugin loading.");
            }
        }
    }

    public SpigotPluginHandler(Plugin plugin, int id, MetricType metricType) {
        this.plugin = plugin;
        this.metricType = metricType;
        if (id != 0) {
            this.id = id;
            System.out.println('[' + plugin.getDescription().getName() + "] Checking for updates...");
            if (plugin.getServer().getPluginManager().getPlugin("PerWorldPlugins") == null) {
                loadMetrics();
                runUpdateCheck();
            } else {
                System.out.println('[' + plugin.getDescription().getName() + "] Could not check for an update due to PerWorldPlugins being installed.");
                System.out.println('[' + plugin.getDescription().getName() + "] PerWorldPlugins could be conflicting with the plugin loading.");
            }
        }
    }

    public SpigotPluginHandler(Plugin plugin, MetricType metricType) {
        this.plugin = plugin;
        this.metricType = metricType;
        System.out.println('[' + plugin.getDescription().getName() + "] Sending Statistics...");
        if (plugin.getServer().getPluginManager().getPlugin("PerWorldPlugins") == null) {
            loadMetrics();
        } else {
            System.out.println('[' + plugin.getDescription().getName() + "] Could not send statistics due to PerWorldPlugins being installed.");
            System.out.println('[' + plugin.getDescription().getName() + "] PerWorldPlugins could be conflicting with the plugin loading.");
        }

    }

    public int getId() {
        return id;
    }

    public String getDownloads() {
        return downloads;
    }

    /**
     * This will also add your plugin to mcstats.org It will use your plugins name as the name to add.
     *
     * @param plugin  Your plugins instance
     * @param author  (Can be null now) Author in the plugin.yml (Must have to work with the Tamper checking of the plugin.yml)
     * @param version (Can be null now) Version of the plugin in the plugin.yml (Must have to work with the Tamper checking of the plugin.yml)
     * @param name    (Can be null now) Name of the plugin in the plugin.yml (Must have to work with the Tamper checking of the plugin.yml)
     * @param id      Spigot id (Look at your plugins spigot link the id is like this["spigotmc.org/resources/plugin.ID"]) If you do not know your spigot plugin id then use 0
     */
    public static void registerPlugin(Plugin plugin, String author, String version, String name, int id) {
        SpigotPluginHandler handler = new SpigotPluginHandler(plugin, id, true);
        if (!handler.runTamperCheck(author, name, version)) {
            return;
        }
        updaterMap.put(plugin, handler);
        if (version != null)
            verionMap.put(plugin, version);
        IdMap.put(plugin, id);
        if (author != null)
            authorMap.put(plugin, author);
        if (name != null)
            pluginNameMap.put(plugin, name);
    }

    public static void registerPlugin(SpigotPluginHandler handler) {
        Plugin plugin = handler.plugin;
        updaterMap.put(plugin, handler);
        IdMap.put(plugin, handler.id);
        PluginDescriptionFile pdf = plugin.getDescription();
        if (pdf.getVersion() != null)
            verionMap.put(plugin, pdf.getVersion());
        if (pdf.getAuthors().get(0) != null)
            authorMap.put(plugin, pdf.getAuthors().get(0));
        if (pdf.getName() != null)
            pluginNameMap.put(plugin, pdf.getName());
    }

    public static void registerPlugin(Plugin plugin, int id) {
        SpigotPluginHandler handler = new SpigotPluginHandler(plugin, id);
        updaterMap.put(plugin, handler);
        PluginDescriptionFile pdf = plugin.getDescription();
        if (pdf.getVersion() != null)
            verionMap.put(plugin, pdf.getVersion());
        if (pdf.getAuthors().get(0) != null)
            authorMap.put(plugin, pdf.getAuthors().get(0));
        if (pdf.getName() != null)
            pluginNameMap.put(plugin, pdf.getName());
    }

    /**
     * This will also add your plugin to mcstats.org It will use your plugins name as the name to add.
     *
     * @param plugin  Your plugins instance
     * @param author  (Can be null now) Author in the plugin.yml (Must have to work with the Tamper checking of the plugin.yml)
     * @param version (Can be null now) Version of the plugin in the plugin.yml (Must have to work with the Tamper checking of the plugin.yml)
     * @param name    (Can be null now) Name of the plugin in the plugin.yml (Must have to work with the Tamper checking of the plugin.yml)
     * @param id      Spigot id (Look at your plugins spigot link the id is like this["spigotmc.org/resources/plugin.ID"]) If you do not know your spigot plugin id then use 0
     * @param metrics Do you want to make your plugin use metrics?
     */
    public static void registerPlugin(Plugin plugin, String author, String version, String name, int id, boolean metrics) {
        SpigotPluginHandler handler = new SpigotPluginHandler(plugin, id, metrics);
        if (!handler.runTamperCheck(author, name, version)) {
            return;
        }
        updaterMap.put(plugin, handler);
        if (version != null)
            verionMap.put(plugin, version);
        IdMap.put(plugin, id);
        if (author != null)
            authorMap.put(plugin, author);
        if (name != null)
            pluginNameMap.put(plugin, name);
    }

    public static void registerPlugin(Plugin plugin, String author, String version, String name, int id, MetricType metrics) {
        SpigotPluginHandler handler = new SpigotPluginHandler(plugin, id, metrics);
        if (!handler.runTamperCheck(author, name, version)) {
            return;
        }
        updaterMap.put(plugin, handler);
        if (version != null)
            verionMap.put(plugin, version);
        IdMap.put(plugin, id);
        if (author != null)
            authorMap.put(plugin, author);
        if (name != null)
            pluginNameMap.put(plugin, name);
    }

    public String getVersion() {
        return version;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean runTamperCheck(String author, String name, String version) {
        PluginDescriptionFile pdf = plugin.getDescription();
        try {
            if (author != null)
                if (!pdf.getAuthors().contains(author)) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: Original author not found.");
                }
            if (author != null)
                if (pdf.getAuthors().size() != 1) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: More than 1 author.");
                }
            if (name != null)
                if (!pdf.getName().equals(name)) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: Plugin name does not match.");
                }
            if (version != null)
                if (!pdf.getVersion().equals(version)) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: Versions do not match.");
                }
        } catch (TamperException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean runTamperCheck(List<String> authors, String name, String version) {
        PluginDescriptionFile pdf = plugin.getDescription();
        try {
            if (authors != null) {
                if (!pdf.getAuthors().equals(authors)) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: Original author not found.");
                }
                if (pdf.getAuthors().size() != authors.size()) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: More than " + authors.size() + " author.");
                }
            }
            if (name != null)
                if (!pdf.getName().equals(name)) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: Plugin name does not match.");
                }
            if (version != null)
                if (!pdf.getVersion().equals(version)) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    throw new TamperException("Plugin.yml has been tampered with, Cause: Versions do not match.");
                }
        } catch (TamperException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void loadMetrics() {
        if (metricType == MetricType.NONE)
            return;
        if (metricType == MetricType.BSTATS) {
            new BStats(plugin);
            System.out.println('[' + plugin.getDescription().getName() + "] loading BStats...");
            System.out.println('[' + plugin.getDescription().getName() + "] BStats Location: https://bstats.org/plugin/bukkit/" + plugin.getDescription().getName().replace(" ", "%20"));
        }
    }

    private void runUpdateCheck() {
        PluginDescriptionFile pdf = plugin.getDescription();
        new BukkitRunnable() {
            @Override
            public void run() {
                String downloadURL = "https://www.spigotmc.org/resources/" + id + '/';
                runUpdateCheck();
                needsUpdate((needsUpdate, version) -> {
                    System.out.println("SimpleAPI >> An update was found for " + pdf.getName() + "!");
                    System.out.println("SimpleAPI >>  Current Version: " + pdf.getVersion());
                    System.out.println("SimpleAPI >>  New Version: " + version);
                    System.out.println("SimpleAPI >>  Download at: " + downloadURL);
                });
            }
        }.runTaskLater(plugin, 20 * 60 * 10);
    }

    public void needsUpdate(UpdateValue value) {
        CompletableFuture.runAsync(() -> {
            boolean update = false;
            String version = "";
            PluginUpdater updater = new PluginUpdater(plugin, id, false);
            PluginUpdater.UpdateResult result = updater.getResult();
            switch (result) {
                case UPDATE_AVAILABLE: {
                    if (!needsUpdate) {
                        version = updater.getVersion();
                        update = true;
                    }
                }
            }
            if (!update) return;

            String finalVersion = version;
            boolean finalUpdate = update;
            new BukkitRunnable() {
                @Override
                public void run() {
                    value.run(finalUpdate, finalVersion);
                }
            }.runTask(plugin);
        });
    }

    public interface UpdateValue {
        void run(boolean needsUpdate, String version);
    }

    public boolean needsUpdate() {
        if (id == 0) {
            version = "0.2";
            return true;
        }
        PluginUpdater updater = new PluginUpdater(plugin, id, false);
        PluginUpdater.UpdateResult result = updater.getResult();
        switch (result) {
            case FAIL_SPIGOT: {
                return false;
            }
            case NO_UPDATE: {
                return false;
            }
            case UPDATE_AVAILABLE: {
                if (!needsUpdate) {
                    version = updater.getVersion();
                    downloads = updater.getDownloads();
                    needsUpdate = true;
                }
            }
        }

        return (!version.equals(plugin.getDescription().getVersion()));
    }

    public static String[] versionStats(Plugin plugin) {
        if (!SpigotPluginHandler.updaterMap.containsKey(plugin)) {
            System.out.println("SimpleAPI >> Plugin is not registered with SimpleAPI (SpigotPluginHandler class)");
            return new String[]{"Error"};
        }
        SpigotPluginHandler handle = SpigotPluginHandler.updaterMap.get(plugin);
        PluginUpdater updater = new PluginUpdater(plugin, handle.id, false);
        PluginUpdater.UpdateResult result = updater.getResult();
        String downloadURL = "https://spigotmc.org/resources/" + handle.id + '/';
        if (result == PluginUpdater.UpdateResult.UPDATE_AVAILABLE) {
            return new String[]{
                    "§bAvailable Update: §7true",
                    "§bNew Version: §7" + updater.getVersion(),
                    "§bCurrent Version: §7" + plugin.getDescription().getVersion(),
                    "§bDownload at: §7" + downloadURL
            };
        } else {
            return new String[]{
                    "§bAvailable Update: §7false",
                    "§bCurrent Version: §7" + plugin.getDescription().getVersion(),
                    "§bDownload at: §7" + downloadURL
            };
        }
    }

    public enum MetricType {
        BSTATS,
        NONE
    }

    private static class PluginUpdater {

        private Plugin plugin;
        private String VERSION_URL;
        private String RESOURCE_ID = "";
        private String version;
        private String DOWNLOADS;
        private String oldVersion;
        private UpdateResult result = UpdateResult.DISABLED;

        public enum UpdateResult {
            NO_UPDATE,
            DISABLED,
            FAIL_SPIGOT,
            BAD_RESOURCEID,
            UPDATE_AVAILABLE
        }

        public PluginUpdater(Plugin plugin, int resourceId, boolean disabled) {
            RESOURCE_ID = String.valueOf(resourceId);
            this.plugin = plugin;
            oldVersion = this.plugin.getDescription().getVersion();

            if (disabled) {
                result = UpdateResult.DISABLED;
                return;
            }
            if (resourceId == 0) {
                result = UpdateResult.BAD_RESOURCEID;
                return;
            }
            VERSION_URL = "https://api.spiget.org/v2/resources/" + RESOURCE_ID + "/versions?sort=-name";
            run();
        }

        private void run() {
            try {
                System.setProperty("http.agent", "Chrome");
                URL url = new URL(VERSION_URL);
                URLConnection connection = url.openConnection();
                connection.addRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                InputStream inputStream = connection.getInputStream();
                JSONArray versionsArray = (JSONArray) JSONValue.parseWithException(IOUtils.toString(inputStream));
                String lastVersion = ((JSONObject) versionsArray.get(0)).get("name").toString();
                DOWNLOADS = ((JSONObject) versionsArray.get(0)).get("downloads").toString();
                if (shouldUpdate(oldVersion, lastVersion)) {
                    version = lastVersion;
                    result = UpdateResult.UPDATE_AVAILABLE;
                } else {
                    result = UpdateResult.NO_UPDATE;
                }
            } catch (Exception e) {
                result = UpdateResult.FAIL_SPIGOT;
            }
        }

        private void versionCheck() {
            if (shouldUpdate(oldVersion, version)) {
                result = UpdateResult.UPDATE_AVAILABLE;
            } else {
                result = UpdateResult.NO_UPDATE;
            }
        }

        private int[] format(String version) {
            int[] target = new int[3];
            String prep = version.toLowerCase()
                    .replace("-snapshot", "").replace("-release", "");
            String[] found = prep.replace(".", "-").split("-");
            target[0] = Integer.parseInt(found[0]);
            target[1] = Integer.parseInt(found[1]);
            target[2] = ((found.length == 3) ? Integer.parseInt(found[2]) : 0);
            return target;
        }

        public boolean shouldUpdate(String localVersion, String remoteVersion) {
            try {
                int[] remote = format(remoteVersion);
                int[] local = format(localVersion);
                if ((remote[0] >= local[0])
                        && (remote[1] > local[1])
                        || (remote[2] > local[2])) return true;
            }catch (Exception e) {
                return !localVersion.equalsIgnoreCase(remoteVersion);
            }
            return false;
        }

        public String getDownloads() {
            return DOWNLOADS;
        }

        public UpdateResult getResult() {
            return result;
        }

        public String getVersion() {
            return version;
        }

    }

    private static class BStats {

        // The version of this bStats class
        public static final int B_STATS_VERSION = 1;

        // The url to which the data is sent
        private static final String URL = "https://bStats.org/submitData";

        // Should failed requests be logged?
        private static boolean logFailedRequests;

        // The uuid of the server
        private static String serverUUID;

        // The plugin
        private final Plugin plugin;

        // A list with all custom charts
        private final List<CustomChart> charts = new ArrayList<>();

        /**
         * Class constructor.
         *
         * @param plugin The plugin which stats should be submitted.
         */
        public BStats(Plugin plugin) {
            if (plugin == null) {
                throw new IllegalArgumentException("Plugin cannot be null!");
            }
            this.plugin = plugin;

            // Get the config file
            File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
            File configFile = new File(bStatsFolder, "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Check if the config file exists
            if (!config.isSet("serverUuid")) {

                // Add default values
                config.addDefault("enabled", true);
                // Every server gets it's unique random id.
                config.addDefault("serverUuid", UUID.randomUUID().toString());
                // Should failed request be logged?
                config.addDefault("logFailedRequests", false);

                // Inform the server owners about bStats
                config.options().header(
                        "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                                "To honor their work, you should not disable it.\n" +
                                "This has nearly no effect on the server performance!\n" +
                                "Check out https://bStats.org/ to learn more :)"
                ).copyDefaults(true);
                try {
                    config.save(configFile);
                } catch (IOException e) {
                }
            }

            // Load the data
            serverUUID = config.getString("serverUuid");
            logFailedRequests = config.getBoolean("logFailedRequests", false);
            if (config.getBoolean("enabled", true)) {
                boolean found = false;
                // Search for all other bStats BStats classes to see if we are the first one
                for (Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
                    try {
                        service.getField("B_STATS_VERSION"); // Our identifier :)
                        found = true; // We aren't the first
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                // Register our service
                Bukkit.getServicesManager().register(BStats.class, this, plugin, ServicePriority.Normal);
                if (!found) {
                    // We are the first!
                    startSubmitting();
                }
            }
        }

        /**
         * Adds a custom chart.
         *
         * @param chart The chart to add.
         */
        public void addCustomChart(CustomChart chart) {
            if (chart == null) {
                throw new IllegalArgumentException("Chart cannot be null!");
            }
            charts.add(chart);
        }

        /**
         * Starts the Scheduler which submits our data every 30 minutes.
         */
        private void startSubmitting() {
            final Timer timer = new Timer(true); // We use a timer cause the Bukkit scheduler is affected by server lags
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!plugin.isEnabled()) { // Plugin was disabled
                        timer.cancel();
                        return;
                    }
                    // Nevertheless we want our code to run in the Bukkit main thread, so we have to use the Bukkit scheduler
                    // Don't be afraid! The connection to the bStats server is still async, only the stats collection is sync ;)
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> submitData());
                }
            }, 1000 * 60 * 5, 1000 * 60 * 30);
            // Submit the data every 30 minutes, first time after 5 minutes to give other plugins enough time to start
            // WARNING: Changing the frequency has no effect but your plugin WILL be blocked/deleted!
            // WARNING: Just don't do it!
        }

        /**
         * Gets the plugin specific data.
         * This method is called using Reflection.
         *
         * @return The plugin specific data.
         */
        public JSONObject getPluginData() {
            JSONObject data = new JSONObject();

            String pluginName = plugin.getDescription().getName();
            String pluginVersion = plugin.getDescription().getVersion();

            data.put("pluginName", pluginName); // Append the name of the plugin
            data.put("pluginVersion", pluginVersion); // Append the version of the plugin
            JSONArray customCharts = new JSONArray();
            for (CustomChart customChart : charts) {
                // Add the data of the custom charts
                JSONObject chart = customChart.getRequestJsonObject();
                if (chart == null) { // If the chart is null, we skip it
                    continue;
                }
                customCharts.add(chart);
            }
            data.put("customCharts", customCharts);

            return data;
        }

        /**
         * Gets the server specific data.
         *
         * @return The server specific data.
         */
        private JSONObject getServerData() {
            // Minecraft specific data
            int playerAmount = Bukkit.getOnlinePlayers().size();
            int onlineMode = Bukkit.getOnlineMode() ? 1 : 0;
            String bukkitVersion = org.bukkit.Bukkit.getVersion();
            bukkitVersion = bukkitVersion.substring(bukkitVersion.indexOf("MC: ") + 4, bukkitVersion.length() - 1);

            // OS/Java specific data
            String javaVersion = System.getProperty("java.version");
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            String osVersion = System.getProperty("os.version");
            int coreCount = Runtime.getRuntime().availableProcessors();

            JSONObject data = new JSONObject();

            data.put("serverUUID", serverUUID);

            data.put("playerAmount", playerAmount);
            data.put("onlineMode", onlineMode);
            data.put("bukkitVersion", bukkitVersion);

            data.put("javaVersion", javaVersion);
            data.put("osName", osName);
            data.put("osArch", osArch);
            data.put("osVersion", osVersion);
            data.put("coreCount", coreCount);

            return data;
        }

        /**
         * Collects the data and sends it afterwards.
         */
        private void submitData() {
            final JSONObject data = getServerData();

            JSONArray pluginData = new JSONArray();
            // Search for all other bStats BStats classes to get their plugin data
            for (Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
                try {
                    service.getField("B_STATS_VERSION"); // Our identifier :)
                } catch (NoSuchFieldException ignored) {
                    continue; // Continue "searching"
                }
                // Found one!
                try {
                    pluginData.add(service.getMethod("getPluginData").invoke(Bukkit.getServicesManager().load(service)));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                }
            }

            data.put("plugins", pluginData);

            // Create a new thread for the connection to the bStats server
            new Thread(() -> {
                try {
                    // Send the data
                    sendData(data);
                } catch (Exception e) {
                    // Something went wrong! :(
                    if (logFailedRequests) {
                        plugin.getLogger().log(Level.WARNING, "Could not submit plugin stats of " + plugin.getName(), e);
                    }
                }
            }).start();

        }

        /**
         * Sends the data to the bStats server.
         *
         * @param data The data to send.
         * @throws Exception If the request failed.
         */
        private static void sendData(JSONObject data) throws Exception {
            if (data == null) {
                throw new IllegalArgumentException("Data cannot be null!");
            }
            if (Bukkit.isPrimaryThread()) {
                throw new IllegalAccessException("This method must not be called from the main thread!");
            }
            System.setProperty("http.agent", "Chrome");
            HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();

            // Compress the data to save bandwidth
            byte[] compressedData = compress(data.toString());

            // Add headers
            connection.setRequestMethod("POST");
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Connection", "close");
            connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
            connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
            connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
            connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);

            // Send data
            connection.setDoOutput(true);
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(compressedData);
            outputStream.flush();
            outputStream.close();

            connection.getInputStream().close(); // We don't care about the response - Just send our data :)
        }

        /**
         * Gzips the given String.
         *
         * @param str The string to gzip.
         * @return The gzipped String.
         * @throws IOException If the compression failed.
         */
        private static byte[] compress(final String str) throws IOException {
            if (str == null) {
                return null;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            gzip.close();
            return outputStream.toByteArray();
        }

        /**
         * Represents a custom chart.
         */
        public static abstract class CustomChart {

            // The id of the chart
            protected final String chartId;

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public CustomChart(String chartId) {
                if (chartId == null || chartId.isEmpty()) {
                    throw new IllegalArgumentException("ChartId cannot be null or empty!");
                }
                this.chartId = chartId;
            }

            protected JSONObject getRequestJsonObject() {
                JSONObject chart = new JSONObject();
                chart.put("chartId", chartId);
                try {
                    JSONObject data = getChartData();
                    if (data == null) {
                        // If the data is null we don't send the chart.
                        return null;
                    }
                    chart.put("data", data);
                } catch (Throwable t) {
                    if (logFailedRequests) {
                        Bukkit.getLogger().log(Level.WARNING, "Failed to get data for custom chart with id " + chartId, t);
                    }
                    return null;
                }
                return chart;
            }

            protected abstract JSONObject getChartData();

        }

        /**
         * Represents a custom simple pie.
         */
        public static abstract class SimplePie extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public SimplePie(String chartId) {
                super(chartId);
            }

            /**
             * Gets the value of the pie.
             *
             * @return The value of the pie.
             */
            public abstract String getValue();

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                String value = getValue();
                if (value == null || value.isEmpty()) {
                    // Null = skip the chart
                    return null;
                }
                data.put("value", value);
                return data;
            }
        }

        /**
         * Represents a custom advanced pie.
         */
        public static abstract class AdvancedPie extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public AdvancedPie(String chartId) {
                super(chartId);
            }

            /**
             * Gets the values of the pie.
             *
             * @param valueMap Just an empty map. The only reason it exists is to make your life easier.
             *                 You don't have to create a map yourself!
             * @return The values of the pie.
             */
            public abstract HashMap<String, Integer> getValues(HashMap<String, Integer> valueMap);

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                JSONObject values = new JSONObject();
                HashMap<String, Integer> map = getValues(new HashMap<>());
                if (map == null || map.isEmpty()) {
                    // Null = skip the chart
                    return null;
                }
                boolean allSkipped = true;
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (entry.getValue() == 0) {
                        continue; // Skip this invalid
                    }
                    allSkipped = false;
                    values.put(entry.getKey(), entry.getValue());
                }
                if (allSkipped) {
                    // Null = skip the chart
                    return null;
                }
                data.put("values", values);
                return data;
            }
        }

        /**
         * Represents a custom single line chart.
         */
        public static abstract class SingleLineChart extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public SingleLineChart(String chartId) {
                super(chartId);
            }

            /**
             * Gets the value of the chart.
             *
             * @return The value of the chart.
             */
            public abstract int getValue();

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                int value = getValue();
                if (value == 0) {
                    // Null = skip the chart
                    return null;
                }
                data.put("value", value);
                return data;
            }

        }

        /**
         * Represents a custom multi line chart.
         */
        public static abstract class MultiLineChart extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public MultiLineChart(String chartId) {
                super(chartId);
            }

            /**
             * Gets the values of the chart.
             *
             * @param valueMap Just an empty map. The only reason it exists is to make your life easier.
             *                 You don't have to create a map yourself!
             * @return The values of the chart.
             */
            public abstract HashMap<String, Integer> getValues(HashMap<String, Integer> valueMap);

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                JSONObject values = new JSONObject();
                HashMap<String, Integer> map = getValues(new HashMap<>());
                if (map == null || map.isEmpty()) {
                    // Null = skip the chart
                    return null;
                }
                boolean allSkipped = true;
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (entry.getValue() == 0) {
                        continue; // Skip this invalid
                    }
                    allSkipped = false;
                    values.put(entry.getKey(), entry.getValue());
                }
                if (allSkipped) {
                    // Null = skip the chart
                    return null;
                }
                data.put("values", values);
                return data;
            }

        }

        /**
         * Represents a custom simple bar chart.
         */
        public static abstract class SimpleBarChart extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public SimpleBarChart(String chartId) {
                super(chartId);
            }

            /**
             * Gets the value of the chart.
             *
             * @param valueMap Just an empty map. The only reason it exists is to make your life easier.
             *                 You don't have to create a map yourself!
             * @return The value of the chart.
             */
            public abstract HashMap<String, Integer> getValues(HashMap<String, Integer> valueMap);

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                JSONObject values = new JSONObject();
                HashMap<String, Integer> map = getValues(new HashMap<>());
                if (map == null || map.isEmpty()) {
                    // Null = skip the chart
                    return null;
                }
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    JSONArray categoryValues = new JSONArray();
                    categoryValues.add(entry.getValue());
                    values.put(entry.getKey(), categoryValues);
                }
                data.put("values", values);
                return data;
            }

        }

        /**
         * Represents a custom advanced bar chart.
         */
        public static abstract class AdvancedBarChart extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public AdvancedBarChart(String chartId) {
                super(chartId);
            }

            /**
             * Gets the value of the chart.
             *
             * @param valueMap Just an empty map. The only reason it exists is to make your life easier.
             *                 You don't have to create a map yourself!
             * @return The value of the chart.
             */
            public abstract HashMap<String, int[]> getValues(HashMap<String, int[]> valueMap);

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                JSONObject values = new JSONObject();
                HashMap<String, int[]> map = getValues(new HashMap<>());
                if (map == null || map.isEmpty()) {
                    // Null = skip the chart
                    return null;
                }
                boolean allSkipped = true;
                for (Map.Entry<String, int[]> entry : map.entrySet()) {
                    if (entry.getValue().length == 0) {
                        continue; // Skip this invalid
                    }
                    allSkipped = false;
                    JSONArray categoryValues = new JSONArray();
                    for (int categoryValue : entry.getValue()) {
                        categoryValues.add(categoryValue);
                    }
                    values.put(entry.getKey(), categoryValues);
                }
                if (allSkipped) {
                    // Null = skip the chart
                    return null;
                }
                data.put("values", values);
                return data;
            }

        }

        /**
         * Represents a custom simple map chart.
         */
        public static abstract class SimpleMapChart extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public SimpleMapChart(String chartId) {
                super(chartId);
            }

            /**
             * Gets the value of the chart.
             *
             * @return The value of the chart.
             */
            public abstract Country getValue();

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                Country value = getValue();

                if (value == null) {
                    // Null = skip the chart
                    return null;
                }
                data.put("value", value.getCountryIsoTag());
                return data;
            }

        }

        /**
         * Represents a custom advanced map chart.
         */
        public static abstract class AdvancedMapChart extends CustomChart {

            /**
             * Class constructor.
             *
             * @param chartId The id of the chart.
             */
            public AdvancedMapChart(String chartId) {
                super(chartId);
            }

            /**
             * Gets the value of the chart.
             *
             * @param valueMap Just an empty map. The only reason it exists is to make your life easier.
             *                 You don't have to create a map yourself!
             * @return The value of the chart.
             */
            public abstract HashMap<Country, Integer> getValues(HashMap<Country, Integer> valueMap);

            @Override
            protected JSONObject getChartData() {
                JSONObject data = new JSONObject();
                JSONObject values = new JSONObject();
                HashMap<Country, Integer> map = getValues(new HashMap<>());
                if (map == null || map.isEmpty()) {
                    // Null = skip the chart
                    return null;
                }
                boolean allSkipped = true;
                for (Map.Entry<Country, Integer> entry : map.entrySet()) {
                    if (entry.getValue() == 0) {
                        continue; // Skip this invalid
                    }
                    allSkipped = false;
                    values.put(entry.getKey().getCountryIsoTag(), entry.getValue());
                }
                if (allSkipped) {
                    // Null = skip the chart
                    return null;
                }
                data.put("values", values);
                return data;
            }

        }

        /**
         * A enum which is used for custom maps.
         */
        public enum Country {

            /**
             * bStats will use the country of the server.
             */
            AUTO_DETECT("AUTO", "Auto Detected"),

            ANDORRA("AD", "Andorra"),
            UNITED_ARAB_EMIRATES("AE", "United Arab Emirates"),
            AFGHANISTAN("AF", "Afghanistan"),
            ANTIGUA_AND_BARBUDA("AG", "Antigua and Barbuda"),
            ANGUILLA("AI", "Anguilla"),
            ALBANIA("AL", "Albania"),
            ARMENIA("AM", "Armenia"),
            NETHERLANDS_ANTILLES("AN", "Netherlands Antilles"),
            ANGOLA("AO", "Angola"),
            ANTARCTICA("AQ", "Antarctica"),
            ARGENTINA("AR", "Argentina"),
            AMERICAN_SAMOA("AS", "American Samoa"),
            AUSTRIA("AT", "Austria"),
            AUSTRALIA("AU", "Australia"),
            ARUBA("AW", "Aruba"),
            ALAND_ISLANDS("AX", "Åland Islands"),
            AZERBAIJAN("AZ", "Azerbaijan"),
            BOSNIA_AND_HERZEGOVINA("BA", "Bosnia and Herzegovina"),
            BARBADOS("BB", "Barbados"),
            BANGLADESH("BD", "Bangladesh"),
            BELGIUM("BE", "Belgium"),
            BURKINA_FASO("BF", "Burkina Faso"),
            BULGARIA("BG", "Bulgaria"),
            BAHRAIN("BH", "Bahrain"),
            BURUNDI("BI", "Burundi"),
            BENIN("BJ", "Benin"),
            SAINT_BARTHELEMY("BL", "Saint Barthélemy"),
            BERMUDA("BM", "Bermuda"),
            BRUNEI("BN", "Brunei"),
            BOLIVIA("BO", "Bolivia"),
            BONAIRE_SINT_EUSTATIUS_AND_SABA("BQ", "Bonaire, Sint Eustatius and Saba"),
            BRAZIL("BR", "Brazil"),
            BAHAMAS("BS", "Bahamas"),
            BHUTAN("BT", "Bhutan"),
            BOUVET_ISLAND("BV", "Bouvet Island"),
            BOTSWANA("BW", "Botswana"),
            BELARUS("BY", "Belarus"),
            BELIZE("BZ", "Belize"),
            CANADA("CA", "Canada"),
            COCOS_ISLANDS("CC", "Cocos Islands"),
            THE_DEMOCRATIC_REPUBLIC_OF_CONGO("CD", "The Democratic Republic Of Congo"),
            CENTRAL_AFRICAN_REPUBLIC("CF", "Central African Republic"),
            CONGO("CG", "Congo"),
            SWITZERLAND("CH", "Switzerland"),
            COTE_D_IVOIRE("CI", "Côte d'Ivoire"),
            COOK_ISLANDS("CK", "Cook Islands"),
            CHILE("CL", "Chile"),
            CAMEROON("CM", "Cameroon"),
            CHINA("CN", "China"),
            COLOMBIA("CO", "Colombia"),
            COSTA_RICA("CR", "Costa Rica"),
            CUBA("CU", "Cuba"),
            CAPE_VERDE("CV", "Cape Verde"),
            CURACAO("CW", "Curaçao"),
            CHRISTMAS_ISLAND("CX", "Christmas Island"),
            CYPRUS("CY", "Cyprus"),
            CZECH_REPUBLIC("CZ", "Czech Republic"),
            GERMANY("DE", "Germany"),
            DJIBOUTI("DJ", "Djibouti"),
            DENMARK("DK", "Denmark"),
            DOMINICA("DM", "Dominica"),
            DOMINICAN_REPUBLIC("DO", "Dominican Republic"),
            ALGERIA("DZ", "Algeria"),
            ECUADOR("EC", "Ecuador"),
            ESTONIA("EE", "Estonia"),
            EGYPT("EG", "Egypt"),
            WESTERN_SAHARA("EH", "Western Sahara"),
            ERITREA("ER", "Eritrea"),
            SPAIN("ES", "Spain"),
            ETHIOPIA("ET", "Ethiopia"),
            FINLAND("FI", "Finland"),
            FIJI("FJ", "Fiji"),
            FALKLAND_ISLANDS("FK", "Falkland Islands"),
            MICRONESIA("FM", "Micronesia"),
            FAROE_ISLANDS("FO", "Faroe Islands"),
            FRANCE("FR", "France"),
            GABON("GA", "Gabon"),
            UNITED_KINGDOM("GB", "United Kingdom"),
            GRENADA("GD", "Grenada"),
            GEORGIA("GE", "Georgia"),
            FRENCH_GUIANA("GF", "French Guiana"),
            GUERNSEY("GG", "Guernsey"),
            GHANA("GH", "Ghana"),
            GIBRALTAR("GI", "Gibraltar"),
            GREENLAND("GL", "Greenland"),
            GAMBIA("GM", "Gambia"),
            GUINEA("GN", "Guinea"),
            GUADELOUPE("GP", "Guadeloupe"),
            EQUATORIAL_GUINEA("GQ", "Equatorial Guinea"),
            GREECE("GR", "Greece"),
            SOUTH_GEORGIA_AND_THE_SOUTH_SANDWICH_ISLANDS("GS", "South Georgia And The South Sandwich Islands"),
            GUATEMALA("GT", "Guatemala"),
            GUAM("GU", "Guam"),
            GUINEA_BISSAU("GW", "Guinea-Bissau"),
            GUYANA("GY", "Guyana"),
            HONG_KONG("HK", "Hong Kong"),
            HEARD_ISLAND_AND_MCDONALD_ISLANDS("HM", "Heard Island And McDonald Islands"),
            HONDURAS("HN", "Honduras"),
            CROATIA("HR", "Croatia"),
            HAITI("HT", "Haiti"),
            HUNGARY("HU", "Hungary"),
            INDONESIA("ID", "Indonesia"),
            IRELAND("IE", "Ireland"),
            ISRAEL("IL", "Israel"),
            ISLE_OF_MAN("IM", "Isle Of Man"),
            INDIA("IN", "India"),
            BRITISH_INDIAN_OCEAN_TERRITORY("IO", "British Indian Ocean Territory"),
            IRAQ("IQ", "Iraq"),
            IRAN("IR", "Iran"),
            ICELAND("IS", "Iceland"),
            ITALY("IT", "Italy"),
            JERSEY("JE", "Jersey"),
            JAMAICA("JM", "Jamaica"),
            JORDAN("JO", "Jordan"),
            JAPAN("JP", "Japan"),
            KENYA("KE", "Kenya"),
            KYRGYZSTAN("KG", "Kyrgyzstan"),
            CAMBODIA("KH", "Cambodia"),
            KIRIBATI("KI", "Kiribati"),
            COMOROS("KM", "Comoros"),
            SAINT_KITTS_AND_NEVIS("KN", "Saint Kitts And Nevis"),
            NORTH_KOREA("KP", "North Korea"),
            SOUTH_KOREA("KR", "South Korea"),
            KUWAIT("KW", "Kuwait"),
            CAYMAN_ISLANDS("KY", "Cayman Islands"),
            KAZAKHSTAN("KZ", "Kazakhstan"),
            LAOS("LA", "Laos"),
            LEBANON("LB", "Lebanon"),
            SAINT_LUCIA("LC", "Saint Lucia"),
            LIECHTENSTEIN("LI", "Liechtenstein"),
            SRI_LANKA("LK", "Sri Lanka"),
            LIBERIA("LR", "Liberia"),
            LESOTHO("LS", "Lesotho"),
            LITHUANIA("LT", "Lithuania"),
            LUXEMBOURG("LU", "Luxembourg"),
            LATVIA("LV", "Latvia"),
            LIBYA("LY", "Libya"),
            MOROCCO("MA", "Morocco"),
            MONACO("MC", "Monaco"),
            MOLDOVA("MD", "Moldova"),
            MONTENEGRO("ME", "Montenegro"),
            SAINT_MARTIN("MF", "Saint Martin"),
            MADAGASCAR("MG", "Madagascar"),
            MARSHALL_ISLANDS("MH", "Marshall Islands"),
            MACEDONIA("MK", "Macedonia"),
            MALI("ML", "Mali"),
            MYANMAR("MM", "Myanmar"),
            MONGOLIA("MN", "Mongolia"),
            MACAO("MO", "Macao"),
            NORTHERN_MARIANA_ISLANDS("MP", "Northern Mariana Islands"),
            MARTINIQUE("MQ", "Martinique"),
            MAURITANIA("MR", "Mauritania"),
            MONTSERRAT("MS", "Montserrat"),
            MALTA("MT", "Malta"),
            MAURITIUS("MU", "Mauritius"),
            MALDIVES("MV", "Maldives"),
            MALAWI("MW", "Malawi"),
            MEXICO("MX", "Mexico"),
            MALAYSIA("MY", "Malaysia"),
            MOZAMBIQUE("MZ", "Mozambique"),
            NAMIBIA("NA", "Namibia"),
            NEW_CALEDONIA("NC", "New Caledonia"),
            NIGER("NE", "Niger"),
            NORFOLK_ISLAND("NF", "Norfolk Island"),
            NIGERIA("NG", "Nigeria"),
            NICARAGUA("NI", "Nicaragua"),
            NETHERLANDS("NL", "Netherlands"),
            NORWAY("NO", "Norway"),
            NEPAL("NP", "Nepal"),
            NAURU("NR", "Nauru"),
            NIUE("NU", "Niue"),
            NEW_ZEALAND("NZ", "New Zealand"),
            OMAN("OM", "Oman"),
            PANAMA("PA", "Panama"),
            PERU("PE", "Peru"),
            FRENCH_POLYNESIA("PF", "French Polynesia"),
            PAPUA_NEW_GUINEA("PG", "Papua New Guinea"),
            PHILIPPINES("PH", "Philippines"),
            PAKISTAN("PK", "Pakistan"),
            POLAND("PL", "Poland"),
            SAINT_PIERRE_AND_MIQUELON("PM", "Saint Pierre And Miquelon"),
            PITCAIRN("PN", "Pitcairn"),
            PUERTO_RICO("PR", "Puerto Rico"),
            PALESTINE("PS", "Palestine"),
            PORTUGAL("PT", "Portugal"),
            PALAU("PW", "Palau"),
            PARAGUAY("PY", "Paraguay"),
            QATAR("QA", "Qatar"),
            REUNION("RE", "Reunion"),
            ROMANIA("RO", "Romania"),
            SERBIA("RS", "Serbia"),
            RUSSIA("RU", "Russia"),
            RWANDA("RW", "Rwanda"),
            SAUDI_ARABIA("SA", "Saudi Arabia"),
            SOLOMON_ISLANDS("SB", "Solomon Islands"),
            SEYCHELLES("SC", "Seychelles"),
            SUDAN("SD", "Sudan"),
            SWEDEN("SE", "Sweden"),
            SINGAPORE("SG", "Singapore"),
            SAINT_HELENA("SH", "Saint Helena"),
            SLOVENIA("SI", "Slovenia"),
            SVALBARD_AND_JAN_MAYEN("SJ", "Svalbard And Jan Mayen"),
            SLOVAKIA("SK", "Slovakia"),
            SIERRA_LEONE("SL", "Sierra Leone"),
            SAN_MARINO("SM", "San Marino"),
            SENEGAL("SN", "Senegal"),
            SOMALIA("SO", "Somalia"),
            SURINAME("SR", "Suriname"),
            SOUTH_SUDAN("SS", "South Sudan"),
            SAO_TOME_AND_PRINCIPE("ST", "Sao Tome And Principe"),
            EL_SALVADOR("SV", "El Salvador"),
            SINT_MAARTEN_DUTCH_PART("SX", "Sint Maarten (Dutch part)"),
            SYRIA("SY", "Syria"),
            SWAZILAND("SZ", "Swaziland"),
            TURKS_AND_CAICOS_ISLANDS("TC", "Turks And Caicos Islands"),
            CHAD("TD", "Chad"),
            FRENCH_SOUTHERN_TERRITORIES("TF", "French Southern Territories"),
            TOGO("TG", "Togo"),
            THAILAND("TH", "Thailand"),
            TAJIKISTAN("TJ", "Tajikistan"),
            TOKELAU("TK", "Tokelau"),
            TIMOR_LESTE("TL", "Timor-Leste"),
            TURKMENISTAN("TM", "Turkmenistan"),
            TUNISIA("TN", "Tunisia"),
            TONGA("TO", "Tonga"),
            TURKEY("TR", "Turkey"),
            TRINIDAD_AND_TOBAGO("TT", "Trinidad and Tobago"),
            TUVALU("TV", "Tuvalu"),
            TAIWAN("TW", "Taiwan"),
            TANZANIA("TZ", "Tanzania"),
            UKRAINE("UA", "Ukraine"),
            UGANDA("UG", "Uganda"),
            UNITED_STATES_MINOR_OUTLYING_ISLANDS("UM", "United States Minor Outlying Islands"),
            UNITED_STATES("US", "United States"),
            URUGUAY("UY", "Uruguay"),
            UZBEKISTAN("UZ", "Uzbekistan"),
            VATICAN("VA", "Vatican"),
            SAINT_VINCENT_AND_THE_GRENADINES("VC", "Saint Vincent And The Grenadines"),
            VENEZUELA("VE", "Venezuela"),
            BRITISH_VIRGIN_ISLANDS("VG", "British Virgin Islands"),
            U_S__VIRGIN_ISLANDS("VI", "U.S. Virgin Islands"),
            VIETNAM("VN", "Vietnam"),
            VANUATU("VU", "Vanuatu"),
            WALLIS_AND_FUTUNA("WF", "Wallis And Futuna"),
            SAMOA("WS", "Samoa"),
            YEMEN("YE", "Yemen"),
            MAYOTTE("YT", "Mayotte"),
            SOUTH_AFRICA("ZA", "South Africa"),
            ZAMBIA("ZM", "Zambia"),
            ZIMBABWE("ZW", "Zimbabwe");

            private String isoTag;
            private String name;

            Country(String isoTag, String name) {
                this.isoTag = isoTag;
                this.name = name;
            }

            /**
             * Gets the name of the country.
             *
             * @return The name of the country.
             */
            public String getCountryName() {
                return name;
            }

            /**
             * Gets the iso tag of the country.
             *
             * @return The iso tag of the country.
             */
            public String getCountryIsoTag() {
                return isoTag;
            }

            /**
             * Gets a country by it's iso tag.
             *
             * @param isoTag The iso tag of the county.
             * @return The country with the given iso tag or <code>null</code> if unknown.
             */
            public static Country byIsoTag(String isoTag) {
                for (Country country : Country.values()) {
                    if (country.isoTag.equals(isoTag)) {
                        return country;
                    }
                }
                return null;
            }

            /**
             * Gets a country by a locale.
             *
             * @param locale The locale.
             * @return The country from the giben locale or <code>null</code> if unknown country or
             * if the locale does not contain a country.
             */
            public static Country byLocale(Locale locale) {
                return byIsoTag(locale.getCountry());
            }

        }

    }
}
