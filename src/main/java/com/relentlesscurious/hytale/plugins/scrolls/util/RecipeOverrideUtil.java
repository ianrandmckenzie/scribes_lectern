package com.relentlesscurious.hytale.plugins.scrolls.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.relentlesscurious.hytale.plugins.scrolls.config.BaseScrollConfig;
import com.relentlesscurious.hytale.plugins.scrolls.config.RecipeIngredientConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility to override item recipes at runtime using reflection,
 * following the pattern established in Tinkers' Bench.
 */
public class RecipeOverrideUtil {

  public static void applyRecipe(String itemId, BaseScrollConfig config, HytaleLogger logger) {
    if (config == null || config.recipe == null || config.recipe.isEmpty()) {
      logger.atInfo().log("Skipping recipe override for %s: config is null or empty.", itemId);
      return;
    }

    logger.atInfo().log("Attempting to apply recipe override for %s. Configured ingredients: %d", itemId, config.recipe.size());

    Item item = Item.getAssetMap().getAsset(itemId);
    if (item == null) {
      logger.atWarning().log("Could not find Item asset for %s to apply recipe overrides. Trying alternative patterns...", itemId);
      
      // Try alternatives if the namespaced path fails
      String shortName = itemId.contains("/") ? itemId.substring(itemId.lastIndexOf("/") + 1) : itemId;
      if (shortName.contains(":")) shortName = shortName.substring(shortName.lastIndexOf(":") + 1);
      
      String[] alternatives = {
          shortName,
          "scribes:" + shortName,
          "scribes:lectern:" + shortName,
          "scribes:Scrolls/" + shortName,
          "scribes:lectern:Scrolls/" + shortName,
          shortName.toLowerCase(),
          "scribes:" + shortName.toLowerCase()
      };
      
      for (String alt : alternatives) {
          item = Item.getAssetMap().getAsset(alt);
          if (item != null) {
              logger.atInfo().log("Successfully found item using alternative ID: %s (Original: %s)", alt, itemId);
              itemId = alt;
              break;
          }
      }
    }

    if (item == null) {
      logger.atWarning().log("Still could not find Item asset for %s after trying alternatives.", itemId);
      return;
    }

    try {
      // Use reflection to get 'recipeToGenerate' from Item
      Field recipeField = null;
      try {
        recipeField = Item.class.getDeclaredField("recipeToGenerate");
      } catch (NoSuchFieldException e) {
          try {
              recipeField = Item.class.getDeclaredField("RecipeToGenerate");
          } catch (NoSuchFieldException e2) {
              logger.atWarning().log("Field 'recipeToGenerate' or 'RecipeToGenerate' not found in Item class. Available fields: %s",
                  java.util.Arrays.toString(java.util.Arrays.stream(Item.class.getDeclaredFields()).map(Field::getName).toArray()));
              return;
          }
      }
      recipeField.setAccessible(true);
      CraftingRecipe recipe = (CraftingRecipe) recipeField.get(item);

      if (recipe == null) {
        logger.atWarning().log("Item %s has no existing recipe to override. Creating one is not yet implemented.", itemId);
        return;
      }

      // Create new Ingredient Array
      List<MaterialQuantity> newIngredients = new ArrayList<>();
      for (RecipeIngredientConfig ingredientConfig : config.recipe) {
        if (ingredientConfig.itemId == null || ingredientConfig.itemId.isEmpty()) {
          logger.atWarning().log("Ingredient for %s has null or empty itemId. Skipping.", itemId);
          continue;
        }

        logger.atInfo().log("Adding ingredient: %s x%d", ingredientConfig.itemId, ingredientConfig.quantity);

        // Constructor: MaterialQuantity(itemId, resourceTypeId, tag, quantity, metadata)
        MaterialQuantity ingredient = new MaterialQuantity(ingredientConfig.itemId, null, null, ingredientConfig.quantity, null);
        newIngredients.add(ingredient);
      }

      // Use reflection to set 'input' on CraftingRecipe
      Field inputField = null;
      try {
        inputField = CraftingRecipe.class.getDeclaredField("input");
      } catch (NoSuchFieldException e) {
          try {
              inputField = CraftingRecipe.class.getDeclaredField("Input");
          } catch (NoSuchFieldException e2) {
              logger.atWarning().log("Field 'input' or 'Input' not found in CraftingRecipe class. Available fields: %s",
                  java.util.Arrays.toString(java.util.Arrays.stream(CraftingRecipe.class.getDeclaredFields()).map(Field::getName).toArray()));
              return;
          }
      }
      inputField.setAccessible(true);

      MaterialQuantity[] ingredientsArray = newIngredients.toArray(new MaterialQuantity[0]);
      inputField.set(recipe, ingredientsArray);

      logger.atInfo().log("Successfully applied recipe overrides for %s (ingredients: %d)", itemId, newIngredients.size());

    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to apply recipe override for %s", itemId);
    }
  }

  public static void disableCrafting(String itemId, HytaleLogger logger) {
    Item item = Item.getAssetMap().getAsset(itemId);
    if (item == null) return;

    try {
      Field recipeField = Item.class.getDeclaredField("recipeToGenerate");
      recipeField.setAccessible(true);
      recipeField.set(item, null);
      logger.atInfo().log("Disabled crafting recipe for %s", itemId);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to disable crafting for %s", itemId);
    }
  }
}
