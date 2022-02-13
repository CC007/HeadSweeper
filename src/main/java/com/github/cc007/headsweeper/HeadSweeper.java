/* 
 * The MIT License
 *
 * Copyright 2015 Rik Schaaf aka CC007 <http://coolcat007.nl/>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.cc007.headsweeper;

import com.github.cc007.headsplugin.api.HeadsPluginApi;
import com.github.cc007.headsplugin.api.business.domain.Head;
import com.github.cc007.headsplugin.api.business.services.heads.HeadSearcher;
import com.github.cc007.headsweeper.commands.HeadSweeperCommand;
import com.github.cc007.headsweeper.controller.HeadSweeperClickListener;
import com.github.cc007.headsweeper.controller.HeadSweeperController;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadSweeper extends JavaPlugin {

    public static Head UNKNOWN_HEAD;
    public static Head FLAG_HEAD;
    public static Head BOMB_HEAD;
    public static Map<Integer, Head> NUMBER_HEADS;
    private boolean init = false;

    private Plugin vault = null;
    private Permission permission = null;
    private HeadSweeperClickListener clickListener;
    private HeadSweeperController controller;

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "Check if data folder exists...");
        if (!getDataFolder().exists()) {
            getLogger().log(Level.INFO, "Data folder doesn't exist yet, creating folder");
            getDataFolder().mkdir();
        } else {
            getLogger().log(Level.INFO, "Data folder already exists");
        }

        /* Configure BStats metrics */
        Metrics metrics = new Metrics(this, 5876);

        /* Setup the sweeper heads */
        getLogger().log(Level.INFO, "Initializing minesweeper heads...");
        initHeads();
        getLogger().log(Level.INFO, "Sweeperheads initialized");

        /* setup controller */
        loadGames(); // has logs, doesn't need extra

        /* setup the listener */
        clickListener = new HeadSweeperClickListener(this);

        /* Setup plugin hooks */
        vault = getPlugin("Vault");
        if (vault != null) {
            setupPermissions();
        }

        /* Register commands */
        getCommand("headsweeper").setExecutor(new HeadSweeperCommand(this));

    }

    @Override
    public void onDisable() {
        PlayerInteractEvent.getHandlerList().unregister(clickListener);
        vault = null;
        permission = null;
    }

    /**
     * Setup permissions
     *
     * @return True: Setup correctly, Didn't setup correctly
     */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        if (permission == null) {
            getLogger().log(Level.WARNING, "Could not hook Vault!");
        } else {
            getLogger().log(Level.WARNING, "Hooked Vault!");
        }

        return (permission != null);
    }

    
    /**
     * Get if the heads are initialized
     * 
     * @return true if they are initialized, otherwise false
     */
    public boolean isInit() {
        return init;
    }
    
    /**
     * Get the vault
     *
     * @return the vault
     */
    public Plugin getVault() {
        return vault;
    }

    /**
     * Get the permissions
     *
     * @return the permissions
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Get the HeadSweeper controller
     *
     * @return the HeadSweeper controller
     */
    public HeadSweeperController getController() {
        return controller;
    }

    /**
     * Save the currently available games to a json file
     */
    public void saveGames() {
        File file = new File(getDataFolder(), "sweeperGames.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Couldn't create sweeperGames.json");
            }
        }

        JsonObject json = controller.serialize();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(json.toString());
            writer.flush();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Couldn't write to sweeperGames.json");
        }
    }

    /**
     * load the list of available games from the json file
     */
    public void loadGames() {
        getLogger().log(Level.INFO, "Loading games...");
        File file = new File(getDataFolder(), "sweeperGames.json");
        if (!file.exists()) {
            getLogger().log(Level.INFO, "First use of this plugin: generate sweeperGames.json");
            try {
                file.createNewFile();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Couldn't create sweeperGames.json");
            }
            getLogger().log(Level.INFO, "Games loaded.");
        }
        try (Scanner scanner = new Scanner(file)) {
            String jsonString = getJsonString(scanner);
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();

            getLogger().log(Level.INFO, "Create controller...");
            controller = new HeadSweeperController(jsonObject, this);
            getLogger().log(Level.INFO, "Controller created.");
        } catch (FileNotFoundException ex) {
            getLogger().log(Level.SEVERE, "Couldn't read from sweeperGames.json", ex);
        }
    }

    private String getJsonString(Scanner scanner) {
        StringBuilder jsonStringBuilder = new StringBuilder();
        while (scanner.hasNextLine()) {
            jsonStringBuilder.append(scanner.nextLine());
        }
        String jsonString = jsonStringBuilder.toString();

        if ("".equals(jsonString)) {
            jsonString = "{}";
        }
        return jsonString;
    }

    /**
     * Initialize the heads that will be used for the head sweeper game
     */
    public void initHeads() {
        HeadSearcher headSearcher = HeadsPluginApi.getHeadsPluginServices().orElseThrow(IllegalStateException::new).headSearcher();

        // make sure that the heads are in the database
        headSearcher.getHeads("Minesweeper");
        headSearcher.getHeads("TNT");

        UNKNOWN_HEAD = getHead("unknown tile", "5ec2960e-8233-3ca4-b235-7a9af34755fd", "30deb948-c6d0-48c1-9899-e61f9a8257c0", headSearcher);
        FLAG_HEAD = getHead("flag tile", "96f10d25-92c6-3b82-a961-203e49b88162", "cce8d286-c327-4bdc-a2f4-8e6b75eed62a", headSearcher);
        BOMB_HEAD = getHead("bomb tile", "a4341464-f2c9-3b3e-8941-3e6de5f105ea", "3d80d659-36cd-4aee-8540-8cdb548ede75", headSearcher);

        NUMBER_HEADS = new HashMap<>();
        NUMBER_HEADS.put(0, getHead("0 tile", "d5a38870-020b-3063-9f46-148ba463db20", "9d756ab9-765c-41a5-8c8e-853203c5c274", headSearcher));
        NUMBER_HEADS.put(1, getHead("1 tile", "1cb8b22b-6ff1-34c7-834f-d2601f00ceef", "9b9255a2-6f65-48ee-a877-2000505047bd", headSearcher));
        NUMBER_HEADS.put(2, getHead("2 tile", "5c38d26d-19e6-3a3e-b553-be25ab855392", "30beeb33-cd7b-4b26-bbf6-24225741cf33", headSearcher));
        NUMBER_HEADS.put(3, getHead("3 tile", "2ce77649-ad2c-3c5f-ab70-127669354c46", "98b890ff-fa7e-4fd0-a27b-99e6883a6219", headSearcher));
        NUMBER_HEADS.put(4, getHead("4 tile", "c9e002f0-9d72-3856-ba33-d66d7870bfe3", "aa109b98-76c2-4336-b50e-cbce1bb8e45f", headSearcher));
        NUMBER_HEADS.put(5, getHead("5 tile", "abe8f80a-0f82-32b3-b0f4-28113bf6a713", "50970266-12e0-4b36-aa2c-6093027fcdb3", headSearcher));
        NUMBER_HEADS.put(6, getHead("6 tile", "233037d3-4937-38f2-bb4b-56c34a80f730", "a8a879cd-174e-40bd-9699-117c498e245f", headSearcher));
        NUMBER_HEADS.put(7, getHead("7 tile", "c1ca5b99-c159-36f0-a29a-d488e6efc7e5", "9538924b-fa99-4f9b-8663-911a6c784501", headSearcher));
        NUMBER_HEADS.put(8, getHead("8 tile", "f63b5c7b-0980-35ab-b386-b74985ef5b6a", "ef74d8b9-065c-4d04-ac75-c813ae48a845", headSearcher));

        init = true;
    }

    private Head getHead(String headName, String primaryUuid, String secondaryUuid, HeadSearcher headSearcher) {

        return headSearcher.getHead(UUID.fromString(primaryUuid))
                .orElseGet(() -> headSearcher.getHead(UUID.fromString(secondaryUuid))
                        .orElseThrow(() -> {
                            init = false;
                            return new IllegalStateException("Minesweeper heads have not been properly initialized: " + headName + " head not found!");
                        })
                );
    }

    /**
     * get the minecraft chat prefix for this plugin
     *
     * @return the minecraft chat prefix for this plugin
     */
    public String pluginChatPrefix() {
        return ChatColor.DARK_AQUA + "[" + ChatColor.GOLD + "Head" + ChatColor.RED + "Sweeper" + ChatColor.DARK_AQUA + "]" + ChatColor.WHITE + " ";
    }

    /**
     * Gets a plugin
     *
     * @param pluginName Name of the plugin to get
     * @return The plugin from name
     */
    protected Plugin getPlugin(String pluginName) {
        if (getServer().getPluginManager().getPlugin(pluginName) != null && getServer().getPluginManager().getPlugin(pluginName).isEnabled()) {
            return getServer().getPluginManager().getPlugin(pluginName);
        } else {
            getLogger().log(Level.WARNING, "Could not find plugin \"{0}\"!", pluginName);
            return null;
        }
    }
}
