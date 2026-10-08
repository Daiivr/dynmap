package io.github.daiivr.dynmapwaystones;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * config/dynmapwaystones-common.toml
 */
public final class Config {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_GENERATED;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHARESTONES;
    public static final ForgeConfigSpec.BooleanValue SHOW_WARP_PLATES;
    public static final ForgeConfigSpec.BooleanValue ONLY_GLOBAL;
    public static final ForgeConfigSpec.BooleanValue SHOW_OWNER;

    public static final ForgeConfigSpec.ConfigValue<String> LAYER_LABEL;
    public static final ForgeConfigSpec.BooleanValue HIDE_BY_DEFAULT;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SHOW_LABELS;
    public static final ForgeConfigSpec.IntValue LAYER_PRIORITY;
    public static final ForgeConfigSpec.IntValue MIN_ZOOM;

    public static final ForgeConfigSpec.IntValue REFRESH_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("waystones");
        SHOW_GENERATED = builder
                .comment("Also show waystones players did not place: generated in villages, the wilderness and dungeons, or by",
                        "other mods' structures (by default only the ones players placed, which excludes any placed before",
                        "Waystones recorded who placed them)")
                .define("showGenerated", false);
        SHOW_SHARESTONES = builder
                .comment("Show sharestones")
                .define("showSharestones", true);
        SHOW_WARP_PLATES = builder
                .comment("Show warp plates and portstones")
                .define("showWarpPlates", false);
        ONLY_GLOBAL = builder
                .comment("Only show global waystones")
                .define("onlyGlobal", false);
        SHOW_OWNER = builder
                .comment("Show who placed a waystone in its popup")
                .define("showOwner", true);
        builder.pop();

        builder.push("layer");
        LAYER_LABEL = builder
                .comment("Name of the layer in the map's layer control")
                .define("label", "Waystones");
        HIDE_BY_DEFAULT = builder
                .comment("Hide the layer until it is switched on in the layer control")
                .define("hideByDefault", false);
        ALWAYS_SHOW_LABELS = builder
                .comment("Always show waystone names next to their icons (otherwise they show on hover)")
                .define("alwaysShowLabels", false);
        LAYER_PRIORITY = builder
                .comment("Position of the layer in the layer control (lower is higher up)")
                .defineInRange("priority", 10, 0, 1000);
        MIN_ZOOM = builder
                .comment("Lowest zoom level the waystones are shown at (-1 = at every zoom level)")
                .defineInRange("minZoom", -1, -1, 20);
        builder.pop();

        REFRESH_SECONDS = builder
                .comment("How often placed, renamed and removed waystones are brought onto the map, in seconds")
                .defineInRange("refreshSeconds", 10, 1, 3600);

        SPEC = builder.build();
    }

    private Config() {
    }
}
