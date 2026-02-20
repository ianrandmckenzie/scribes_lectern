package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.google.gson.annotations.SerializedName;

public class RecipeIngredientConfig {
  @SerializedName("itemId")
  public String itemId;

  @SerializedName("quantity")
  public int quantity;

  public RecipeIngredientConfig() {}

  public RecipeIngredientConfig(String itemId, int quantity) {
    this.itemId = itemId;
    this.quantity = quantity;
  }
}
