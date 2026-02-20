package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HomeBindingScrollConfig extends BaseScrollConfig {

  public HomeBindingScrollConfig() {
    this.recipe.add(new RecipeIngredientConfig("Ingredient_Fibre", 4));
    this.recipe.add(new RecipeIngredientConfig("Ingredient_Stick", 1));
    this.recipe.add(new RecipeIngredientConfig("Ingredient_Tree_Sap", 1));
  }
}
