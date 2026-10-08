package io.github.daiivr.dynmapwaystones;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkConstants;
import org.dynmap.DynmapCommonAPIListener;
import org.slf4j.Logger;

/**
 * Shows the server's waystones on the Dynmap web map, kept up to date every few seconds.
 */
@Mod(DynmapWaystones.MOD_ID)
public class DynmapWaystones {
    public static final String MOD_ID = "dynmapwaystones";
    static final Logger LOG = LogUtils.getLogger();

    private final WaystoneMarkers markers = new WaystoneMarkers();
    private MinecraftServer server;
    private int ticksUntilUpdate;

    public DynmapWaystones() {
        // Server side only: players don't need it installed to join
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        DynmapCommonAPIListener.register(markers);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        ticksUntilUpdate = 0;
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        server = null;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if ((event.phase != TickEvent.Phase.END) || (server == null)) {
            return;
        }
        if (--ticksUntilUpdate <= 0) {
            ticksUntilUpdate = Config.REFRESH_SECONDS.get() * 20;
            markers.update(server);
        }
    }
}
