/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.confabricate;

import ca.stellardrift.confabricate.typeserializers.IdentifierSerializer;
import ca.stellardrift.confabricate.typeserializers.RegistrySerializer;
import ca.stellardrift.confabricate.typeserializers.TaggableCollection;
import ca.stellardrift.confabricate.typeserializers.TaggableCollectionSerializer;
import ca.stellardrift.confabricate.typeserializers.TextSerializer;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.decoration.painting.PaintingMotive;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatType;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.structure.rule.PosRuleTestType;
import net.minecraft.structure.rule.RuleTestType;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.TagContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.carver.Carver;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.TreeDecoratorType;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.size.FeatureSizeType;
import net.minecraft.world.gen.foliage.FoliagePlacerType;
import net.minecraft.world.gen.placer.BlockPlacerType;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import net.minecraft.world.gen.trunk.TrunkPlacerType;
import net.minecraft.world.poi.PointOfInterestType;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.configurate.reference.ConfigurationReference;
import ninja.leaping.configurate.reference.WatchServiceListener;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.TransformAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Configurate integration holder, providing access to configuration loaders pre-configured to work with Minecraft types.
 *
 * This class has static utility methods for usage by other mods -- it should not be instantiated by anyone but the mod loader.
 */
public class Confabricate implements ModInitializer {
    static final String MOD_ID = "confabricate";

    private static Confabricate instance;
    static final Logger LOGGER = LogManager.getLogger();

    private WatchServiceListener listener;
    private TypeSerializerCollection mcTypeSerializers;
    private final Set<Registry<?>> brokenRegistries = Sets.newHashSet();
    private final Set<Registry<?>> registeredRegistries = Sets.newHashSet();

    public Confabricate() {
        if (instance != null) {
            throw new ExceptionInInitializerError("Confabricate can only be initialized by the Fabric mod loader");
        }
        instance = this;
    }

    static Identifier id(String item) {
        return new Identifier(MOD_ID, item);
    }

