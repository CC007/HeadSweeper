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

import com.github.cc007.headsplugin.heads.HeadCreator;
import com.github.cc007.headsplugin.heads.HeadsPlacer;
import com.github.cc007.headsutils.heads.Head;
import com.github.cc007.headsweeper.HeadSweeper;
import com.github.cc007.mcsweeper.api.Field;
import com.github.cc007.mcsweeper.api.Sweeper;
import com.github.cc007.mcsweeper.implementation.MineSweeper;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

/**
 *
 * @author Rik Schaaf aka CC007 <http://coolcat007.nl/>
 */
public class HeadSweeperGame {

    private int x;
    private int y;
    private int z;
    private Sweeper game;
    private World world;
    Plugin plugin;

    public HeadSweeperGame(int x, int y, int z, Sweeper game, World world, Plugin plugin) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.game = game;
        this.world = world;
        this.plugin = plugin;
    }

    public HeadSweeperGame(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the value of x
     *
     * @return the value of x
     */
    public int getX() {
        return x;
    }

    /**
     * Set the value of x
     *
     * @param x new value of x
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the value of y
     *
     * @return the value of y
     */
    public int getY() {
        return y;
    }

    /**
     * Set the value of y
     *
     * @param y new value of y
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Get the value of z
     *
     * @return the value of z
     */
    public int getZ() {
        return z;
    }

    /**
     * Set the value of z
     *
     * @param z new value of z
     */
    public void setZ(int z) {
        this.z = z;
    }

    /**
     * Get the sweeper game
     *
     * @return the sweeper game
     */
    public Sweeper getGame() {
        return game;
    }

    /**
     * Set the sweeper game
     *
     * @param game new sweeper game
     */
    public void setGame(Sweeper game) {
        this.game = game;
    }

    /**
     * Get the world of the game
     *
     * @return the world of the game
     */
    public World getWorld() {
        return world;
    }

    /**
     * Set the world of the game
     *
     * @param world new world of the game
     */
    public void setWorld(World world) {
        this.world = world;
    }

    public boolean isIntersecting(World world, int x, int y, int z, int width, int height) {
        if (this.world.equals(world)) {
            if (this.y == y) {
                if (x + width >= this.x && this.x + game.getField().getWidth() >= x) {
                    if (z + height >= this.z && this.z + game.getField().getHeight() >= z) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isInField(World world, int x, int y, int z) {
        if (this.world.equals(world)) {
            if (this.y == y) {
                if (x >= this.x && x <= this.x + game.getField().getWidth() - 1) {
                    if (z >= this.z && z <= this.z + game.getField().getHeight() - 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void placeHeads() {
        for (int i = 0; i < game.getField().getWidth(); i++) {
            for (int j = 0; j < game.getField().getHeight(); j++) {
                Head head = getHeadAt(i, j);
                ItemStack stack = HeadCreator.getItemStack(head);
                HeadsPlacer.placeHead(stack, x + i, y, z + j, 0, world, plugin.getLogger());
            }
        }
    }

    public void placeAir() {
        for (int i = 0; i < game.getField().getWidth(); i++) {
            for (int j = 0; j < game.getField().getHeight(); j++) {
                world.getBlockAt(x + i, y, z + j).setType(Material.AIR);
            }
        }
    }

    private Head getHeadAt(int x, int y) {
        if (game.getField().getState(x, y) == Field.BOMB_STATE) {
            return HeadSweeper.BOMB_HEAD;
        } else if (game.getField().getState(x, y) == Field.UNKNOWN_STATE) {
            return HeadSweeper.UNKNOWN_HEAD;
        } else if (game.getField().getState(x, y) == Field.FLAG_STATE) {
            return HeadSweeper.FLAG_HEAD;
        } else {
            return HeadSweeper.NUMBER_HEADS.get(game.getField().getState(x, y));
        }
    }

    public JSONObject serialize() {
        JSONObject output = new JSONObject();
        output.put("x", x);
        output.put("y", y);
        output.put("z", z);
        output.put("game", game.serialize());
        output.put("world", world.getName());
        return output;
    }

    public void deserialize(JSONObject input) {
        x = input.getInt("x");
        y = input.getInt("y");
        z = input.getInt("z");
        game = new MineSweeper(true);
        game.deserialize(input.getJSONObject("game"));
        world = plugin.getServer().getWorld(input.getString("world"));
    }
}
