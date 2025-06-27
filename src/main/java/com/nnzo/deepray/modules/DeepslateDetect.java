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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.world.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

public class DeepslateDetect extends Module {

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");

    private final List<BlockPos> invalidBlocks = new ObjectArrayList<>();
    private int tickTimer = 0;
    private final int TICK_INTERVAL = 10; // adjust for performance vs responsiveness

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

    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public DeepslateDetect() {
        super(Deepray.CATEGORY, "deepslate-detect", "Detect mis-orientated deepslate blocks");
    }

    @EventHandler
    private void onChunkLoad(PacketEvent.Receive event) {
        if (!(event.packet instanceof ChunkDataS2CPacket packet) || mc.world == null) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ()); // ✅ use getters

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < mc.world.getHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
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

    @EventHandler
    private void onGameJoin(PacketEvent.Receive event) {
        if (event.packet instanceof GameJoinS2CPacket) {
            invalidBlocks.clear();
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
