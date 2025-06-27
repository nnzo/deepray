package com.nnzo.deepray.modules;

import com.nnzo.deepray.Deepray;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Set;

public class DeepslateDetect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender  = settings.createGroup("Render");

    private final Setting<Direction.Axis> expectedAxis = sgGeneral.add(new EnumSetting.Builder<Direction.Axis>()
        .name("expected-axis")
        .description("Which axis (X/Y/Z) is correct for deepslate pillars.")
        .defaultValue(Direction.Axis.Y)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the highlight is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Integer> fillOpacity = sgRender.add(new IntSetting.Builder()
        .name("fill-opacity")
        .description("The opacity of the shape fill.")
        .defaultValue(50)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    public final Setting<Integer> outlineWidth = sgRender.add(new IntSetting.Builder()
        .name("outline-width")
        .description("The width of the outline.")
        .defaultValue(1)
        .range(1, 10)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(new ColorSetting.Builder()
        .name("box-color")
        .description("Fill color.")
        .defaultValue(new SettingColor(255, 0, 255, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(255, 0, 255, 255))
        .build()
    );

    // cache of all mis-oriented deepslate positions
    private final Set<BlockPos> invalidBlocks = new ObjectOpenHashSet<>();

    public DeepslateDetect() {
        super(Deepray.CATEGORY, "deepslate-esp", "Highlights deepslate pillars mis-oriented on axis.");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        invalidBlocks.clear();
        if (mc.world == null || mc.player == null) return;

        int vd = mc.options.getViewDistance().getValue();
        int pcx = mc.player.getBlockPos().getX() >> 4;
        int pcz = mc.player.getBlockPos().getZ() >> 4;

        for (int dx = -vd; dx <= vd; dx++) {
            for (int dz = -vd; dz <= vd; dz++) {
                Chunk chunk = mc.world.getChunk(pcx + dx, pcz + dz, ChunkStatus.FULL, false);
                if (chunk != null) scanChunk(pcx + dx, pcz + dz);
            }
        }
    }

    @EventHandler
    private void onWorldChange(PacketEvent.Receive event) {
        if (event.packet instanceof GameJoinS2CPacket || event.packet instanceof PlayerRespawnS2CPacket) {
            invalidBlocks.clear();
            onActivate();
        }
    }

    @EventHandler
    private void onChunkData(PacketEvent.Receive event) {
        if (event.packet instanceof ChunkDataS2CPacket pkt && mc.world != null) {
            scanChunk(pkt.getChunkX(), pkt.getChunkZ());
        }
    }

    @EventHandler
    private void onBlockUpdate(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket pkt) {
            checkPos(pkt.getPos());
        }
    }

    private void scanChunk(int cx, int cz) {
        int startX  = cx << 4;
        int startZ  = cz << 4;
        int bottomY = mc.world.getBottomY();
        int topY    = Math.min(mc.world.getHeight(), 0); // deepslate only below y=0

        invalidBlocks.removeIf(pos -> (pos.getX() >> 4) == cx && (pos.getZ() >> 4) == cz);

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                for (int y = bottomY; y < topY; y++) {
                    checkPos(new BlockPos(startX + dx, y, startZ + dz));
                }
            }
        }
    }

    private void checkPos(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);

        if (state.getBlock() == Blocks.DEEPSLATE && state.contains(Properties.AXIS)) {
            if (state.get(Properties.AXIS) != expectedAxis.get()) {
                invalidBlocks.add(pos.toImmutable());
                return;
            }
        }

        invalidBlocks.remove(pos);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Cache")).expandX().widget();
        clear.action = invalidBlocks::clear;
        return list;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BlockPos pos : invalidBlocks) {
            Box box = new Box(pos);
            event.renderer.box(
                box,
                boxColor.get(),
                lineColor.get(),
                shapeMode.get(),
                outlineWidth.get()
            );
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(invalidBlocks.size());
    }
}
