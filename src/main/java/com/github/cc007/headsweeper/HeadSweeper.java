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

import com.github.cc007.headsplugin.exceptions.AuthenticationException;
import com.github.cc007.headsweeper.commands.HeadSweeperCommand;
import com.github.cc007.headsplugin.utils.HeadsUtils;
import com.github.cc007.headsplugin.utils.heads.Head;
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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
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
        String jsonString = "";
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                jsonString += scanner.nextLine();
            }

            if ("".equals(jsonString)) {
                jsonString = "{}";
            }

            JsonParser parser = new JsonParser();

            getLogger().log(Level.INFO, "Create controller...");
            controller = new HeadSweeperController(parser.parse(jsonString).getAsJsonObject(), this);
            getLogger().log(Level.INFO, "Controller created.");
        } catch (FileNotFoundException ex) {
            getLogger().log(Level.SEVERE, "Couldn't read from sweeperGames.json", ex);
        }
    }

    /**
     * Initialize the heads that will be used for the head sweeper game
     */
    public void initHeads() {
        try {
            if(!HeadsUtils.getInstance().getCategories().hasCategory("sweeper")){
                HeadsUtils.getInstance().loadCategory("sweeper");
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        } catch (AuthenticationException ex) {
            getLogger().log(Level.WARNING, "You don't have permission to load the sweeper category.");
        }
        NUMBER_HEADS = new HashMap<>();
        List<Head> heads = HeadsUtils.getInstance().getCategoryHeads("sweeper");
        if (heads != null) {
            for (Head head : heads) {
                if (head.getName().equalsIgnoreCase("Minesweeper Unknown Tile")) {
                    UNKNOWN_HEAD = head;
                } else if (head.getName().equalsIgnoreCase("Minesweeper Flag Tile")) {
                    FLAG_HEAD = head;
                } else if (head.getName().equalsIgnoreCase("TNT [1.8]")) {
                    BOMB_HEAD = head;
                } else {
                    for (int i = 0; i < 9; i++) {
                        if (head.getName().equalsIgnoreCase("Minesweeper " + i + " Tile")) {
                            NUMBER_HEADS.put(i, head);
                        }
                    }
                }
            }
        }
        if ((UNKNOWN_HEAD == null || BOMB_HEAD == null) || NUMBER_HEADS.size() != 9) {
            getLogger().log(Level.SEVERE, "Minesweeper heads have not been properly initialized!");
            init = false;
        }else{
            init = true;
        }        
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
