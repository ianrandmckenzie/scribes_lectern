package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.google.gson.annotations.SerializedName;

public class RandomTeleportLogicConfig {
  @SerializedName("min_range")
  public double minRange = 50.0;

  @SerializedName("max_range")
  public double maxRange = 1500.0;

  @SerializedName("shape")
  public String shape = "circle";

  @SerializedName("max_attempts")
  public int maxAttempts = 12;

  @SerializedName("min_surface_y")
  public int minSurfaceY = 40;
}
