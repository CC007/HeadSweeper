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

import com.github.cc007.headsweeper.HeadSweeper;
import com.github.cc007.headsplugin.heads.HeadsPlacer;
import com.github.cc007.headsweeper.controller.HeadSweeperGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Autom
 */
public class HeadSweeperCommand implements CommandExecutor {

    private final HeadSweeper plugin;

    public HeadSweeperCommand(HeadSweeper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Use: /(headsweeper|mcsweeper|sweeper) (updateheads | reset <boardnr> | create (<worldname> <xloc> <yloc> <zloc>|here) <width in x> <depth in z> <bombcount> | delete <boardnr>)");
        } else {
            if (args[0].equalsIgnoreCase("updateheads") && sender.hasPermission("sweeper.update")) {
                plugin.getHeadsUtils().loadCategory("sweeper");
                return true;
            }
            if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("sweeper.reset")) {
                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage("You didn't specify which minesweeper board to reset!");
                } else {
                    HeadSweeperGame game = plugin.getController().getGame(Integer.parseInt(args[1]));
                    if (game != null) {
                        game.getGame().resetField();
                        game.placeHeads();
                        game.placeHeads();//Due to a bug it can happen that heads have no texture, therefore do twice to make sure all textures are set
                        sender.sendMessage(ChatColor.GREEN + "The board has been reset.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "There is no game with that game number!" + ChatColor.GOLD + "Tip: rightclick a game to get its number.");
                    }
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("create") && sender.hasPermission("sweeper.manage")) {
                if (args.length == 5) {
                    if (!args[1].equalsIgnoreCase("here")) {
                        sender.sendMessage(ChatColor.RED + "You didn't use this command correctly! Use: /(headsweeper|mcsweeper|sweeper) create (<worldname> <xloc> <yloc> <zloc>|here) <width in x> <depth in z> <bombcount>");
                    } else if (!isInteger(args[2]) || !isInteger(args[3]) || !isInteger(args[4])) {
                        sender.sendMessage(ChatColor.RED + "Not all of the parameters you gave were numbers!");
                    } else {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Only players can use \"here\" in this command!");
                            return true;
                        }
                        Player player = (Player) sender;
                        plugin.getController().createNewField(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ(), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), sender, player.getWorld());
                    }
                } else if (args.length == 8) {
                    World world = Bukkit.getWorld(args[1]);
                    if (world == null) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "The world that you specified does not exist! " + ChatColor.GOLD + "Tip: the first world is called \"" + plugin.getServer().getWorlds().get(0).getName() + "\".");
                            return true;
                        }
                        Player player = (Player) sender;
                        sender.sendMessage(ChatColor.RED + "The world that you specified does not exist! " + ChatColor.GOLD + "Tip: the world you are in is called \"" + player.getWorld().getName() + "\".");
                    } else if (!isInteger(args[2]) || !isInteger(args[3]) || !isInteger(args[4]) || !isInteger(args[5]) || !isInteger(args[6]) || !isInteger(args[7])) {
                        sender.sendMessage(ChatColor.RED + "Not all of the parameters you gave were numbers!");
                    } else {
                        plugin.getController().createNewField(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), sender, world);

                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You didn't use this command correctly! Use: /(headsweeper|mcsweeper|sweeper) create (<worldname> <xloc> <yloc> <zloc>|here) <width in x> <depth in z> <bombcount>");
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("delete") && sender.hasPermission("sweeper.manage")) {
                if (args.length < 2 || !isInteger(args[1])) {
                    sender.sendMessage(ChatColor.RED + "You didn't specify which minesweeper board to delete!");
                } else {
                    if (plugin.getController().removeGame(Integer.parseInt(args[1]))) {
                        sender.sendMessage(ChatColor.GREEN + "The board has been deleted.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "There is no game with that game number!" + ChatColor.GOLD + "Tip: rightclick a game to get its number.");
                    }
                }
                return true;
            }
        }
        return false;
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
