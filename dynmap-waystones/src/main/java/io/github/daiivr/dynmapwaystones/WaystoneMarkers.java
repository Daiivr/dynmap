package io.github.daiivr.dynmapwaystones;

import com.mojang.authlib.GameProfile;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.Level;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Keeps a Dynmap marker set in step with the waystones on the server. Runs on the server thread.
 */
final class WaystoneMarkers extends DynmapCommonAPIListener {
    private static final String MARKER_SET_ID = "waystones";

    private enum Kind {
        WAYSTONE("waystone", "Waystone"),
        SHARESTONE("sharestone", "Sharestone"),
        WARP_PLATE("warp_plate", "Warp Plate"),
        PORTSTONE("warp_plate", "Portstone");

        final String icon;
        final String title;

        Kind(String icon, String title) {
            this.icon = icon;
            this.title = title;
        }

        static Kind of(ResourceLocation type) {
            String path = (type != null) ? type.getPath() : "";
            if (path.endsWith("sharestone")) {   // sharestone and the dyed <colour>_sharestone
                return SHARESTONE;
            }
            if (path.equals("warp_plate")) {
                return WARP_PLATE;
            }
            if (path.equals("portstone")) {
                return PORTSTONE;
            }
            return WAYSTONE;
        }
    }

    // Set from Dynmap's thread, used on the server thread
    private volatile MarkerAPI markerApi;

