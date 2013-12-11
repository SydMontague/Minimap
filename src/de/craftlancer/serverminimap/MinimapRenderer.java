package de.craftlancer.serverminimap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

import net.minecraft.server.v1_7_R1.MaterialMapColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import de.craftlancer.serverminimap.event.MinimapPlayerCursorEvent;

public class MinimapRenderer extends MapRenderer implements Listener
{
    private Map<Integer, Map<Integer, MapChunk>> cacheMap = new TreeMap<Integer, Map<Integer, MapChunk>>();
    protected Queue<Coords> queue = new LinkedList<Coords>();
    private RenderTask cacheTask = new RenderTask(this);
    private SendTask sendTask = new SendTask();
    
    private int scale = 0;
    private int cpr = 0;
    private int colorlimit;
    private ServerMinimap plugin;
    private World world;
    
    public MinimapRenderer(int scale, int cpr, World world, ServerMinimap plugin)
    {
        super(true);
        
        this.plugin = plugin;
        this.cpr = cpr;
        this.world = world;
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        this.scale = (scale < 1 || scale > 4) ? 1 : (int) Math.pow(2, scale);
        colorlimit = (this.scale * this.scale) / 2;
        
        cacheTask.runTaskTimer(plugin, plugin.getRunPerTicks(), plugin.getRunPerTicks());
        sendTask.runTaskTimer(plugin, plugin.getFastTicks(), plugin.getFastTicks());
    }
    
    public int getScale()
    {
        return scale;
    }
    
    public int getChunksPerRun()
    {
        return cpr;
    }
    
