package com.meteorologia;

import com.meteorologia.block.RadarBlock;
import com.meteorologia.block.RainGaugeBlock;
import com.meteorologia.block.SensorBlock;
import com.meteorologia.block.StormSirenBlock;
import com.meteorologia.entity.HotAirBalloon;
import com.meteorologia.item.EmergencyKitItem;
import com.meteorologia.item.HandheldMeterItem;
import com.meteorologia.item.HotAirBalloonItem;
import com.meteorologia.item.WeatherRadioItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Meteorologia.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Meteorologia.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Meteorologia.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Meteorologia.MODID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    // ------------------------------------------------------------ blocos ----
    public static final DeferredHolder<Block, RadarBlock> RADAR =
            BLOCKS.register("radar", () -> new RadarBlock(metal()));
    public static final DeferredHolder<Block, SensorBlock> ANEMOMETER =
            BLOCKS.register("anemometer", () -> new SensorBlock(metal(), SensorBlock.Kind.ANEMOMETER));
    public static final DeferredHolder<Block, SensorBlock> BAROMETER =
            BLOCKS.register("barometer", () -> new SensorBlock(metal(), SensorBlock.Kind.BAROMETER));
    public static final DeferredHolder<Block, SensorBlock> THERMOMETER =
            BLOCKS.register("thermometer", () -> new SensorBlock(metal(), SensorBlock.Kind.THERMOMETER));
    public static final DeferredHolder<Block, SensorBlock> WEATHER_STATION =
            BLOCKS.register("weather_station", () -> new SensorBlock(metal(), SensorBlock.Kind.STATION));
    public static final DeferredHolder<Block, RainGaugeBlock> RAIN_GAUGE =
            BLOCKS.register("rain_gauge", () -> new RainGaugeBlock(metal().randomTicks()));
    public static final DeferredHolder<Block, StormSirenBlock> STORM_SIREN =
            BLOCKS.register("storm_siren", () -> new StormSirenBlock(metal()));

    // ------------------------------------------------- itens de bloco -------
    public static final DeferredHolder<Item, BlockItem> RADAR_ITEM =
            ITEMS.register("radar", () -> new BlockItem(RADAR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ANEMOMETER_ITEM =
            ITEMS.register("anemometer", () -> new BlockItem(ANEMOMETER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> BAROMETER_ITEM =
            ITEMS.register("barometer", () -> new BlockItem(BAROMETER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> THERMOMETER_ITEM =
            ITEMS.register("thermometer", () -> new BlockItem(THERMOMETER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WEATHER_STATION_ITEM =
            ITEMS.register("weather_station", () -> new BlockItem(WEATHER_STATION.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> RAIN_GAUGE_ITEM =
            ITEMS.register("rain_gauge", () -> new BlockItem(RAIN_GAUGE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> STORM_SIREN_ITEM =
            ITEMS.register("storm_siren", () -> new BlockItem(STORM_SIREN.get(), new Item.Properties()));

    // ------------------------------------------------------------ itens -----
    public static final DeferredHolder<Item, Item> WEATHER_RADIO =
            ITEMS.register("weather_radio", () -> new WeatherRadioItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> HANDHELD_METER =
            ITEMS.register("handheld_meter", () -> new HandheldMeterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> EMERGENCY_KIT =
            ITEMS.register("emergency_kit", () -> new EmergencyKitItem(new Item.Properties().stacksTo(4)));
    public static final DeferredHolder<Item, Item> PROPANE_TANK =
            ITEMS.register("propane_tank", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> ENERGY_BAR =
            ITEMS.register("energy_bar", () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).fast().build())));
    public static final DeferredHolder<Item, Item> BALLOON_ITEM =
            ITEMS.register("hot_air_balloon", () -> new HotAirBalloonItem(new Item.Properties().stacksTo(1)));

    // ---------------------------------------------------------- entidades ---
    public static final DeferredHolder<EntityType<?>, EntityType<HotAirBalloon>> BALLOON =
            ENTITIES.register("hot_air_balloon", () -> EntityType.Builder
                    .<HotAirBalloon>of(HotAirBalloon::new, MobCategory.MISC)
                    .sized(2.2F, 6.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("hot_air_balloon"));

    // --------------------------------------------------------- aba criativa -
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("meteorologia", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.meteorologia"))
                    .icon(() -> new ItemStack(RADAR_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(RADAR_ITEM.get());
                        output.accept(ANEMOMETER_ITEM.get());
                        output.accept(BAROMETER_ITEM.get());
                        output.accept(THERMOMETER_ITEM.get());
                        output.accept(WEATHER_STATION_ITEM.get());
                        output.accept(RAIN_GAUGE_ITEM.get());
                        output.accept(STORM_SIREN_ITEM.get());
                        output.accept(WEATHER_RADIO.get());
                        output.accept(HANDHELD_METER.get());
                        output.accept(EMERGENCY_KIT.get());
                        output.accept(ENERGY_BAR.get());
                        output.accept(PROPANE_TANK.get());
                        output.accept(BALLOON_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }
}
