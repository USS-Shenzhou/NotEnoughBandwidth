package cn.ussshenzhou.notenoughbandwidth.stat;

import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * @author USS_Shenzhou
 */
public class StatScreen extends Screen {
    private String client = "Client";
    private String actual = "Actual Transmission";
    private String actual1 = "";
    private String raw = "Raw Payload";
    private String raw1 = "";
    private int tick = 0;

    public StatScreen() {
        super(Component.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (tick % 10 == 0){
            raw1 = "↓ Inbound  "
                    + getReadableSpeed((int) SimpleStat.inboundSpeedRaw.averageIn1s())
                    + "  Total  "
                    + getReadableSize(SimpleStat.inboundBytesRaw.get())
                    + "    ↑ Outbound  "
                    + getReadableSpeed((int) SimpleStat.outboundSpeedRaw.averageIn1s())
                    + "  Total  "
                    + getReadableSize(SimpleStat.outboundBytesRaw.get());
            actual1 = "↓ Inbound  "
                    + getReadableSpeed((int) SimpleStat.inboundSpeedBaked.averageIn1s())
                    + "  Total  "
                    + getReadableSize(SimpleStat.inboundBytesBaked.get())
                    + "    ↑ Outbound  "
                    + getReadableSpeed((int) SimpleStat.outboundSpeedBaked.averageIn1s())
                    + "  Total  "
                    + getReadableSize(SimpleStat.outboundBytesBaked.get());
        }
        tick++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        var textRenderer = graphics.textRenderer();
        var pose = graphics.pose();
        textRenderer.accept(10, 10, Component.literal(client));

        textRenderer.accept(10, 30, Component.literal(actual));

        textRenderer.accept(10, 50, Component.literal(actual1));

        textRenderer.accept(10, 70, Component.literal(raw));

        textRenderer.accept(10, 90, Component.literal(raw1));

    }

    private String getReadableSpeed(int bytes) {
        if (bytes < 1000) {
            return bytes + " §7Bytes/S§r";
        } else if (bytes < 1000 * 1000) {
            return String.format("%.1f §7KiB/S§r", bytes / 1024f);
        } else {
            return String.format("%.2f §7MiB/S§r", bytes / (1024 * 1024f));
        }
    }

    private String getReadableSize(long bytes) {
        if (bytes < 1000) {
            return bytes + " §7Bytes§r";
        } else if (bytes < 1000 * 1000) {
            return String.format("%.1f §7KiB§r", bytes / 1024d);
        } else if (bytes < 1000 * 1000 * 1000) {
            return String.format("%.2f §7MiB§r", bytes / (1024 * 1024d));
        } else {
            return String.format("%.2f §7GiB§r", bytes / (1024 * 1024 * 1024d));
        }
    }
}
