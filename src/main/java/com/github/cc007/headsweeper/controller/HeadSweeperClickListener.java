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
package com.github.cc007.headsweeper.controller;

import com.github.cc007.headsweeper.HeadSweeper;
import com.github.cc007.mcsweeper.api.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadSweeperClickListener implements Listener {

    private final HeadSweeper plugin;
    private final Map<Integer, Date> lastFlagged;

    public HeadSweeperClickListener(HeadSweeper plugin) {
        this.plugin = plugin;
        lastFlagged = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        if (clickedBlock.hasMetadata("sweeperBlock") && clickedBlock.getMetadata("sweeperBlock").get(0).asString().equals("underBlock")) {
            event.setCancelled(true);
            return;
        }

        HeadSweeperGame activeGame = plugin.getController().getActiveGame(clickedBlock.getWorld(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());

        if (activeGame == null) {
            return;
        }

        event.setCancelled(true);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            headClicked(event.getClickedBlock().getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ(), event.getPlayer(), activeGame);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            headFlagged(event.getClickedBlock().getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ(), event.getPlayer(), activeGame);
        }

    }

    public void headClicked(int x, int y, int z, Player player, HeadSweeperGame activeGame) {
        if (!player.hasPermission("sweeper.interact")) {
            player.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "You don't have the permission to play minesweeper games. Ask an operator if you think you should have the permission.");
            return;
        }
        if (activeGame.getGame().hasWon() || activeGame.getGame().hasLost()) {
            player.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "The game has already ended. Reset this board to play a new game.");
            return;
        }

        int fieldX = x - activeGame.getX();
        int fieldY = z - activeGame.getZ();
        activeGame.getGame().sweep(fieldX, fieldY);
        plugin.saveGames();
        if (plugin.isInit()) {
            activeGame.placeHeads();
        } else {
            plugin.getLogger().log(Level.SEVERE, "The plugin has not properly been initialized. Run /headsweeper updateheads to initialize the heads for this plugin");
            player.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "The plugin has not properly been initialized. Contact an admin to fix this issue");
        }
        if (activeGame.getGame().hasWon()) {
            player.sendMessage(plugin.pluginChatPrefix() + ChatColor.GREEN + "You have won the game! Reset the board to play another game.");
        } else if (activeGame.getGame().hasLost()) {
            player.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "You have lost the game! Reset the board to play another game.");
        }

    }

    public void headFlagged(int x, int y, int z, Player player, HeadSweeperGame activeGame) {
        int gameNr = plugin.getController().getGameNr(activeGame);
        if (lastFlagged.containsKey(gameNr) && lastFlagged.get(gameNr) != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MILLISECOND, -250);
            Date checkDate = cal.getTime();
            if (lastFlagged.get(gameNr).after(checkDate)) {
                return;
            }
        }
        lastFlagged.put(gameNr, new Date());
        int fieldX = x - activeGame.getX();
        int fieldY = z - activeGame.getZ();

        if (activeGame.getGame().hasLost() || activeGame.getGame().hasWon() || activeGame.getGame().getField().getState(fieldX, fieldY) >= Field.BOMB_STATE) {
            if (player.hasPermission("sweeper.lookup")) {
                player.sendMessage(plugin.pluginChatPrefix() + "This is game has game number " + gameNr);
            }
            return;
        }

        activeGame.getGame().flag(fieldX, fieldY);
        plugin.saveGames();
        if (plugin.isInit()) {
            activeGame.placeHeads();
        } else {
            plugin.getLogger().log(Level.SEVERE, "The plugin has not properly been initialized. Run /headsweeper updateheads to initialize the heads for this plugin");
            player.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "The plugin has not properly been initialized. Contact an admin to fix this issue");
        }
    }
}