    @Override
    public void onInitialize() {
        try {
            listener = WatchServiceListener.create();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    listener.close();
                } catch (IOException e) {
                    LOGGER.catching(e);
                }
            }, "Confabricate shutdown thread"));
        } catch (IOException e) {
            LOGGER.error("Could not initialize file listener", e);
        }


        mcTypeSerializers = TypeSerializerCollection.defaults()
                .newChild()
                .register(IdentifierSerializer.TOKEN, IdentifierSerializer.INSTANCE)
                .register(TextSerializer.INSTANCE);

        registerRegistry(SoundEvent.class, Registry.SOUND_EVENT);
        registerTaggedRegistry(TypeToken.of(Fluid.class), Registry.FLUID, FluidTags.getContainer());
        registerRegistry(StatusEffect.class, Registry.STATUS_EFFECT);
        registerTaggedRegistry(TypeToken.of(Block.class), Registry.BLOCK, BlockTags.getContainer());
        registerRegistry(Enchantment.class, Registry.ENCHANTMENT);
        registerTaggedRegistry(new TypeToken<EntityType<?>>() {}, Registry.ENTITY_TYPE, EntityTypeTags.getContainer());
        registerTaggedRegistry(TypeToken.of(Item.class), Registry.ITEM, ItemTags.getContainer());
        registerRegistry(Potion.class, Registry.POTION);
        registerRegistry(new TypeToken<Carver<?>>() {}, Registry.CARVER);
        registerRegistry(new TypeToken<SurfaceBuilder<?>>() {}, Registry.SURFACE_BUILDER);
        registerRegistry(new TypeToken<Feature<?>>() {}, Registry.FEATURE);
        registerRegistry(new TypeToken<Decorator<?>>() {}, Registry.DECORATOR);
        registerRegistry(Biome.class, Registry.BIOME);
        registerRegistry(new TypeToken<BlockStateProviderType<?>>() {}, Registry.BLOCK_STATE_PROVIDER_TYPE);
        registerRegistry(new TypeToken<BlockPlacerType<?>>() {}, Registry.BLOCK_PLACER_TYPE);
        registerRegistry(new TypeToken<FoliagePlacerType<?>>() {}, Registry.FOLIAGE_PLACER_TYPE);
        registerRegistry(new TypeToken<TreeDecoratorType<?>>() {}, Registry.TREE_DECORATOR_TYPE);
        registerRegistry(new TypeToken<ParticleType<?>>() {}, Registry.PARTICLE_TYPE);
        registerRegistry(new TypeToken<Codec<? extends BiomeSource>>() {}, Registry.BIOME_SOURCE);
        registerRegistry(new TypeToken<BlockEntityType<?>>() {}, Registry.BLOCK_ENTITY_TYPE);
        // TODO: Figure out how to manage dimension types. They are present in "class_5318 and are not statically availible or DimensionTracker as Yarn PRs say"
        //registerRegistry(DimensionType.class, Registry.DIMENSION_TYPE);
        registerRegistry(PaintingMotive.class, Registry.PAINTING_MOTIVE);
        brokenRegistries.add(Registry.CUSTOM_STAT);
        //registerRegistry(Identifier, Registry.CUSTOM_STAT); // can't register -- doesn't have its own type
        registerRegistry(ChunkStatus.class, Registry.CHUNK_STATUS);
        registerRegistry(new TypeToken<StructureFeature<?>>() {}, Registry.STRUCTURE_FEATURE);
        registerRegistry(StructurePieceType.class, Registry.STRUCTURE_PIECE);
        registerRegistry(new TypeToken<RuleTestType<?>>() {}, Registry.RULE_TEST);
        registerRegistry(new TypeToken<StructureProcessorType<?>>() {}, Registry.STRUCTURE_PROCESSOR);
        registerRegistry(new TypeToken<StructurePoolElementType<?>>() {}, Registry.STRUCTURE_POOL_ELEMENT);
        registerRegistry(new TypeToken<ScreenHandlerType<?>>() {}, Registry.SCREEN_HANDLER);
        registerRegistry(new TypeToken<RecipeType<?>>() {}, Registry.RECIPE_TYPE);
        registerRegistry(new TypeToken<RecipeSerializer<?>>() {}, Registry.RECIPE_SERIALIZER);
        registerRegistry(new TypeToken<StatType<?>>() {}, Registry.STAT_TYPE);
        registerRegistry(VillagerType.class, Registry.VILLAGER_TYPE);
        registerRegistry(VillagerProfession.class, Registry.VILLAGER_PROFESSION);
        registerRegistry(PointOfInterestType.class, Registry.POINT_OF_INTEREST_TYPE);
        registerRegistry(new TypeToken<MemoryModuleType<?>>() {}, Registry.MEMORY_MODULE_TYPE);
        registerRegistry(new TypeToken<SensorType<?>>() {}, Registry.SENSOR_TYPE);
        registerRegistry(Schedule.class, Registry.SCHEDULE);
        registerRegistry(Activity.class, Registry.ACTIVITY);
        registerRegistry(new TypeToken<TrunkPlacerType<?>>() {}, Registry.TRUNK_PLACER_TYPE);
        registerRegistry(new TypeToken<FeatureSizeType<?>>() {}, Registry.FEATURE_SIZE_TYPE);
        //registerRegistry(PosRuleTestType.class, Registry.POS_RULE_TEST);
        registerRegistry(EntityAttribute.class, Registry.ATTRIBUTES);

        for (Registry reg : Registry.REGISTRIES) {
            if (!registeredRegistries.contains(reg) && !brokenRegistries.contains(reg)) {
                LOGGER.warn("Registry " + ((Registry) Registry.REGISTRIES).getId(reg) + " does not have an associated TypeSerializer!");
            }
        }

        // Commands for testing
       // CommandRegistry.INSTANCE.register(false, TestCommands::register);
    }

    private <T> void registerTaggedRegistry(TypeToken<T> token, Registry<T> registry, TagContainer<T> tagRegistry) {
        final TypeParameter<T> tParam = new TypeParameter<T>() {};
        final TypeToken<TaggableCollection<T>> fullToken = new TypeToken<TaggableCollection<T>>() {
        }.where(tParam, token);

        if (registeredRegistries.add(registry)) {
            mcTypeSerializers.register(fullToken, new TaggableCollectionSerializer<>(registry, tagRegistry));
            mcTypeSerializers.register(token, new RegistrySerializer<>(registry));
        }
    }

    /**
     * Register a TypeSerializer for a registry that has a generic type parameter
     *
     * @param token A token for the type contained within the registry
     * @param registry The registry
     * @param <T> The type registered by the registry
     */
    private <T> void registerRegistry(TypeToken<T> token, Registry<T> registry) {
        if (registeredRegistries.add(registry)) {
            mcTypeSerializers.register(token, new RegistrySerializer<>(registry));
        }
    }

    /**
     * Register a TypeSerializer for a {@link Registry} that has a concrete type
     *
     * @param registeredType The class contained within the registry
     * @param registry The registry to register
     * @param <T> The type contained by the registry
     */
    private <T> void registerRegistry(Class<T> registeredType, Registry<T> registry) {
        registerRegistry(TypeToken.of(registeredType), registry);
    }

    /**
     * Get a {@link TypeSerializerCollection} which contains additional {@link TypeSerializers} for Minecraft types,
     * in addition to the defaults provided by Configurate.
     *
     * While the collection is mutable, modifying it is discouraged in favour of working with a new child,
     * created with {@link TypeSerializerCollection#newChild()}. Collections of serializers will become immutable in Configurate 4.0
     *
     * @return Confabricate's collection of serializers.
     */
    public static TypeSerializerCollection getMinecraftTypeSerializers() {
        return instance.mcTypeSerializers;
    }

    /**
     * Create a configuration loader for the given mod's main configuration file.
     * By default, this config file is in a dedicated directory for the mod.
     *
     * @see #createLoaderFor(ModContainer, boolean)
     * @param mod The mod wanting to access its config
     * @return A configuration loader in the Hocon format
     */
    public static ConfigurationLoader<CommentedConfigurationNode> createLoaderFor(ModContainer mod) {
        return createLoaderFor(mod, true);
    }

    /**
     * Get a configuration loader for a mod. The configuration will be in Hocon format.
     * If the configuration is in its own directory, the path will be <pre>&lt;config root&gt;/&lt;modid&gt;/&lt;modid&gt;.conf</pre>
     * Otherwise, the path will be <pre>&lt;config root&gt;/&lt;modid&gt;.conf</pre>
     *
     * The returned {@link ConfigurationLoader ConfigurationLoaders} will be pre-configured to use the type serializers
     * from {@link #getMinecraftTypeSerializers()}, but will otherwise use default settings.
     *
     * @param mod The mod to get the configuration loader for
     * @param ownDirectory Whether the configuration should be in a directory just for the mod
     * @return The newly created configuration loader
     */
    public static ConfigurationLoader<CommentedConfigurationNode> createLoaderFor(ModContainer mod, boolean ownDirectory) {
        return HoconConfigurationLoader.builder()
                .setPath(getConfigurationFile(mod, ownDirectory))
                .setDefaultOptions(o -> o.withSerializers(getMinecraftTypeSerializers()))
                .build();
    }

    /**
     * Create a configuration reference to the provided mod's main configuration file
     * By default, this config file is in a dedicated directory for the mod.
     * The returned reference will automatically reload.
     *
     * @see #createConfigurationFor(ModContainer, boolean)
     * @param mod The mod wanting to access its config
     * @return A configuration reference for a loaded node in HOCON format
     * @throws IOException if a listener could not be established or the configuration failed to load
     */
    public static ConfigurationReference<CommentedConfigurationNode> createConfigurationFor(ModContainer mod) throws IOException {
        return createConfigurationFor(mod, true);
    }

    /**
     * Get a configuration reference for a mod. The configuration will be in Hocon format.
     * If the configuration is in its own directory, the path will be <pre>&lt;config root&gt;/&lt;modid&gt;/&lt;modid&gt;.conf</pre>
     * Otherwise, the path will be <pre>&lt;config root&gt;/&lt;modid&gt;.conf</pre>
     *
     * The reference's {@link ConfigurationLoader} will be pre-configured to use the type serializers
     * from {@link #getMinecraftTypeSerializers()}, but will otherwise use default settings.
     *
     * @param mod The mod to get the configuration loader for
     * @param ownDirectory Whether the configuration should be in a directory just for the mod
     * @return The newly created and loaded configuration reference
     * @throws IOException if a listener could not be established or the configuration failed to load
     */
    public static ConfigurationReference<CommentedConfigurationNode> createConfigurationFor(ModContainer mod, boolean ownDirectory) throws IOException {
        return getFileWatcher().listenToConfiguration(path -> {
            return HoconConfigurationLoader.builder()
                    .setPath(path)
                    .setDefaultOptions(o -> o.withSerializers(getMinecraftTypeSerializers()))
                    .build();
        }, getConfigurationFile(mod, ownDirectory));
    }

    /**
     * Get the path to a configuration file in HOCON format for the provided mod.
     *
     * HOCON uses the {@code .conf} file extension.
     *
     * @param mod container of the mod
     * @param ownDirectory whether the configuration should be in its own directory, or in the main configuration directory
     * @return path to a configuration file
     */
    public static Path getConfigurationFile(ModContainer mod, boolean ownDirectory) {
        Path configRoot = FabricLoader.getInstance().getConfigDirectory().toPath();
        if (ownDirectory) {
            configRoot = configRoot.resolve(mod.getMetadata().getId());
        }
        return configRoot.resolve(mod.getMetadata().getId() + ".conf");
    }

    /**
     * See static variant
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes provided to the transformer
     * @return A transformation that executes a {@link DataFixer} transformation.
     * @deprecated see {@link #createTransformation(DataFixer, DSL.TypeReference, int, Object...)}, this should have been static
     */
    @Deprecated
    public ConfigurationTransformation createTransformationFrom(DataFixer fixer, DSL.TypeReference reference, int targetVersion, Object... versionKey) {
        return createTransformation(fixer, reference, targetVersion, versionKey);
    }

    /**
     * See static variant
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes provided to the transformer
     * @return A transformation that executes a {@link DataFixer} transformation.
     * @deprecated {@link #createTransformAction(DataFixer, DSL.TypeReference, int, Object...)}, this should have been static
     */
    @Deprecated
    public TransformAction createTransformActionFrom(DataFixer fixer, DSL.TypeReference reference, int targetVersion, Object... versionKey) {
        return createTransformAction(fixer, reference, targetVersion, versionKey);
    }

    /**
     * Create a {@link ConfigurationTransformation} that applies a {@link DataFixer} to a Configurate node. The current
     * version of the node is provided by the path {@code versionKey}. The transformation is executed from the provided node.
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes provided to the transformer
     * @return A transformation that executes a {@link DataFixer} transformation.
     */
    public static ConfigurationTransformation createTransformation(DataFixer fixer, DSL.TypeReference reference, int targetVersion, Object... versionKey) {
        return ConfigurationTransformation.builder()
                .addAction(new Object[]{}, createTransformAction(fixer, reference, targetVersion, versionKey))
                .build();

    }

    /**
     * Create a TransformAction applying a {@link DataFixer} to a Configurate node. This can be used within {@link ConfigurationTransformation}
     * when some values are controlled by DFUs and some aren't.
     *
     * @param fixer The fixer containing DFU transformations to apply
     * @param reference The reference to the DFU {@link DSL} type representing this node
     * @param targetVersion The version to convert to
     * @param versionKey The location of the data version in nodes seen by this action
     * @return The created action
     */
    public static TransformAction createTransformAction(DataFixer fixer, DSL.TypeReference reference, int targetVersion, Object... versionKey) {
        return (inputPath, valueAtPath) ->  {
            final int currentVersion = valueAtPath.getNode(versionKey).getInt(-1);
            final Dynamic<ConfigurationNode> dyn = ConfigurateOps.wrap(valueAtPath);
            valueAtPath.setValue(fixer.update(reference, dyn, currentVersion, targetVersion).getValue());
            return null;
        };
    }

    public static WatchServiceListener getFileWatcher() {
        final WatchServiceListener ret = instance.listener;
        if (ret == null) {
            throw new IllegalStateException("Configurate file watcher failed to initialize, check log for earlier errors");
        }
        return ret;
    }
}
