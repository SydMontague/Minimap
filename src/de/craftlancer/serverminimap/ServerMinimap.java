package de.craftlancer.serverminimap;

import java.io.File;
import java.io.IOException;

import net.minecraft.server.v1_7_R1.MaterialMapColor;

import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_7_R1.util.CraftMagicNumbers;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import de.craftlancer.serverminimap.metrics.Metrics;
import de.craftlancer.serverminimap.metrics.Metrics.Graph;

/**
 * The plugin is based on a request/idea by toxictroop and by the team at:
 * http://www.crossfiregaming.net/
 * http://forums.bukkit.org/threads/dynamic-maps.146024/
 * 
 * @author Syd
 */
public class ServerMinimap extends JavaPlugin
{
    public static short MAPID = 0;
    private FileConfiguration config;
    private int SCALE = 0;
    private int CPR = 8;
    private int runPerTicks = 1;
    private int fastTicks = 20;
    private boolean canSeeOthers;
    
    @Override
    public void onEnable()
    {
        loadConfig();
        loadMap();
        
        try
        {
            Metrics metrics = new Metrics(this);
            
            Graph scaleGraph = metrics.createGraph("Scale");
            scaleGraph.addPlotter(new Metrics.Plotter(String.valueOf(SCALE))
            {
                
                @Override
                public int getValue()
                {
                    return 1;
                }
            });
            
            Graph cprGraph = metrics.createGraph("Chunks per Run");
            cprGraph.addPlotter(new Metrics.Plotter(String.valueOf(CPR))
            {
                @Override
                public int getValue()
                {
                    return 1;
                }
            });
            
            Graph rptGraph = metrics.createGraph("Render per Ticks");
            rptGraph.addPlotter(new Metrics.Plotter(String.valueOf(runPerTicks))
            {
                @Override
                public int getValue()
                {
                    return 1;
                }
            });
            
            metrics.start();
        }
        catch (IOException e)
        {
        }
    }
    
    @Override
    public void onDisable()
    {
        getServer().getScheduler().cancelTasks(this);
        config.set("mapID", MAPID);
        saveConfig();
    }
    
    private void loadConfig()
    {
        if (!new File(getDataFolder().getPath() + File.separatorChar + "config.yml").exists())
            saveDefaultConfig();
        
        config = getConfig();
        
        SCALE = config.getInt("scale", 0);
        CPR = config.getInt("chunksPerRun", 4);
        runPerTicks = config.getInt("runPerTicks", 1);
        fastTicks = config.getInt("fastTicks", 20);
        canSeeOthers = config.getBoolean("canSeeOthers", true);
        MAPID = (short) config.getInt("mapID", 0);
    }
    
    public int getRunPerTicks()
    {
        return runPerTicks;
    }
    
    public int getFastTicks()
    {
        return fastTicks;
    }
    
    public boolean canSeeOthers()
    {
        return canSeeOthers;
    }
    
    @SuppressWarnings("deprecation")
    private void loadMap()
    {
        MapView map = getServer().getMap(MAPID);
        if (map == null)
            map = getServer().createMap(getServer().getWorlds().get(0));
        
        MAPID = map.getId();
        
        if (!(map.getRenderers().get(0) instanceof MinimapRenderer))
        {
            for (MapRenderer r : map.getRenderers())
                map.removeRenderer(r);
            
            map.addRenderer(new MinimapRenderer(SCALE, CPR, getServer().getWorlds().get(0), this));
        }
        
        getLogger().info("Created Minimap with ID " + MAPID + ". Use /give <name> MAP 1 " + MAPID + " to get the map as item.");
    }
    
    @SuppressWarnings("deprecation")
    public MaterialMapColor getColor(Block block)
    {
        return CraftMagicNumbers.getBlock(block).f(block.getData());
    }
}
