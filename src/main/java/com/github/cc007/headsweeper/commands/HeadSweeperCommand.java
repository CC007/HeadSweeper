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
package com.github.cc007.headsweeper.commands;

import com.github.cc007.headsplugin.utils.HeadsUtils;
import com.github.cc007.headsweeper.HeadSweeper;
import com.github.cc007.headsweeper.controller.HeadSweeperGame;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadSweeperCommand implements CommandExecutor {

    private final HeadSweeper plugin;

    public HeadSweeperCommand(HeadSweeper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(plugin.pluginChatPrefix() + ChatColor.GOLD + "Use: /headsweeper (updateheads | reset <boardnr> | create (<worldname> <xloc> <yloc> <zloc>|here) <width in x> <depth in z> <bombcount> | delete <boardnr>)");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "updateheads":
                if (!sender.hasPermission("sweeper.update")) {
                    return false;
                }

                try {
                    HeadsUtils.getInstance().loadCategory("sweeper");
                } catch (IOException ex) {
                    Logger.getLogger(HeadSweeperCommand.class.getName()).log(Level.SEVERE, null, ex);
                }
                plugin.initHeads();
                return true;

            case "reset":
                if (!sender.hasPermission("sweeper.reset")) {
                    return false;
                }

                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage(plugin.pluginChatPrefix() + "You didn't specify which minesweeper board to reset!");
                    return false;
                }

                HeadSweeperGame game = plugin.getController().getGame(Integer.parseInt(args[1]));

                if (game == null) {
                    sender.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "There is no game with that game number!" + ChatColor.GOLD + "Tip: rightclick a game to get its number.");
                    return false;
                }

                game.getGame().resetField();
                plugin.saveGames();
                game.placeHeads();
                //game.placeHeads();//Due to a bug it can happen that heads have no texture, therefore do twice to make sure all textures are set

                sender.sendMessage(plugin.pluginChatPrefix() + ChatColor.GREEN + "The board has been reset.");
                return true;

            case "create":
                if (!sender.hasPermission("sweeper.manage")) {
                    return false;
                }

                if (args.length < 5) {
                    return false;
                }

                Location createLocation;
                int width,
                 height,
                 bombCount;

                if (args[1].equalsIgnoreCase("here")) {

                    if (args.length != 5 || !isInteger(args[2]) || !isInteger(args[3]) || !isInteger(args[4]) || !(sender instanceof Player)) {
                        return false;
                    }

                    width = Integer.parseInt(args[2]);
                    height = Integer.parseInt(args[3]);
                    bombCount = Integer.parseInt(args[4]);

                    createLocation = ((Player) sender).getLocation();
                } else {

                    if (args.length != 8 || !isInteger(args[2]) || !isInteger(args[3]) || !isInteger(args[4]) || !isInteger(args[5]) || !isInteger(args[6]) || !isInteger(args[7]) || Bukkit.getWorld(args[1]) == null) {
                        return false;
                    }

                    width = Integer.parseInt(args[5]);
                    height = Integer.parseInt(args[6]);
                    bombCount = Integer.parseInt(args[7]);

                    createLocation = new Location(Bukkit.getWorld(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                }

                plugin.getController().createNewField(createLocation, width, height, bombCount, sender);

                return true;
            case "delete":

                if (!sender.hasPermission("sweeper.manage")) {
                    return false;
                }

                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "You didn't specify which minesweeper board to delete!");
                    return false;
                }

                if (!plugin.getController().removeGame(Integer.parseInt(args[1]))) {
                    sender.sendMessage(plugin.pluginChatPrefix() + ChatColor.RED + "There is no game with that game number!" + ChatColor.GOLD + "Tip: rightclick a game to get its number.");
                } else {
                    sender.sendMessage(plugin.pluginChatPrefix() + ChatColor.GREEN + "The board has been deleted.");
                }
                return true;
            default:
                return false;
        }
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }
}
