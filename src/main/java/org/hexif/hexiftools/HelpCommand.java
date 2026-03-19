package org.hexif.hexiftools;

import org.bukkit.command.CommandSender;

public class HelpCommand {

  public void sendHelp(CommandSender sender, String label) {
    sender.sendMessage("§6§lHeXifTools Commands");

    if (sender.hasPermission("hexiftools.help")) {
      sender.sendMessage("§e/" + label + " help §7- Show this help menu");
    }
    if (sender.hasPermission("hexiftools.reload")) {
      sender.sendMessage("§e/" + label + " reload §7- Reload plugin configuration");
      sender.sendMessage("§e/" + label + " reload full §7- Fully reload the plugin");
    }
    if (sender.hasPermission("hexiftools.viewcontainers")) {
      sender.sendMessage("§e/" + label + " viewcontainers §7- View tracked containers");
    }
    if (sender.hasPermission("hexiftools.config")) {
      sender.sendMessage("§e/" + label + " config §7- Manage the config");
    }
  }
}
