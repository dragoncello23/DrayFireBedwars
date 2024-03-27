package DrayFireBedwars;

import org.bukkit.Bukkit;


import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class Main extends JavaPlugin implements CommandExecutor, Listener {
   
    private boolean bedWarsRunning = false;
    private Map<String, Location> arenaLocations = new HashMap<>();
    private Map<String, Team> teams = new HashMap<>();
    private Map<String, Location> bedLocations = new HashMap<>();
    private Scoreboard scoreboard;
    private YamlConfiguration shopConfig;
    private int roundCountdownTask;
    private File configFile;
    private FileConfiguration config;
    private Set<Player> privateGamePlayers = new HashSet<>(); 
    private boolean privateGameRunning = false;
    private String bedWarsPrefix = "";
    private Map<String, Integer> playerLevels = new HashMap<>();
    private Set<Generator> generators = new HashSet<>();
    private Objective o;
    private int privateGameCountdown = 155; // Numero di secondi prima dell'avvio della BedWars privata
    private int privateGameCountdownTask;
    private HashMap<OfflinePlayer, Score> scores = new HashMap<OfflinePlayer, Score>();
    private BedwarsScoreboard bedwarsScoreboard;
    public FileConfiguration getShopConfig() {
        return this.shopConfig;
    }
    public Main(YamlConfiguration BedWarsCommandExecutor) {
        this.shopConfig = BedWarsCommandExecutor;
    }

    public Main() {
        // Costruttore vuoto senza argomenti
    }

    public class ShopConfig {
        // ... Il resto della tua implementazione ...
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onEnable() {

    	
    	 Bukkit.getServer().getPluginManager().registerEvents(this, this);
         
         scoreboard = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
         
         o = scoreboard.registerNewObjective("test", "dummy");
         o.setDisplayName("Steps");
         o.setDisplaySlot(DisplaySlot.SIDEBAR);
         
         saveDefaultConfig();
         
         List<String> s = getConfig().getStringList("scores");
         
         for (String str : s) {
             String[] words = str.split(":");
             scores.put(Bukkit.getServer().getOfflinePlayer(words[0]), o.getScore(Bukkit.getServer().getOfflinePlayer(ChatColor.GREEN + "Number:")));
             scores.get(Bukkit.getServer().getOfflinePlayer(words[0])).setScore(Integer.parseInt(words[1]));
         }
         
    	
    	ItemStack anvilItem = new ItemStack(Material.ANVIL);
        ItemMeta anvilMeta = anvilItem.getItemMeta();
        anvilMeta.setDisplayName(ChatColor.BLUE + "Crea BedWars Privata ");
        anvilItem.setItemMeta(anvilMeta);

        // Dai l'anvil solo ai giocatori con il permesso
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("dfm.createprivatebedwars")) {
                player.getInventory().addItem(anvilItem);
            }
        }
        getCommand("bedwars").setExecutor(this);
        scoreboard = getServer().getScoreboardManager().getNewScoreboard();
        configFile = new File(getDataFolder(), "config/bedwars.yml");
        int taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            int currentMinute = (int) (System.currentTimeMillis() / 1000L / 45L); // Calcola il minuto corrente

            switch (currentMinute % 5) {
                case 0:
                    sendAdvertisement1();
                    break;
                case 1:
                    sendAdvertisement2();
                    break;
                case 2:
                    sendAdvertisement3();
                    break;
                case 3:
                    sendAdvertisement4();
                    break;
                case 4:
                    sendAdvertisement5();
                    break;
                
            }
        }, 0, 20 * 45L);


        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs(); // Crea tutte le cartelle necessarie
            saveResource("config/bedwars.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        loadConfig(); // Aggiunta per leggere i dati salvati in bedwars.yml
        // Aggiungi questo per caricare anche la configurazione dello shop
        File shopFile = new File(getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            shopFile.getParentFile().mkdirs(); // Crea tutte le cartelle necessarie
            saveResource("shop.yml", false);
            
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }
 


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo i giocatori possono eseguire questo comando.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("bedwars")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("start") && args.length > 1 && args[1].equalsIgnoreCase("Bedwars")) {
                    if (!bedWarsRunning) {
                        if (areAllArenasSet()) {
                            startBedWars1();
                            player.sendMessage(ChatColor.BLUE + "BedWars avviato con successo!");
                        } else {
                            player.sendMessage(ChatColor.RED +"Devi impostare tutte le arene di spawn prima di avviare BedWars.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED +"Il BedWars è già in corso.");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("private") && args.length > 1 && args[1].equalsIgnoreCase("create")) {
                	if (player.hasPermission("dfm.createprivatebedwars")) {
                        createPrivateGame(player);
                        player.sendMessage(ChatColor.BLUE + "Partita privata di BedWars creata con successo!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Non hai il permesso per creare partite private di BedWars.");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("join") && args.length > 1 && args[1].equalsIgnoreCase("private")) {
                    joinPrivateGame(player);
                    player.sendMessage(ChatColor.BLUE + "Ti sei unito a una partita privata di BedWars!");
                    return true;
                } else if (args[0].equalsIgnoreCase("start") && args.length > 1 && args[1].equalsIgnoreCase("private")) {
                    if (player.hasPermission("dfm.startprivatebedwars")) {
                        startPrivateGame(player);
                        player.sendMessage("Round privato di BedWars avviato con successo!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Non hai il permesso per avviare partite private di BedWars.");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("create")) {
                    if (args.length >= 2) {
                        String generatorType = args[1].toUpperCase();

                        try {
                            Material material = Material.valueOf(generatorType);
                            // Resto del codice per la gestione del comando (crea generatore, ecc.)
                            // ...
                            sender.sendMessage("Generatore creato con successo!");
                            return true;
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("Tipo di generatore non valido. Utilizza IRON, GOLD o DIAMOND.");
                            return false;
                        }
                    } else {
                        sender.sendMessage("Utilizzo: /bedwars create <IRON/GOLD/DIAMOND>");
                        return false;
                    }
                }


                if (args.length > 2 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("prefix")) {
                    // Esempio: /bedwars set prefix [🎇<numero di livello>]
                    StringBuilder prefixBuilder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        prefixBuilder.append(args[i]).append(" ");
                    }
                    bedWarsPrefix = ChatColor.translateAlternateColorCodes('&', prefixBuilder.toString().trim());
                    sender.sendMessage("Prefisso impostato con successo: " + bedWarsPrefix);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("set") && args.length > 2 && args[1].equalsIgnoreCase("arena")) {
                    String arenaName = args[2];
                    Location playerLocation = player.getLocation();
                    setArenaSpawn(arenaName, playerLocation);
                    player.sendMessage(ChatColor.BLUE + "Arena di spawn " + arenaName + " impostata con successo!");
                    saveConfig(); // Salva le impostazioni nel file bedwars.yml
                    return true;
                } else if (args[0].equalsIgnoreCase("team") && args.length > 2 && args[1].equalsIgnoreCase("create")) {
                    String teamName = args[2];
                    createTeam(player, teamName);
                    player.sendMessage("Team " + teamName + " creato con successo!");
                    player.sendMessage(ChatColor.BLUE + "Team " + teamName + " creato con successo!");
                    saveConfig(); // Salva i nomi dei team nel file bedwars.yml
                    return true;
                } else if (args[0].equalsIgnoreCase("set") && args.length > 2 && args[1].equalsIgnoreCase("bed")) {
                    String teamName = args[2];
                    setBedLocation(player, teamName);
                    player.sendMessage(ChatColor.BLUE + "Posizione del letto per il team " + teamName + " impostata con successo!");
                    saveConfig(); // Salva le impostazioni nel file bedwars.yml
                    return true;
                } else if (args[0].equalsIgnoreCase("shop")) {
                    openShop(player);
                    return true;
                }
                if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                	if (player.hasPermission("dfm.help")) {
                		sendHelpMessage(sender);
                    } else {
                        player.sendMessage(ChatColor.RED + "Non hai il permesso per il comando /bedwars help");
                    }
                    
                    return true;
                }
                

            }
        

        return false;
        }
		return bedWarsRunning;
    }


    private void openShop(Player player) {
    	{
    		Inventory shopInventory = getServer().createInventory(null, 27, "BedWars Shop");

            ConfigurationSection shopSection = getShopConfig().getConfigurationSection("shop");
            if (shopSection != null) {
                for (String key : shopSection.getKeys(false)) {
                    ItemStack item = shopSection.getItemStack(key);
                    shopInventory.addItem(item);
                }
            }

            player.openInventory(shopInventory);
        }
    	}

    private void loadAdvertisements(String sendAdvertisement1) {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Carica le scritte pubblicitarie dal file di configurazione
        sendAdvertisement1 = config.getString("advertisement1", "Messaggio di default 1");
        String sendAdvertisement2 = config.getString("advertisement2", "Messaggio di default 2");
        String sendAdvertisement3 = config.getString("advertisement3", "Messaggio di default 3");
        String sendAdvertisement4 = config.getString("advertisement4", "Messaggio di default 4");
        String sendAdvertisement5 = config.getString("advertisement5", "Messaggio di default 5");
    }
    private void saveAdvertisements(Object sendAdvertisement1, Object sendAdvertisement2, Object sendAdvertisement3, Object sendAdvertisement4, Object sendAdvertisement5) {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Salva le scritte pubblicitarie nel file di configurazione
        config.set("advertisement1", sendAdvertisement1);
        config.set("advertisement2", sendAdvertisement2);
        config.set("advertisement3", sendAdvertisement3);
        config.set("advertisement4", sendAdvertisement4);
        config.set("advertisement5", sendAdvertisement5);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setArenaSpawn(String arenaName, Location location) {
        arenaLocations.put(arenaName, location);
        // Puoi aggiungere ulteriori logiche o salvare questa posizione a seconda delle tue esigenze.
    }

    private boolean areAllArenasSet() {
        return !arenaLocations.isEmpty();
    }

    private void startBedWars1() {
    	bedWarsRunning = true;
        teleportPlayersToArena();
        teleportPlayersToBeds();  // Aggiunto teletrasporto dei giocatori ai letti
        
        
    }

    private void teleportPlayersToArena() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(Objects.requireNonNull(arenaLocations.values().iterator().next())); // Teletrasporta tutti i giocatori alla prima arena
        }
    }

 

    private void teleportPlayersToBeds() {
    	 for (Player player : Bukkit.getOnlinePlayers()) {
    	        Team playerTeam = getPlayerTeam(player);
    	        if (playerTeam != null && bedLocations.containsKey(playerTeam.getName())) {
    	            Location bedLocation = bedLocations.get(playerTeam.getName());
    	            player.teleport(bedLocation);
    	        }
    	    }
    }

    private Team getPlayerTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.hasEntry(player.getName())) {
                return team;
            }
        }
        return null;
    }

    private void createTeam(Player player, String teamName) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            teams.put(teamName, team);
            player.sendMessage("Team " + teamName + " creato con successo!");
        } else {
            player.sendMessage("Il team " + teamName + " esiste già.");
        }
        // Aggiungi qui eventuali altre impostazioni del team
    }

    private void setBedLocation(Player player, String teamName) {
        Team team = teams.get(teamName);
        if (team != null) {
            Location bedLocation = player.getLocation();
            bedLocations.put(teamName, bedLocation);
            // Puoi aggiungere ulteriori logiche o salvare questa posizione a seconda delle tue esigenze.
            saveConfig(); // Salva le impostazioni nel file bedwars.yml
        } else {
            player.sendMessage("Il team " + teamName + " non esiste. Crea il team prima di impostare la posizione del letto.");
        }
    }

    private Team getTeamByName(String teamName) {
        return teams.get(teamName);
    }

    private void loadConfig() {
        // Carica le posizioni delle arene da bedwars.yml
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaName : arenasSection.getKeys(false)) {
                String worldName = arenasSection.getString(arenaName + ".world");
                double x = arenasSection.getDouble(arenaName + ".x");
                double y = arenasSection.getDouble(arenaName + ".y");
                double z = arenasSection.getDouble(arenaName + ".z");
                Location arenaLocation = new Location(getServer().getWorld(worldName), x, y, z);
                arenaLocations.put(arenaName, arenaLocation);
            }
        }

        // Carica i nomi dei team da bedwars.yml
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String teamName : teamsSection.getKeys(false)) {
                Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                    teams.put(teamName, team);
                }
            }
        }

        // Carica le posizioni dei letti da bedwars.yml
        ConfigurationSection bedsSection = config.getConfigurationSection("beds");
        if (bedsSection != null) {
            for (String bedName : bedsSection.getKeys(false)) {
                String worldName = bedsSection.getString(bedName + ".world");
                double x = bedsSection.getDouble(bedName + ".x");
                double y = bedsSection.getDouble(bedName + ".y");
                double z = bedsSection.getDouble(bedName + ".z");
                Location bedLocation = new Location(getServer().getWorld(worldName), x, y, z);
                bedLocations.put(bedName, bedLocation);
            }
        }
    }

    private void saveBedWarsConfig() {
        // Salva le posizioni delle arene in bedwars.yml
        ConfigurationSection arenasSection = config.createSection("arenas");
        for (String arenaName : arenaLocations.keySet()) {
            Location arenaLocation = arenaLocations.get(arenaName);
            arenasSection.set(arenaName + ".world", arenaLocation.getWorld().getName());
            arenasSection.set(arenaName + ".x", arenaLocation.getX());
            arenasSection.set(arenaName + ".y", arenaLocation.getY());
            arenasSection.set(arenaName + ".z", arenaLocation.getZ());
        }

        // Salva i nomi dei team in bedwars.yml
        ConfigurationSection teamsSection = config.createSection("teams");
        for (String teamName : teams.keySet()) {
            teamsSection.set(teamName, true);
        }

        // Salva le posizioni dei letti in bedwars.yml
        ConfigurationSection bedsSection = config.createSection("beds");
        for (String bedName : bedLocations.keySet()) {
            Location bedLocation = bedLocations.get(bedName);
            bedsSection.set(bedName + ".world", bedLocation.getWorld().getName());
            bedsSection.set(bedName + ".x", bedLocation.getX());
            bedsSection.set(bedName + ".y", bedLocation.getY());
            bedsSection.set(bedName + ".z", bedLocation.getZ());
        }

        // Salva la configurazione in bedwars.yml
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void createPrivateGame(Player creator) {
        privateGamePlayers.clear();
        privateGamePlayers.add(creator);
        privateGameRunning = false;

        // Aggiungi altre logiche per la creazione di partite private
    }

    private void joinPrivateGame(Player player) {
        if (privateGameRunning) {
            player.sendMessage("La partita privata è già in corso.");
            return;
        }

        if (privateGamePlayers.contains(player)) {
            player.sendMessage("Sei già nella partita privata.");
            return;
        }

        privateGamePlayers.add(player);
        player.sendMessage("Sei stato aggiunto alla partita privata di BedWars.");
    }

    private void startPrivateGame(Player initiator) {
        if (!privateGameRunning && privateGamePlayers.contains(initiator)) {
            privateGameRunning = true;

            // Aggiungi qui la logica specifica per avviare il round privato

            for (Player player : privateGamePlayers) {
                // Teletrasporta i giocatori alla posizione di inizio
                player.sendMessage("Il round privato di BedWars è iniziato!");
            }
        }
    }
    
    private void incrementPlayerLevels() {
        for (String playerName : playerLevels.keySet()) {
            int currentLevel = playerLevels.get(playerName);
            playerLevels.put(playerName, currentLevel + 1);
        }
    }
    
    private void sendAdvertisement1() {
        for (Player player : Bukkit.getOnlinePlayers()) {
        	player.sendMessage(ChatColor.DARK_PURPLE +  "§lDrayFire" + ChatColor.LIGHT_PURPLE + "§lFIRE " + ChatColor.GRAY + " >>" + ChatColor.WHITE + " Entra ora nel nostro DISCORD" + ChatColor.YELLOW + " https://discord.gg/kNMu73qEhk");
        	
        	
            // Puoi personalizzare il messaggio e il colore come preferisci
        }
    }
    
    private void sendAdvertisement2() {
        for (Player player : Bukkit.getOnlinePlayers()) {
        	
        	player.sendMessage(ChatColor.DARK_PURPLE + "§lDrayFire" + ChatColor.LIGHT_PURPLE + "§lFIRE " + ChatColor.GRAY + " >>" + ChatColor.WHITE + " Segui tutte le novità su" + ChatColor.AQUA + " Telegram" + ChatColor.YELLOW + " @AstroFireNetwork!");

        	
            // Puoi personalizzare il messaggio e il colore come preferisci
        }
    }
    private void sendAdvertisement3() {
        for (Player player : Bukkit.getOnlinePlayers()) {
        	
        	player.sendMessage(ChatColor.DARK_PURPLE + "§lASTRO" + ChatColor.LIGHT_PURPLE + "§lFIRE" + ChatColor.GRAY + " >>" + ChatColor.YELLOW + " Vuoi reportare un player?" + ChatColor.WHITE + " Utilizza" + ChatColor.AQUA + " /report [nome] [motivo]");
 

        	
            // Puoi personalizzare il messaggio e il colore come preferisci
        }
    }
    private void sendAdvertisement4() {
        for (Player player : Bukkit.getOnlinePlayers()) {
        	
        	player.sendMessage(ChatColor.DARK_PURPLE + "§lASTRO" + ChatColor.LIGHT_PURPLE + "§lFIRE" + ChatColor.GRAY + " >>" + ChatColor.WHITE + " Supporta il tuo" + ChatColor.AQUA + " server preferito" + ChatColor.WHITE + " acquistando un rank su" + ChatColor.YELLOW + " astrofire.craftingstore.net/");

        	
            // Puoi personalizzare il messaggio e il colore come preferisci
        }
    }
    private void sendAdvertisement5() {
        for (Player player : Bukkit.getOnlinePlayers()) {
        	
        	player.sendMessage(ChatColor.DARK_PURPLE + "§lASTRO" + ChatColor.LIGHT_PURPLE + "§lFIRE" + ChatColor.GRAY + " >>" + ChatColor.YELLOW + " Acquista un VIP" + ChatColor.WHITE +" e prova le nostre fantastiche" + ChatColor.AQUA + " BWPrivate!");

        	
            // Puoi personalizzare il messaggio e il colore come preferisci
        }
    }
 
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Lista dei comandi BedWars:");
        sender.sendMessage(ChatColor.RED +"/bedwars start - Avvia una partita BedWars");
        sender.sendMessage(ChatColor.RED +"/bedwars set arena <nomeArena> - Imposta l'arena di gioco");
        sender.sendMessage(ChatColor.RED +"/bedwars set team <nomeTeam> - Crea un nuovo team");
        sender.sendMessage(ChatColor.RED +"/bedwars set bed <nomeTeam> - Imposta la posizione del letto per un team");
        sender.sendMessage(ChatColor.RED +"/bedwars shop - Apre il negozio BedWars");
        sender.sendMessage(ChatColor.RED +"/bedwars join private <nomePartitaPrivata> - Entra in una partita privata");
        sender.sendMessage(ChatColor.GREEN + "Lista dei comandi per VIP:");
        sender.sendMessage(ChatColor.GREEN +"/bedwars private create <nomePartitaPrivata> - Crea una partita privata");
        // Aggiungi altri comandi e descrizioni secondo le tue esigenze
    }
    private void createGenerator(Player player, String generatorType, Map<String, Location> generatorMap) {
        // Ottieni la posizione del giocatore
        Location generatorLocation = player.getLocation();

        // Salva la posizione del generatore nella mappa
        generatorMap.put(generatorType, generatorLocation);

        // Puoi aggiungere ulteriori logiche o salvare questa posizione a seconda delle tue esigenze.

        player.sendMessage("Generatore " + generatorType + " creato con successo!");
        saveGeneratorData(); // Salva i dati del generatore nel file (implementa questo metodo)
    }
    private void saveGeneratorData() {
        // Puoi implementare la logica per salvare i dati dei generatori in un file (ad esempio, un file YAML)
        // Esempio di come potrebbe apparire il salvataggio dei dati dei generatori in un file:
        // ConfigurazioneSection generatorsSection = config.createSection("generators");
        // ... aggiungi i dati dei generatori alla sezione ...
        // Salva la configurazione nel file
    }
    private void createGenerator(Player player, Material material) {
        Location playerLocation = player.getLocation();
        Generator generator = new Generator(material, playerLocation);
        generators.add(generator);

        // Inizia il task di spawn
        startSpawnTask(generator);
    }

    private void startSpawnTask(Generator generator) {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Location generatorLocation = generator.getLocation();
            generatorLocation.getWorld().dropItemNaturally(generatorLocation, new ItemStack(generator.getMaterial()));
        }, 0L, 20L); // Spawn ogni secondo
    }
    
    private void startPrivateGameCountdown(Player initiator) {
        privateGameCountdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (privateGameCountdown > 0) {
                Bukkit.broadcastMessage(ChatColor.BLUE + "La BedWars privata inizierà tra " + privateGameCountdown + " secondi!");
                privateGameCountdown--;
            } else {
                Bukkit.broadcastMessage(ChatColor.BLUE + "Il round privato di BedWars è iniziato!");
                initiatePrivateGame(initiator);
                Bukkit.getScheduler().cancelTask(privateGameCountdownTask);
            }
        }, 0L, 20L);
    }

    private void initiatePrivateGame(Player initiator) {
        // Implementa qui la logica specifica per avviare il round privato

        for (Player player : privateGamePlayers) {
            // Teletrasporta i giocatori alla posizione di inizio
            player.sendMessage("Il round privato di BedWars è iniziato!");
        }
    }

 
    private void saveShopConfig() {
        File shopFile = new File(getDataFolder(), "shop.yml");
        FileConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        ConfigurationSection shopSection = shopConfig.createSection("shop");
        for (String key : shopConfig.getConfigurationSection("shop").getKeys(false)) {
            shopSection.set(key, shopConfig.get("shop." + key));
        }

        try {
            shopConfig.save(shopFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Salvataggio finale
    	saveBedWarsConfig();
        saveShopConfig();
 List<String> s = getConfig().getStringList("scores");
        
        for (OfflinePlayer p : scores.keySet()) {
            s.add(p.getName() + ":" + scores.get(p).getScore());
        }
        
        getConfig().set("scores", s);
        saveConfig();
    }
    
    @SuppressWarnings("deprecation")
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        
        p.setScoreboard(scoreboard);
        
        if (scores.get(p) == null) scores.put(p, o.getScore(Bukkit.getServer().getOfflinePlayer(ChatColor.GREEN + "Number:")));
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockY() == e.getTo().getBlockY() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        
        scores.get(e.getPlayer()).setScore(scores.get(e.getPlayer()).getScore() + 1);
    }
    }