    public Queue<Coords> getQueue()
    {
        return queue;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void render(MapView map, MapCanvas canvas, Player player)
    {
        if (!player.getWorld().equals(world) || !(player.getItemInHand().getType() == Material.MAP && player.getItemInHand().getDurability() == ServerMinimap.MAPID))
            return;
        
        int locX = player.getLocation().getBlockX() / scale - 64;
        int locZ = player.getLocation().getBlockZ() / scale - 64;
        
        for (int i = 0; i < 128; i++)
            for (int j = 0; j < 128; j++)
            {
                int x = (int) ((locX + i) / 16D);
                if (locX + i < 0 && (locX + i) % 16 != 0)
                    x--;
                int z = (int) ((locZ + j) / 16D);
                if (locZ + j < 0 && (locZ + j) % 16 != 0)
                    z--;
                
                if (cacheMap.get(x) != null && cacheMap.get(x).get(z) != null)
                {
                    MaterialMapColor color = cacheMap.get(x).get(z).get(Math.abs((locX + i + 16 * Math.abs(x))) % 16, Math.abs((locZ + j + 16 * Math.abs(z))) % 16);
                    short avgY = cacheMap.get(x).get(z).getY(Math.abs((locX + i + 16 * Math.abs(x))) % 16, Math.abs((locZ + j + 16 * Math.abs(z))) % 16);
                    short prevY = getPrevY(x, z, Math.abs((locX + i + 16 * Math.abs(x))) % 16, Math.abs((locZ + j + 16 * Math.abs(z))) % 16);
                    
                    double d2 = (avgY - prevY) * 4.0D / (scale + 4) + ((i + j & 1) - 0.5D) * 0.4D;
                    byte b0 = 1;
                    
                    if (d2 > 0.6D)
                        b0 = 2;
                    if (d2 < -0.6D)
                        b0 = 0;
                    canvas.setPixel(i, j, (byte) (color.M * 4 + b0));
                    
                }
                else
                {
                    canvas.setPixel(i, j, (byte) 0);
                    if (queue.size() < 200)
                        addToQueue(x, z, true);
                }
            }
        
        MapCursorCollection cursors = canvas.getCursors();
        while (cursors.size() > 0)
            cursors.removeCursor(cursors.getCursor(0));
        
        for (Player p : plugin.getServer().getOnlinePlayers())
        {
            float yaw = p.getLocation().getYaw();
            if (yaw < 0)
                yaw += 360;
            
            byte direction = (byte) ((Math.abs(yaw) + 11.25) / 22.5);
            if (direction > 15)
                direction = 0;
            
            int x = ((p.getLocation().getBlockX() - player.getLocation().getBlockX()) / scale) * 2;
            int z = ((p.getLocation().getBlockZ() - player.getLocation().getBlockZ()) / scale) * 2;
            
            if (Math.abs(x) > 128 || Math.abs(z) > 128)
                continue;
            
            MinimapPlayerCursorEvent e = new MinimapPlayerCursorEvent(player, p, plugin.canSeeOthers());
            plugin.getServer().getPluginManager().callEvent(e);
            
            if (e.isCursorShown())
                cursors.addCursor(x, z, direction, e.getType().getValue());
        }
    }
    
    public void addToQueue(int x, int y, boolean chunk)
    {
        Coords c = new Coords(x, y, chunk);
        if (!queue.contains(c))
            queue.offer(c);
    }
    
    public void loadData(int x, int z)
    {
        if (!cacheMap.containsKey(x))
            cacheMap.put(x, new TreeMap<Integer, MapChunk>());
        
        if (!cacheMap.get(x).containsKey(z))
            cacheMap.get(x).put(z, new MapChunk());
        
        MapChunk map = cacheMap.get(x).get(z);
        
        int initX = x * scale * 16;
        int initZ = z * scale * 16;
        
        for (int i = 0; i < 16; i++)
            for (int j = 0; j < 16; j++)
                map.set(i, j, renderBlock(initX + i * scale, initZ + j * scale, getPrevY(x, z, i, j)));
    }
    
    private short getPrevY(int x, int z, int i, int j)
    {
        j--;
        
        if (j < 0)
        {
            z--;
            j = 15;
        }
        
        if (cacheMap.containsKey(x) && cacheMap.get(x).containsKey(z))
            return cacheMap.get(x).get(z).getY(i, j);
        
        short avgY = 0;
        
        return avgY;
    }
    
    public void loadBlock(int initX, int initZ)
    {
        int locX = initX / scale;
        int locZ = initZ / scale;
        
        int x = (int) (locX / 16D);
        if (locX < 0 && locX % 16 != 0)
            x--;
        int z = (int) (locZ / 16D);
        if (locZ < 0 && locZ % 16 != 0)
            z--;
        
        int sx = Math.abs((locX + 16 * Math.abs(x))) % 16;
        int sz = Math.abs((locZ + 16 * Math.abs(z))) % 16;
        
        if (!cacheMap.containsKey(x))
            return;// cacheMap.put(x, new TreeMap<Integer, MapChunk>());
            
        if (!cacheMap.get(x).containsKey(z))
            return;// cacheMap.get(x).put(z, new MapChunk());
            
        MapChunk map = cacheMap.get(x).get(z);
        map.set(sx, sz, renderBlock((x * 16 + sx) * scale, (z * 16 + sz) * scale, getPrevY(x, z, sx, sz)));
    }
    
    public RenderResult renderBlock(int baseX, int baseZ, short prevY)
    {
        Map<MaterialMapColor, Integer> colors = new HashMap<MaterialMapColor, Integer>();
        short avgY = 0;
        MaterialMapColor mainColor = null;
        
        for (int k = 0; k < scale; k++)
            for (int l = 0; l < scale; l++)
            {
                Block b = world.getBlockAt(baseX + k, world.getHighestBlockYAt(baseX + k, baseZ + l) + 1, baseZ + l);
                
                if (!b.getChunk().isLoaded())
                    b.getChunk().load();
                
                while (b.getY() > 0 && plugin.getColor(b) == MaterialMapColor.b)
                    b = world.getBlockAt(b.getX(), b.getY() - 1, b.getZ());
                
                avgY += b.getY();
                
                if (mainColor == null)
                {
                    MaterialMapColor color = plugin.getColor(b);
                    int value = colors.containsKey(color) ? colors.get(color) + 1 : 1;
                    colors.put(color, value);
                    
                    if (colors.get(color) >= colorlimit)
                        mainColor = color;
                }
            }
        
        avgY /= scale;
        
        if (mainColor == null)
        {
            int max = 0;
            for (Entry<MaterialMapColor, Integer> c : colors.entrySet())
                if (c.getValue() > max)
                {
                    max = c.getValue();
                    mainColor = c.getKey();
                }
        }
        
        // d0 - previous d1
        // d1 - average height
        // i - 1 << scale == (2^scale)
        // k1 - x iterator (map x)
        // j2 - z iterator (map z)
        // i4 - height of liquid divided by i*i
        // double d2 = (d1 - d0) * 4.0D / (double) (i + 4) + ((double) (k1 + j2
        // & 1) - 0.5D) * 0.4D;
        // d2 = (double) i4 * 0.1D + (double) (k1 + j2 & 1) * 0.2D;
        return new RenderResult(mainColor, avgY);
    }
    
    private void handleBlockEvent(Block e)
    {
        Location loc = e.getLocation();
        if (loc.getBlockY() >= loc.getWorld().getHighestBlockYAt(loc) - 1)
            addToQueue(loc.getBlockX(), loc.getBlockZ(), false);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockPlaceEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockFromToEvent e)
    {
        handleBlockEvent(e.getBlock());
        handleBlockEvent(e.getToBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockPhysicsEvent e)
    {
        switch (e.getChangedType())
        {
            case LAVA:
            case WATER:
            case STATIONARY_LAVA:
            case STATIONARY_WATER:
                handleBlockEvent(e.getBlock());
                break;
            default:
                break;
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockBreakEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockBurnEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockFadeEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockFormEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockGrowEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(BlockSpreadEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockEvent(EntityBlockFormEvent e)
    {
        handleBlockEvent(e.getBlock());
    }
}
