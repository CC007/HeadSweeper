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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 *
 * @author Rik Schaaf aka CC007 <http://coolcat007.nl/>
 */
public class HeadSweeperClickListener implements Listener {

    private HeadSweeper plugin;

    public HeadSweeperClickListener(HeadSweeper plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            HeadSweeperGame activeGame = plugin.getController().getActiveGame(clickedBlock.getWorld(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
            if (activeGame != null) {
                event.setCancelled(true);
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    headClicked(event.getClickedBlock().getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ(), event.getPlayer(), activeGame);
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.getPlayer().sendMessage("This is game has game number " + plugin.getController().getGameNr(activeGame));
                }
            }
        }
    }

    public void headClicked(int x, int y, int z, Player player, HeadSweeperGame activeGame) {
        int gameNr = plugin.getController().getGameNr(activeGame);
        if (!activeGame.getGame().hasWon() && !activeGame.getGame().hasLost()) {
            int fieldX = x - activeGame.getX();
            int fieldY = z - activeGame.getZ();
            activeGame.getGame().sweep(fieldX, fieldY);
            plugin.saveGames();
            activeGame.placeHeads();
            activeGame.placeHeads();//Due to a bug it can happen that heads have no texture, therefore do twice to make sure all textures are set
            if (activeGame.getGame().hasWon()) {
                player.sendMessage("You have won the game! Type \"/sweeper reset " + gameNr + "\" to reset the game");
            } else if (activeGame.getGame().hasLost()) {
                player.sendMessage("You have lost the game! Type \"/sweeper reset " + gameNr + "\" to reset the game");
            }
        } else {
            player.sendMessage("The game has already ended. Type \"/sweeper reset " + gameNr + "\" to reset the game");
        }
    }
}
