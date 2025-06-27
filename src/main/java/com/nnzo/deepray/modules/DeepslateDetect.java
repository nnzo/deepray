package com.nnzo.deepray.modules;

import com.nnzo.deepray.Deepray;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import meteordevelopment.meteorclient.settings.IntSetting;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;

public class DeepslateDetect extends Module {

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");

    private final List<BlockPos> invalidBlocks = new ObjectArrayList<>();
    private int tickTimer = 0;

    /**
     * Example setting. The {@code name} parameter should be in kebab-case. If
     * you want to access the setting from another class, simply make the
     * setting {@code public}, and use
     * {@link meteordevelopment.meteorclient.systems.modules.Modules#get(Class)}
     * to access the {@link Module} object.
     */
    private final Setting<Direction.Axis> expectedAxis = sgGeneral.add(new EnumSetting.Builder<Direction.Axis>()
            .name("expected-orientation")
            .description("The orientation that is considered correct.")
            .defaultValue(Direction.Axis.X)
            .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of the marker.")
            .defaultValue(Color.MAGENTA)
            .build()
    );

    private final Setting<Integer> tickInterval = sgRender.add(
    new IntSetting.Builder()
        .name("tick-interval")                    // kebab-case name
        .description("Amount of ticks before re-checking")  // tooltip
        .defaultValue(10)                        // initial value
        .min(0)                                  // absolute min
        .max(1000)                                // absolute max
        .sliderRange(0, 100)                     // (optional) GUI slider bounds
        .build()
);

    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public DeepslateDetect() {
        super(Deepray.CATEGORY, "deepslate-detect", "Detect mis-orientated deepslate blocks");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        if (++tickTimer >= tickInterval) {
            tickTimer = 0;
            invalidBlocks.clear();

            BlockPos playerPos = mc.player.getBlockPos();
            int radius = mc.options.getViewDistance().getValue() * 16;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (!mc.world.isChunkLoaded(pos)) {
                            continue;
                        }

                        var state = mc.world.getBlockState(pos);
                        if (state.getBlock() == net.minecraft.block.Blocks.DEEPSLATE
                                && state.contains(net.minecraft.state.property.Properties.AXIS)) {
                            var axis = state.get(net.minecraft.state.property.Properties.AXIS);
                            if (axis != expectedAxis.get()) {
                                invalidBlocks.add(pos.toImmutable());
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        for (BlockPos pos : invalidBlocks) {
            Box box = new Box(pos);
            event.renderer.box(box, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }

}