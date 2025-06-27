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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Set;

public class DeepslateDetect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender  = settings.createGroup("Render");

    private final Setting<Direction.Axis> expectedAxis = sgGeneral.add(
        new EnumSetting.Builder<Direction.Axis>()
            .name("expected-axis")
            .description("Which axis (X/Y/Z) is correct for deepslate.")
            .defaultValue(Direction.Axis.X)
            .build()
    );

    private final Setting<SettingColor> color = sgRender.add(
        new ColorSetting.Builder()
            .name("color")
            .description("Highlight color.")
            .defaultValue(Color.MAGENTA)
            .build()
    );

    // all currently “invalid” deepslate positions
    private final Set<BlockPos> invalidBlocks = new ObjectOpenHashSet<>();

    public DeepslateDetect() {
        super(Deepray.CATEGORY, "deepslate-detect", "Detect mis-oriented raw deepslate on chunk load or block update.");
    }

    /** When a full chunk arrives, scan it once. */
    @EventHandler
    private void onChunkData(PacketEvent.Receive event) {
        if (!(event.packet instanceof ChunkDataS2CPacket pkt) || mc.world == null) return;

        int cx = pkt.getX(), cz = pkt.getZ();
        int startX = cx << 4, startZ = cz << 4;
        int bottomY = mc.world.getBottomY(), topY = mc.world.getHeight();

        // clear out any old entries in this chunk
        invalidBlocks.removeIf(pos -> (pos.getX() >> 4) == cx && (pos.getZ() >> 4) == cz);

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                for (int y = bottomY; y < topY; y++) {
                    checkPos(new BlockPos(startX + dx, y, startZ + dz));
                }
            }
        }
    }

    /** When a single block changes, update that one position. */
    @EventHandler
    private void onBlockUpdate(PacketEvent.Receive event) {
        if (!(event.packet instanceof BlockUpdateS2CPacket pkt)) return;
        checkPos(pkt.getPos());
    }

    /**
     * Add the pos to invalidBlocks if it's deepslate + wrong axis,
     * otherwise remove it if it was previously marked.
     */
    private void checkPos(BlockPos pos) {
        if (mc.world == null) return;
        BlockState state = mc.world.getBlockState(pos);

        if (state.getBlock() == Blocks.DEEPSLATE && state.contains(Properties.AXIS)) {
            if (state.get(Properties.AXIS) != expectedAxis.get()) {
                invalidBlocks.add(pos.toImmutable());
                return;
            }
        }

        invalidBlocks.remove(pos);
    }

    /** Draw a box around every bad block each render pass. */
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        for (BlockPos pos : invalidBlocks) {
            Box box = new Box(pos);
            event.renderer.box(box, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }
}
