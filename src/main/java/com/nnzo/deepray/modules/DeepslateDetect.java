package com.nnzo.deepray.modules;

import com.nnzo.deepray.Deepray;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;

public class DeepslateDetect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender  = settings.createGroup("Render");

    private final Setting<Direction.Axis> expectedAxis = sgGeneral.add(new EnumSetting.Builder<Direction.Axis>()
        .name("expected-axis")
        .description("Which axis (X/Y/Z) is correct for deepslate pillars.")
        .defaultValue(Direction.Axis.X)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Highlight color.")
        .defaultValue(new SettingColor(Color.MAGENTA))
        .build()
    );

    // store positions that fail the orientation test
    private final List<BlockPos> invalidBlocks = new ArrayList<>();

    public DeepslateDetect() {
        super(Deepray.CATEGORY, "deepslate-detect", "Highlight mis-oriented deepslate blocks on chunk load or update.");
    }

    /** Called whenever a full chunk’s data arrives. */
    @EventHandler
    private void onChunkData(PacketEvent.Receive event) {
        if (!(event.packet instanceof ChunkDataS2CPacket pkt)) return;

        int cx = pkt.getX();
        int cz = pkt.getZ();
        // loop entire chunk column
        int bottomY = mc.world.getBottomY();
        int topY    = mc.world.getHeight();
        int startX  = cx << 4;
        int startZ  = cz << 4;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                for (int y = bottomY; y < topY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    checkPos(pos);
                }
            }
        }
    }

    /** Called whenever a single block changes in the world. */
    @EventHandler
    private void onBlockUpdate(PacketEvent.Receive event) {
        if (!(event.packet instanceof BlockUpdateS2CPacket pkt)) return;
        BlockPos pos = pkt.getPos();
        checkPos(pos);
    }

    /** Inspect one block, and add it to invalidBlocks if it’s deepslate + wrong axis. */
    private void checkPos(BlockPos pos) {
        if (mc.world == null) return;
        BlockState state = mc.world.getBlockState(pos);

        // only raw deepslate
        if (state.getBlock() != Blocks.DEEPSLATE) return;

        // must have an AXIS property (e.g. if you really have a pillar‐type deepslate)
        if (!state.contains(PillarBlock.AXIS)) return;

        // if it’s mis‐oriented, mark it
        if (state.get(PillarBlock.AXIS) != expectedAxis.get()) {
            invalidBlocks.add(pos);
        }
    }

    /** Your existing render pass to draw boxes around each bad block. */
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        for (BlockPos pos : invalidBlocks) {
            Box box = new Box(pos);
            event.renderer.box(box, color.get(), color.get(), ShapeMode.Both, 0);
        }
        // clear after drawing so we don’t keep old ones
        invalidBlocks.clear();
    }
}
