package net.tapacywka.teyvatcartographia.map.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.tapacywka.teyvatcartographia.config.MapConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The plants the map may show (Settings > Map > Plants and its list): every block of the game and its mods that is a
 * plant by its class (bushes: flowers, saplings, mushrooms, crops, grass, ferns, lily pads, berry bushes, dead bushes;
 * cacti, sugar cane, chorus, bamboo) or by the vanilla plant tags (flowers, saplings, crops), and which of them the
 * player switched on ({@code MapConfig.PLANTS_SHOWN}, by block id). A plant that is off is not drawn: a soft one leaves
 * its column to the ground (or water) under it, a solid one (a cactus, chorus) is looked through to the ground.
 */
public final class Plants {
    private Plants() {}

    /** The plants switched on, read again when the setting changes; read by the chunk workers. */
    private static volatile Set<Block> shown = Set.of();
    private static List<? extends String> shownFrom;

    /** Whether a block is a plant (switched on or not). */
    public static boolean plant(Block b) {
        if (b instanceof BushBlock || b instanceof CactusBlock || b instanceof SugarCaneBlock || b instanceof ChorusPlantBlock
                || b instanceof ChorusFlowerBlock || b instanceof BambooStalkBlock) return true;
        BlockState s = b.defaultBlockState();
        // Tags are bound once a world is open (at the title screen they are empty).
        return s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS);
    }

    /** Whether the map looks through this block: a plant the list leaves out ({@code shown}: {@link #shownSet}). */
    public static boolean hidden(Block b, Set<Block> shown) {
        return !shown.contains(b) && plant(b);
    }

    /** The plants shown now (empty with the plants off), read once per chunk by the scanner. */
    public static Set<Block> shownSet() {
        List<? extends String> ids = MapConfig.get(MapConfig.PLANTS_SHOWN);
        if (ids != shownFrom) {
            Set<Block> s = new HashSet<>();
            for (String id : ids) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(s::add);
            }
            shown = s;
            shownFrom = ids;
        }
        return MapConfig.get(MapConfig.MAP_PLANTS) ? shown : Set.of();
    }

    /** Every plant of the game and its mods, by id. */
    public static List<Block> all() {
        List<Block> out = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) if (plant(b)) out.add(b);
        return out;
    }

    /** Switches a plant on or off in the list (saved). */
    public static void set(Block b, boolean on) {
        MapConfig.PLANTS_SHOWN.set(toggled(MapConfig.get(MapConfig.PLANTS_SHOWN), BuiltInRegistries.BLOCK.getKey(b).toString(), on));
        MapConfig.save();
    }

    /** The list of ids with {@code id} switched on (once, at the end) or off (every time it is there). */
    static List<String> toggled(List<? extends String> ids, String id, boolean on) {
        List<String> out = new ArrayList<>(ids);
        out.removeIf(id::equals);
        if (on) out.add(id);
        return out;
    }

    /** Whether a plant is switched on in the list (whatever the plants switch). */
    public static boolean listed(Block b) {
        return MapConfig.get(MapConfig.PLANTS_SHOWN).contains(BuiltInRegistries.BLOCK.getKey(b).toString());
    }
}
