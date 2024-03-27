package DrayFireBedwars;

import java.io.File;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopConfig {
	private Main plugin;
    private File shopFile;
    private FileConfiguration shopConfig;

    public ShopConfig(Main plugin) {
        this.plugin = plugin;
        shopFile = new File(plugin.getDataFolder(), "shop.yml");
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }

    public void loadShopConfig() {
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
    }

    public ItemStack getItem(String itemName) {
        ConfigurationSection itemSection = shopConfig.getConfigurationSection("shop." + itemName);
        if (itemSection != null) {
            Material material = Material.getMaterial(itemSection.getString("material"));
            String displayName = ChatColor.translateAlternateColorCodes('&', itemSection.getString("display_name"));
            List<String> lore = itemSection.getStringList("lore");

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }

        return null;
    }
}
