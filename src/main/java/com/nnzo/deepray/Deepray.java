package com.nnzo.deepray;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.nnzo.deepray.modules.DeepslateDetect;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Deepray extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Deepray");
    public static final HudGroup HUD_GROUP = new HudGroup("Deepray");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Deepray");

        // Modules
        Modules.get().add(new DeepslateDetect());

        // Commands
        // Commands.add(new CommandExample());

        // HUD
        // Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.nnzo.deepray";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
