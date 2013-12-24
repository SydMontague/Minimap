package de.craftlancer.serverminimap;

import org.bukkit.map.MapCursor;

/**
 * Represents a cursor, that will be shown when it is in the players minimap
 * range.
 */
public class ExtraCursor
{
    private int x;
    private int z;
    private boolean visible;
    private MapCursor.Type type;
    private byte direction;
    
    /**
     * Construct a new ExtraCursor object.
     * 
     * @param x - the absolute x coordinates of the cursor
     * @param z - the absolute z coordiantes of the cursor
     * @param visible - whether the cursor is visible or not
     * @param type - the type/shape of the cursor
     * @param direction - the direction the cursor is pointing at
     */
    public ExtraCursor(int x, int z, boolean visible, MapCursor.Type type, byte direction)
    {
        setX(x);
        setZ(z);
        setVisible(visible);
        setType(type);
        setDirection(direction);
    }
    
    /**
     * Get the x coordinate of the marker.
     * 
     * @return the x coordinate using the MC coordinate system.
     */
    public int getX()
    {
        return x;
    }
    
    /**
     * Set the x coordinate of the marker.
     * 
     * @param x
     *            the x coordinate in the MC coordinate system.
     */
    public void setX(int x)
    {
        this.x = x;
    }
    
    /**
     * Get the z coordinate of the marker.
     * 
     * @return the z coordinate using the MC coordinate system.
     */
    public int getZ()
    {
        return z;
    }
    
    /**
     * Set the z coordinate of the marker.
     * 
     * @param z
     *            the z coordinate in the MC coordinate system.
     */
    public void setZ(int z)
    {
        this.z = z;
    }
    
    /**
     * Get if the marker is visible.
     * 
     * @return true if the marker is visible, false if not
     */
    public boolean isVisible()
    {
        return visible;
    }
    
    /**
     * Set the visibility of the marker.
     * 
     * @param visible
     *            true for a visible marker, false for invisible
     */
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }
    
    /**
     * Get the shape of the cursor.
     * See MapCursor.Type for available types.
     * 
     * @return the shape type of the cursor
     */
    public MapCursor.Type getType()
    {
        return type;
    }
    
    /**
     * Set the shape of the cursor.
     * See MapCursor.Type for available types.
     * 
     * @param type
     *            the type of the cursor
     */
    public void setType(MapCursor.Type type)
    {
        this.type = type;
    }
    
    /**
     * Get the direction the cursor is looking.
     * There are 16 possible directions where between them lays a difference of
     * 22,5 degree.
     * 
     * @return the byte value of the direction, between 0 and 15
     */
    public byte getDirection()
    {
        return direction;
    }
    
    /**
     * Set the direction in which the cursor is looking.
     * Values values are between 0 and 15. 0 is pointing north, between 2 values
     * are 22,5 degree rotation.
     * 
     * @param direction
     *            s byte value. It makes sure that the direction value is always
     *            between 0 and 15 by calculating the input with mod 16.
     */
    public void setDirection(byte direction)
    {
        this.direction = (byte) (direction % 16);
    }
    
}
