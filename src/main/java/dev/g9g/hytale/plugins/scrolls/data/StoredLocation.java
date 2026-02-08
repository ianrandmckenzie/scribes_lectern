package com.relentlesscurious.hytale.plugins.scrolls.data;

public class StoredLocation {
  private String worldName;
  private double x;
  private double y;
  private double z;
  private float yaw;
  private float pitch;

  public StoredLocation() {
  }

  public StoredLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
    this.worldName = worldName;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  public String getWorldName() {
    return worldName;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public float getYaw() {
    return yaw;
  }

  public float getPitch() {
    return pitch;
  }

  @Override
  public String toString() {
    return String.format("%s (%.2f, %.2f, %.2f)", worldName, x, y, z);
  }
}
