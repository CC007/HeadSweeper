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
import com.google.gson.JsonObject;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

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
    private final Plugin plugin;

    public HeadSweeperGame(int x, int y, int z, Sweeper game, World world, Plugin plugin) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.game = game;
        this.world = world;
        this.plugin = plugin;

        initMetaData();
    }

    public HeadSweeperGame(JsonObject input, Plugin plugin) {

        this.plugin = plugin;

        x = input.getAsJsonPrimitive("x").getAsInt();
        y = input.getAsJsonPrimitive("y").getAsInt();
        z = input.getAsJsonPrimitive("z").getAsInt();

        game = new MineSweeper(true);
        game.deserialize(input.getAsJsonObject("game"));

        world = null;

        try {
            UUID worldUID = UUID.fromString(input.getAsJsonPrimitive("world").getAsString());
            world = Bukkit.getServer().getWorld(worldUID);
        } catch (IllegalArgumentException e) {
            world = Bukkit.getServer().getWorld(input.getAsJsonPrimitive("world").getAsString());
        }

        initMetaData();
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
                HeadsPlacer.placeHead(stack, x + i, y, z + j, 0, world, Bukkit.getLogger());
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

    public JsonObject serialize() {
        JsonObject output = new JsonObject();
        output.addProperty("x", x);
        output.addProperty("y", y);
        output.addProperty("z", z);
        output.add("game", game.serialize());
        output.addProperty("world", world.getUID().toString());
        return output;
    }

    private void initMetaData() {
        for (int i = x; i < x + this.getGame().getField().getWidth(); i++) {
            for (int j = z; j < z + this.getGame().getField().getHeight(); j++) {

                Block headBlock = this.getWorld().getBlockAt(i, this.getY(), j);
                headBlock.setMetadata("sweeperBlock", new FixedMetadataValue(plugin, "headBlock"));

                if (this.getY() != 0) {
                    Block underBlock = this.getWorld().getBlockAt(i, this.getY() - 1, j);
                    underBlock.setMetadata("sweeperBlock", new FixedMetadataValue(plugin, "underBlock"));
                }
            }
        }
    }

    void removeMetaData() {
        for (int i = x; i < x + this.getGame().getField().getWidth(); i++) {
            for (int j = this.getZ(); j < z + this.getGame().getField().getHeight(); j++) {

                Block headBlock = this.getWorld().getBlockAt(i, this.getY(), j);
                headBlock.removeMetadata("sweeperBlock", plugin);

                if (this.getY() != 0) {
                    Block underBlock = this.getWorld().getBlockAt(i, this.getY() - 1, j);
                    underBlock.removeMetadata("sweeperBlock", plugin);
                }
            }
        }
    }

}
