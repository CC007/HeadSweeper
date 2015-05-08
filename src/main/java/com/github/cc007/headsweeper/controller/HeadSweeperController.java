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
import com.github.cc007.mcsweeper.implementation.MineSweeper;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Rik Schaaf aka CC007 <http://coolcat007.nl/>
 */
public class HeadSweeperController {

    private List<HeadSweeperGame> sweeperGames;
    private final HeadSweeper plugin;

    public HeadSweeperController(HeadSweeper plugin, List<HeadSweeperGame> sweeperGames) {
        this.sweeperGames = sweeperGames;
        this.plugin = plugin;
    }

    public HeadSweeperController(HeadSweeper plugin) {
        this(plugin, new ArrayList<HeadSweeperGame>());
    }

    public void createNewField(int x, int y, int z, int width, int height, int bombCount, CommandSender sender, World world) {
        if (this.isIntersecting(world, x, y, z, width, height)) {
            sender.sendMessage(ChatColor.RED + "There already is a minesweeper game at the specified location!");
        } else {
            HeadSweeperGame newGame = new HeadSweeperGame(x, y, z, new MineSweeper(width, height, bombCount), world, plugin);
            newGame.getGame().resetField();
            int index = sweeperGames.size();
            sweeperGames.add(newGame);
            plugin.saveGames();
            newGame.placeHeads();
            newGame.placeHeads();//Due to a bug it can happen that heads have no texture, therefore do twice to make sure all textures are set
            sender.sendMessage(ChatColor.GREEN + "The new minesweeper game has been created with game number " + index + ".");
        }
    }

    public boolean isIntersecting(World world, int x, int y, int z, int width, int height) {
        for (HeadSweeperGame sweeperGame : sweeperGames) {
            if (sweeperGame.isIntersecting(world, x, y, z, width, height)) {
                return true;
            }
        }
        return false;
    }

    public HeadSweeperGame getActiveGame(World world, int x, int y, int z) {
        for (HeadSweeperGame sweeperGame : sweeperGames) {
            if (sweeperGame.isInField(world, x, y, z)) {
                return sweeperGame;
            }
        }
        return null;
    }

    public int getGameNr(HeadSweeperGame activeGame) {
        int i = 0;
        for (HeadSweeperGame sweeperGame : sweeperGames) {
            if (sweeperGame.equals(activeGame)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public HeadSweeperGame getGame(int gameNr) {
        return sweeperGames.get(gameNr);
    }

    public boolean removeGame(int gameNr) {
        if (gameNr < sweeperGames.size()) {
            getGame(gameNr).placeAir();
            sweeperGames.remove(gameNr);
            plugin.saveGames();
            return true;
        }
        return false;
    }

    public List<HeadSweeperGame> getSweeperGames() {
        return sweeperGames;
    }

    public JSONObject serialize() {
        JSONObject output = new JSONObject();
        JSONArray sweeperGamesJSON = new JSONArray();
        for (HeadSweeperGame sweeperGame : sweeperGames) {
            sweeperGamesJSON.put(sweeperGame.serialize());
        }
        output.put("sweeperGames", sweeperGamesJSON);
        return output;
    }

    public void deserialize(JSONObject input) {
        JSONArray sweeperGamesJSON = input.getJSONArray("sweeperGames");
        for (int i = 0; i < sweeperGamesJSON.length(); i++) {
            HeadSweeperGame sweeperGame = new HeadSweeperGame(plugin);
            sweeperGame.deserialize(sweeperGamesJSON.getJSONObject(i));
            sweeperGames.add(sweeperGame);
        }
    }
}
