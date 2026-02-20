package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.DoubleCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class RandomTeleportConfig extends BaseScrollConfig {
  public double minRange = 50.0;
  public double maxRange = 1500.0;
  public String shape = "circle";
  public int maxAttempts = 12;
  public int minSurfaceY = 40;

  public RandomTeleportConfig() {
    this.recipe.add(new RecipeIngredientConfig("Ingredient_Fibre", 4));
    this.recipe.add(new RecipeIngredientConfig("Ingredient_Stick", 1));
    this.recipe.add(new RecipeIngredientConfig("Ingredient_Tree_Sap", 1));
  }
}
