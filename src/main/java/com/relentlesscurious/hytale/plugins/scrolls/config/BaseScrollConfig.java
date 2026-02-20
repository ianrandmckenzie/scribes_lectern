package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class BaseScrollConfig {
  @SerializedName("craftingEnabled")
  public boolean craftingEnabled = true;

  @SerializedName("chargingTime")
  public double chargingTime = 1.5;

  @SerializedName("recipe")
  public List<RecipeIngredientConfig> recipe = new ArrayList<>();
}
