package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.google.gson.annotations.SerializedName;

public class MainConfig {
    @SerializedName("spawnScroll")
    public SpawnScrollConfig spawnScroll = new SpawnScrollConfig();

    @SerializedName("homeScroll")
    public HomeScrollConfig homeScroll = new HomeScrollConfig();

    @SerializedName("homeBindingScroll")
    public HomeBindingScrollConfig homeBindingScroll = new HomeBindingScrollConfig();

    @SerializedName("randomTeleportScroll")
    public RandomTeleportConfig randomTeleportScroll = new RandomTeleportConfig();

    @SerializedName("friendTeleportScroll")
    public FriendTeleportConfig friendTeleportScroll = new FriendTeleportConfig();
}

