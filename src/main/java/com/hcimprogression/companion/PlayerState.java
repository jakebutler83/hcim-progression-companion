package com.hcimprogression.companion;

public class PlayerState
{
    private String playerName;
    private int world;
    private int regionId;
    private int x;
    private int y;
    private int plane;
    private long timestamp;

    public String getPlayerName()
    {
        return playerName;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }

    public int getWorld()
    {
        return world;
    }

    public void setWorld(int world)
    {
        this.world = world;
    }

    public int getRegionId()
    {
        return regionId;
    }

    public void setRegionId(int regionId)
    {
        this.regionId = regionId;
    }

    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY()
    {
        return y;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public int getPlane()
    {
        return plane;
    }

    public void setPlane(int plane)
    {
        this.plane = plane;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}