    private MarkerAPI usedApi;
    private final Map<String, MarkerIcon> icons = new HashMap<>();
    // What each marker currently shows, so unchanged ones are not rewritten every update
    private final Map<String, String> shown = new HashMap<>();
    private boolean failed;

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        markerApi = api.getMarkerAPI();
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        markerApi = null;
    }

    void update(MinecraftServer server) {
        MarkerAPI api = markerApi;
        if ((api == null) || failed) {    // Dynmap not started (yet) or its markers are disabled
            return;
        }
        if (api != usedApi) {   // Dynmap (re)started: its marker data starts afresh
            usedApi = api;
            icons.clear();
            shown.clear();
        }
        try {
            MarkerSet set = markerSet(api);
            if (set != null) {
                updateMarkers(server, api, set);
            }
        } catch (RuntimeException | LinkageError e) {
            // Most likely an incompatible Waystones or Dynmap version: say so once rather than every update
            failed = true;
            DynmapWaystones.LOG.error("Could not show waystones on the map, giving up until the server restarts", e);
        }
    }

    private MarkerSet markerSet(MarkerAPI api) {
        String label = Config.LAYER_LABEL.get();
        MarkerSet set = api.getMarkerSet(MARKER_SET_ID);
        if (set == null) {
            // Not persistent: rebuilt from the Waystones data on every start
            set = api.createMarkerSet(MARKER_SET_ID, label, null, false);
            if (set == null) {
                return null;
            }
        }
        // Only write what changed, so config edits apply without churning Dynmap's marker files
        if (!label.equals(set.getMarkerSetLabel())) {
            set.setMarkerSetLabel(label);
        }
        boolean hide = Config.HIDE_BY_DEFAULT.get();
        if (set.getHideByDefault() != hide) {
            set.setHideByDefault(hide);
        }
        int priority = Config.LAYER_PRIORITY.get();
        if (set.getLayerPriority() != priority) {
            set.setLayerPriority(priority);
        }
        int minZoom = Config.MIN_ZOOM.get();
        if (set.getMinZoom() != minZoom) {
            set.setMinZoom(minZoom);
        }
        Boolean showLabels = Config.ALWAYS_SHOW_LABELS.get() ? Boolean.TRUE : null;    // null: Dynmap's default (on hover)
        if (!Objects.equals(set.getLabelShow(), showLabels)) {
            set.setLabelShow(showLabels);
        }
        return set;
    }

    private void updateMarkers(MinecraftServer server, MarkerAPI api, MarkerSet set) {
        Map<String, Marker> stale = new HashMap<>();
        for (Marker marker : set.getMarkers()) {
            stale.put(marker.getMarkerID(), marker);
        }

        Iterator<IWaystone> waystones = WaystonesAPI.getAllWaystones(server).iterator();
        while (waystones.hasNext()) {
            IWaystone waystone = waystones.next();
            Kind kind = Kind.of(waystone.getWaystoneType());
            if (!isShown(waystone, kind)) {
                continue;
            }
            String id = waystone.getWaystoneUid().toString();
            String world = dynmapWorldName(server, waystone.getDimension());
            BlockPos pos = waystone.getPos();
            double x = pos.getX() + 0.5;
            double y = pos.getY();
            double z = pos.getZ() + 0.5;
            String label = waystone.hasName() ? waystone.getName() : kind.title;
            MarkerIcon icon = icon(api, kind);
            String description = description(server, waystone, kind, label, pos);

            String state = world + '|' + x + '|' + y + '|' + z + '|' + label + '|' + icon.getMarkerIconID() + '|' + description;
            Marker marker = stale.remove(id);
            if (marker == null) {
                marker = set.createMarker(id, label, false, world, x, y, z, icon, false);
                if (marker == null) {
                    continue;
                }
                marker.setDescription(description);
            }
            else if (!state.equals(shown.get(id))) {
                marker.setLabel(label, false);
                marker.setLocation(world, x, y, z);
                marker.setMarkerIcon(icon);
                marker.setDescription(description);
            }
            shown.put(id, state);
        }

        // Removed, or no longer shown because of the config
        for (Marker marker : stale.values()) {
            shown.remove(marker.getMarkerID());
            marker.deleteMarker();
        }
    }

    private static boolean isShown(IWaystone waystone, Kind kind) {
        if (!waystone.isValid()) {
            return false;
        }
        if (waystone.wasGenerated() && !Config.SHOW_GENERATED.get()) {
            return false;
        }
        if (Config.ONLY_GLOBAL.get() && !waystone.isGlobal()) {
            return false;
        }
        switch (kind) {
            case SHARESTONE:
                return Config.SHOW_SHARESTONES.get();
            case WARP_PLATE:
            case PORTSTONE:
                return Config.SHOW_WARP_PLATES.get();
            default:
                return true;
        }
    }

    private MarkerIcon icon(MarkerAPI api, Kind kind) {
        MarkerIcon icon = icons.get(kind.icon);
        if (icon != null) {
            return icon;
        }
        String id = "waystones_" + kind.icon;
        try (InputStream png = WaystoneMarkers.class.getResourceAsStream("/assets/dynmapwaystones/icons/" + kind.icon + ".png")) {
            if (png != null) {
                icon = api.getMarkerIcon(id);
                if (icon == null) {
                    icon = api.createMarkerIcon(id, kind.title, png);
                }
                else {
                    icon.setMarkerIconImage(png);   // Keep it in step with this jar's image
                }
            }
        } catch (IOException e) {
            DynmapWaystones.LOG.warn("Could not load the {} map icon", kind.icon, e);
        }
        if (icon == null) {
            icon = api.getMarkerIcon(MarkerIcon.DEFAULT);
        }
        icons.put(kind.icon, icon);
        return icon;
    }

    private static String description(MinecraftServer server, IWaystone waystone, Kind kind, String label, BlockPos pos) {
        StringBuilder html = new StringBuilder();
        html.append("<b>").append(escape(label)).append("</b><br/>");
        html.append("X ").append(pos.getX()).append(", Y ").append(pos.getY()).append(", Z ").append(pos.getZ()).append("<br/>");
        html.append(kind.title).append(" in ").append(escape(dimensionTitle(waystone.getDimension())));
        if (waystone.isGlobal()) {
            html.append(" (global)");
        }
        if (Config.SHOW_OWNER.get()) {
            String owner = ownerName(server, waystone.getOwnerUid());
            if (owner != null) {
                html.append("<br/>Placed by ").append(escape(owner));
            }
        }
        return html.toString();
    }

    /**
     * The name Dynmap gives a dimension's world on Forge (ForgeWorld.getWorldName, DynmapWorld.normalizeWorldName).
     */
    static String dynmapWorldName(MinecraftServer server, ResourceKey<Level> dimension) {
        String name;
        if (dimension.equals(Level.OVERWORLD)) {
            name = server.getWorldData().getLevelName();
        }
        else if (dimension.equals(Level.NETHER)) {
            name = "DIM-1";
        }
        else if (dimension.equals(Level.END)) {
            name = "DIM1";
        }
        else {
            name = dimension.location().getNamespace() + "_" + dimension.location().getPath();
        }
        return name.replace('/', '-').replace('[', '_').replace(']', '_');
    }

    private static String dimensionTitle(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return "the Overworld";
        }
        if (dimension.equals(Level.NETHER)) {
            return "the Nether";
        }
        if (dimension.equals(Level.END)) {
            return "the End";
        }
        return dimension.location().toString();
    }

    private static String ownerName(MinecraftServer server, UUID owner) {
        GameProfileCache profiles = server.getProfileCache();
        if ((owner == null) || (profiles == null)) {
            return null;
        }
        return profiles.get(owner).map(GameProfile::getName).orElse(null);
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&': escaped.append("&amp;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                default: escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
