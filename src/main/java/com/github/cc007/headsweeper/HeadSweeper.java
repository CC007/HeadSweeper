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

import com.github.cc007.headsweeper.commands.HeadSweeperCommand;
import com.github.cc007.headsweeper.commands.HeadSweeperTabCompleter;
import com.github.cc007.headsplugin.HeadsPlugin;
import com.github.cc007.headsutils.HeadsUtils;
import com.github.cc007.headsutils.heads.Head;
import com.github.cc007.headsutils.heads.HeadsCategory;
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
import java.util.logging.Logger;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 *
 * @author Rik Schaaf aka CC007 <http://coolcat007.nl/>
 */
public class HeadSweeper extends HeadsPlugin {

    public static Head UNKNOWN_HEAD;
    public static Head FLAG_HEAD;
    public static Head BOMB_HEAD;
    public static Map<Integer, Head> NUMBER_HEADS;

    private Plugin vault = null;
    private Permission permission = null;
    private HeadsUtils headsUtils;
    private HeadSweeperClickListener clickListener;
    private HeadSweeperController controller;
    private Logger log;

    @Override
    public void onEnable() {
        log = getLogger();
        
        /* Config stuffs */
        this.getCategoriesConfig().options().copyDefaults(true);
        saveDefaultConfig();
        
        /* Setup the utils */
        log.log(Level.INFO, "Initializing minesweeper heads...");
        headsUtils = HeadsUtils.getInstance(log);
        headsUtils.loadCategory("sweeper");
        initHeads();
        log.log(Level.INFO, "Sweeperheads initialized");
        
        /* setup controller */
        loadGames();
        
        /* setup the listener */
        clickListener = new HeadSweeperClickListener(this);

        /* Setup plugin hooks */
        vault = getPlugin("Vault");
        if (vault != null) {
            setupPermissions();
        }
        /* Register commands */
        getCommand("headsweeper").setExecutor(new HeadSweeperCommand(this));
        getCommand("headsweeper").setTabCompleter(new HeadSweeperTabCompleter(this));

    }

    @Override
    public void onDisable() {
        PlayerInteractEvent.getHandlerList().unregister(clickListener);
        for (HeadsCategory category : headsUtils.getCategories().getList()) {
            category.clear();
        }
        headsUtils.getCategories().clear();
        headsUtils = null;
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
     * Get the HeadUtils instance
     *
     * @return the HeadUtils instance
     */
    public HeadsUtils getHeadsUtils() {
        return headsUtils;
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
                log.log(Level.SEVERE, "Couldn't create sweeperGames.json");
            }
        }
        
        JsonObject json = controller.serialize();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(json.toString());
            writer.flush();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Couldn't write to sweeperGames.json");
        }
    }

    /**
     * load the list of available games from the json file
     *
     * @return the list of available games
     */
    public void loadGames() {
        log.log(Level.INFO, "Create controller...");
        log.log(Level.INFO, "Controller created.");
        log.log(Level.INFO, "Loading games...");
        File file = new File(getDataFolder(), "sweeperGames.json");
        if (!file.exists()) {
            log.log(Level.INFO, "First use of this plugin: generate sweeperGames.json");
            saveGames();
            log.log(Level.INFO, "Games loaded.");
        }
        String jsonString = "";
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                jsonString += scanner.nextLine();
            }
            
            if (jsonString == "") {
                jsonString = "{}";
            }
            
            JsonParser parser = new JsonParser();
            
            controller = new HeadSweeperController(parser.parse(jsonString).getAsJsonObject(), this);
            log.log(Level.INFO, "Games loaded.");
        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, "Couldn't read from sweeperGames.json", ex);
        }
    }

    /**
     * Initialize the heads that will be used for the head sweeper game
     */
    private void initHeads() {
        NUMBER_HEADS = new HashMap<>();
        List<Head> heads = getHeadsUtils().getCategoryHeads("sweeper");
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
        if ((UNKNOWN_HEAD == null || BOMB_HEAD == null) || NUMBER_HEADS.size() != 9) {
            getLogger().log(Level.SEVERE, "Minesweeper heads have not been properly initialized!");
        }
    }
    
}
