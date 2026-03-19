package cn.ussshenzhou.notenoughbandwidth.stat;

import cn.ussshenzhou.network.StatQuery;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import static cn.ussshenzhou.notenoughbandwidth.stat.SimpleStatManager.*;

/**
 * @author USS_Shenzhou
 */
public class StatScreen extends Screen {
    private final String client = "Client";
    private final String actual = "Actual Transmission";
    private String actualC = "";
    private String raw = "Raw Payload";
    private String rawC = "";
    private String ratioC = "";

    private final String server = "Server";
    private String actualS = "-";
    private String rawS = "-";
    private String ratioS = "-";

    private int tick = 0;

    public StatScreen() {
        super(Component.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (tick % 10 == 0) {
            ClientPacketDistributor.sendToServer(new StatQuery());
            actualC = "↓ Inbound  "
                    + getReadableSpeed((int) LOCAL.inboundSpeedBaked().averageIn1s())
                    + "  Total  "
                    + getReadableSize(LOCAL.inboundBytesBaked().get())
                    + "    ↑ Outbound  "
                    + getReadableSpeed((int) LOCAL.outboundSpeedBaked().averageIn1s())
                    + "  Total  "
                    + getReadableSize(LOCAL.outboundBytesBaked().get());
            rawC = "↓ Inbound  "
                    + getReadableSpeed((int) LOCAL.inboundSpeedRaw().averageIn1s())
                    + "  Total  "
                    + getReadableSize(LOCAL.inboundBytesRaw().get())
                    + "    ↑ Outbound  "
                    + getReadableSpeed((int) LOCAL.outboundSpeedRaw().averageIn1s())
                    + "  Total  "
                    + getReadableSize(LOCAL.outboundBytesRaw().get());
            ratioC = "Ratio                            "
                    + String.format("%.2f", 100d * LOCAL.inboundBytesBaked().get() / LOCAL.inboundBytesRaw().get())
                    + "%                                        "
                    + String.format("%.2f", 100d * LOCAL.outboundBytesBaked().get() / LOCAL.outboundBytesRaw().get())
                    + "%";

            actualS = "↓ Inbound  "
                    + getReadableSpeed((int) inboundSpeedBakedServer)
                    + "  Total  "
                    + getReadableSize(inboundBytesBakedServer)
                    + "    ↑ Outbound  "
                    + getReadableSpeed((int) outboundSpeedBakedServer)
                    + "  Total  "
                    + getReadableSize(outboundBytesBakedServer);
            rawS = "↓ Inbound  "
                    + getReadableSpeed((int) inboundSpeedRawServer)
                    + "  Total  "
                    + getReadableSize(inboundBytesRawServer)
                    + "    ↑ Outbound  "
                    + getReadableSpeed((int) outboundSpeedRawServer)
                    + "  Total  "
                    + getReadableSize(outboundBytesRawServer);
            ratioS = "Ratio                            "
                    + String.format("%.2f", 100d * inboundBytesBakedServer / inboundBytesRawServer)
                    + "%                                        "
                    + String.format("%.2f", 100d * outboundBytesBakedServer / outboundBytesRawServer)
                    + "%";

        }
        tick++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        graphics.fill(0,0,width,height,0x80000000);
        var textRenderer = graphics.textRenderer();
        var pose = graphics.pose();
        textRenderer.accept(10, 10, Component.literal(client));
        textRenderer.accept(10, 30, Component.literal(actual));
        textRenderer.accept(10, 40, Component.literal(actualC));
        textRenderer.accept(10, 60, Component.literal(raw));
        textRenderer.accept(10, 70, Component.literal(rawC));
        textRenderer.accept(10, 90, Component.literal(ratioC));

        textRenderer.accept(10, 120, Component.literal(server));
        textRenderer.accept(10, 140, Component.literal(actual));
        textRenderer.accept(10, 150, Component.literal(actualS));
        textRenderer.accept(10, 170, Component.literal(raw));
        textRenderer.accept(10, 180, Component.literal(rawS));
        textRenderer.accept(10, 200, Component.literal(ratioS));
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
