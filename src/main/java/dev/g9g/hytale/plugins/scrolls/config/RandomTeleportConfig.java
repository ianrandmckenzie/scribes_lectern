package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.DoubleCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class RandomTeleportConfig {
  public static final BuilderCodec<RandomTeleportConfig> CODEC = BuilderCodec
      .builder(RandomTeleportConfig.class, RandomTeleportConfig::new)
      .append(
          new KeyedCodec<>("Shape", new StringCodec()),
          (config, value) -> config.shape = value,
          config -> config.shape)
      .add()
      .append(
          new KeyedCodec<>("MinRange", new DoubleCodec()),
          (config, value) -> config.minRange = value,
          config -> config.minRange)
      .add()
      .append(
          new KeyedCodec<>("MaxRange", new DoubleCodec()),
          (config, value) -> config.maxRange = value,
          config -> config.maxRange)
      .add()
      .append(
          new KeyedCodec<>("MaxAttempts", new IntegerCodec()),
          (config, value) -> config.maxAttempts = value,
          config -> config.maxAttempts)
      .add()
      .append(
          new KeyedCodec<>("MinSurfaceY", new IntegerCodec()),
          (config, value) -> config.minSurfaceY = value,
          config -> config.minSurfaceY)
      .add()
      .build();

  public double minRange = 50.0;
  public double maxRange = 1500.0;
  public String shape = "circle";
  public int maxAttempts = 12;
  public int minSurfaceY = 40;
}
