package cn.ussshenzhou.notenoughbandwidth.indextype;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("UnstableApiUsage")
public class NamespaceIndexManager {
    private static volatile boolean initialized = false;
    private static final ArrayList<String> NAMESPACES = new ArrayList<>();
    private static final ArrayList<ArrayList<String>> PATHS = new ArrayList<>();
    private static final Object2IntMap<String> NAMESPACE_MAP = new Object2IntOpenHashMap<>();
    private static final HashMap<Integer, Object2IntMap<String>> PATH_MAPS = new HashMap<>();
    private static final VarHandle PAYLOAD_REGISTRATIONS;

    static {
        try {
            //TODO add mc
            var lookup = MethodHandles.lookup();
            var privateLookup = MethodHandles.privateLookupIn(NetworkRegistry.class, lookup);
            PAYLOAD_REGISTRATIONS = privateLookup.findStaticVarHandle(NetworkRegistry.class, "PAYLOAD_REGISTRATIONS", Map.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see net.minecraft.network.protocol.game.GamePacketTypes
     */
    private static final List<String> VANILLA_PATHS = new ArrayList<>() {{
        add("bundle");
        add("bundle_delimiter");
        add("add_entity");
        add("animate");
        add("award_stats");
        add("block_changed_ack");
        add("block_destruction");
        add("block_entity_data");
        add("block_event");
        add("block_update");
        add("boss_event");
        add("change_difficulty");
        add("chunk_batch_finished");
        add("chunk_batch_start");
        add("chunks_biomes");
        add("clear_titles");
        add("command_suggestions");
        add("commands");
        add("container_close");
        add("container_set_content");
        add("container_set_data");
        add("container_set_slot");
        add("cooldown");
        add("custom_chat_completions");
        add("damage_event");
        add("debug/block_value");
        add("debug/chunk_value");
        add("debug/entity_value");
        add("debug/event");
        add("debug_sample");
        add("delete_chat");
        add("disguised_chat");
        add("entity_event");
        add("entity_position_sync");
        add("explode");
        add("forget_level_chunk");
        add("game_event");
        add("game_test_highlight_pos");
        add("mount_screen_open");
        add("hurt_animation");
        add("initialize_border");
        add("level_chunk_with_light");
        add("level_event");
        add("level_particles");
        add("light_update");
        add("login");
        add("low_disk_space_warning");
        add("map_item_data");
        add("merchant_offers");
        add("move_entity_pos");
        add("move_entity_pos_rot");
        add("move_minecart_along_track");
        add("move_entity_rot");
        add("move_vehicle");
        add("open_book");
        add("open_screen");
        add("open_sign_editor");
        add("place_ghost_recipe");
        add("player_abilities");
        add("player_chat");
        add("player_combat_end");
        add("player_combat_enter");
        add("player_combat_kill");
        add("player_info_remove");
        add("player_info_update");
        add("player_look_at");
        add("player_position");
        add("player_rotation");
        add("recipe_book_add");
        add("recipe_book_remove");
        add("recipe_book_settings");
        add("remove_entities");
        add("remove_mob_effect");
        add("respawn");
        add("rotate_head");
        add("section_blocks_update");
        add("select_advancements_tab");
        add("server_data");
        add("set_action_bar_text");
        add("set_border_center");
        add("set_border_lerp_size");
        add("set_border_size");
        add("set_border_warning_delay");
        add("set_border_warning_distance");
        add("set_camera");
        add("set_chunk_cache_center");
        add("set_chunk_cache_radius");
        add("set_default_spawn_position");
        add("set_display_objective");
        add("set_entity_data");
        add("set_entity_link");
        add("set_entity_motion");
        add("set_equipment");
        add("set_experience");
        add("set_health");
        add("set_held_slot");
        add("set_objective");
        add("set_passengers");
        add("set_player_team");
        add("set_score");
        add("set_simulation_distance");
        add("set_subtitle_text");
        add("set_time");
        add("set_title_text");
        add("set_titles_animation");
        add("sound_entity");
        add("sound");
        add("start_configuration");
        add("stop_sound");
        add("system_chat");
        add("tab_list");
        add("tag_query");
        add("take_item_entity");
        add("teleport_entity");
        add("test_instance_block_status");
        add("update_advancements");
        add("update_attributes");
        add("update_mob_effect");
        add("update_recipes");
        add("projectile_power");
        add("waypoint");
        add("accept_teleportation");
        add("block_entity_tag_query");
        add("bundle_item_selected");
        add("change_game_mode");
        add("chat_ack");
        add("chat_command");
        add("chat_command_signed");
        add("chat");
        add("chat_session_update");
        add("chunk_batch_received");
        add("client_command");
        add("client_tick_end");
        add("command_suggestion");
        add("configuration_acknowledged");
        add("container_button_click");
        add("container_click");
        add("container_slot_state_changed");
        add("debug_subscription_request");
        add("edit_book");
        add("entity_tag_query");
        add("interact");
        add("jigsaw_generate");
        add("lock_difficulty");
        add("move_player_pos");
        add("move_player_pos_rot");
        add("move_player_rot");
        add("move_player_status_only");
        add("paddle_boat");
        add("pick_item_from_block");
        add("pick_item_from_entity");
        add("place_recipe");
        add("player_action");
        add("player_command");
        add("player_input");
        add("player_loaded");
        add("recipe_book_change_settings");
        add("recipe_book_seen_recipe");
        add("rename_item");
        add("seen_advancements");
        add("select_trade");
        add("set_beacon");
        add("set_carried_item");
        add("set_command_block");
        add("set_command_minecart");
        add("set_creative_mode_slot");
        add("set_jigsaw_block");
        add("set_structure_block");
        add("set_test_block");
        add("test_instance_block_action");
        add("sign_update");
        add("swing");
        add("teleport_to_entity");
        add("use_item_on");
        add("use_item");
        add("reset_score");
        add("ticking_state");
        add("ticking_step");
        add("set_cursor_item");
        add("set_player_inventory");
    }};

    public synchronized static void init(NetworkPayloadSetup setup) {
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER && initialized) {
            return;
        }
        initialized = false;
        NAMESPACES.clear();
        PATHS.clear();
        NAMESPACE_MAP.clear();
        PATH_MAPS.clear();


        AtomicInteger namespaceIndex = new AtomicInteger();
        indexVanillaPackets(namespaceIndex);
        indexCustomPayloads(setup, namespaceIndex);

        initTrace();
        if (NAMESPACES.size() > 4096 || PATHS.stream().anyMatch(l -> l.size() > 4096)) {
            throw new RuntimeException("There are too many namespaces and/or paths (Max 4096 namespaces, 4096 paths for each namespace). NEB is not designed to work with so many mods.");
        }
        initialized = true;
    }

    private static void indexVanillaPackets(AtomicInteger namespaceIndex) {
        VANILLA_PATHS.forEach(path -> fillSingle(namespaceIndex, Identifier.withDefaultNamespace(path)));
    }

    private static void indexCustomPayloads(NetworkPayloadSetup setup, AtomicInteger namespaceIndex) {
        List<Identifier> types = new ArrayList<>(setup.channels().get(ConnectionProtocol.PLAY).keySet());
        types.sort(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath));
        @SuppressWarnings("unchecked")
        var registration = ((Map<ConnectionProtocol, Map<Identifier, PayloadRegistration<?>>>) PAYLOAD_REGISTRATIONS.get()).get(ConnectionProtocol.PLAY);
        types.forEach(type -> {
            if (!registration.containsKey(type) || registration.get(type).optional()) {
                return;
            }
            fillSingle(namespaceIndex, type);
        });
    }

    private static void initTrace() {
        var logger = LogUtils.getLogger();
        if (logger.isDebugEnabled()) {
            logger.debug("PacketTypeIndexManager initialized.");
            NAMESPACE_MAP.forEach((namespace, id) -> {
                logger.debug("namespace: {} id: {}", namespace, id);
                PATH_MAPS.get(id).forEach((path, id1) -> logger.debug("- path: {} id: {}", path, id1));
            });
        }
    }

    private static void fillSingle(AtomicInteger namespaceIndex, Identifier packetId) {
        if (!NAMESPACE_MAP.containsKey(packetId.getNamespace())) {
            NAMESPACE_MAP.put(packetId.getNamespace(), namespaceIndex.get());
            NAMESPACES.add(packetId.getNamespace());
            PATHS.add(new ArrayList<>());
            namespaceIndex.getAndIncrement();
        }
        PATH_MAPS.compute(namespaceIndex.get() - 1, (namespaceId1, pathMap) -> {
            if (pathMap == null) {
                pathMap = new Object2IntOpenHashMap<>();
            }
            pathMap.put(packetId.getPath(), pathMap.size());
            return pathMap;
        });
        PATHS.get(namespaceIndex.get() - 1).add(packetId.getPath());
    }

    private static boolean contains(Identifier type) {
        if (!initialized) {
            return false;
        }
        return NAMESPACE_MAP.containsKey(type.getNamespace()) && PATH_MAPS.get(NAMESPACE_MAP.getInt(type.getNamespace())).containsKey(type.getPath());
    }

    public static int getNebIndex(Identifier type) {
        if (initialized && contains(type)) {
            int namespaceIndex = NAMESPACE_MAP.getInt(type.getNamespace());
            int pathIndex = PATH_MAPS.get(namespaceIndex).getInt(type.getPath());
            if (namespaceIndex < 256 && pathIndex < 256) {
                return 0xc0000000 | (namespaceIndex << 16) | (pathIndex << 8);
            } else {
                return 0x80000000 | (namespaceIndex << 12) | (pathIndex);
            }
        }
        return 0;
    }

    public static int getNebIndexNotTight(Identifier type) {
        if (initialized && contains(type)) {
            int namespaceIndex = NAMESPACE_MAP.getInt(type.getNamespace());
            int pathIndex = PATH_MAPS.get(namespaceIndex).getInt(type.getPath());
            return 0x80000000 | (namespaceIndex << 12) | (pathIndex);
        }
        return 0;
    }

    public static Identifier getIdentifier(int nebIndex, boolean tight) {
        if (!initialized) {
            return null;
        }
        int namespaceIndex, pathIndex;
        if (tight) {
            namespaceIndex = (nebIndex & 0b11111111_00000000) >>> 8;
            pathIndex = (nebIndex & 0b00000000_11111111);
        } else {
            namespaceIndex = (nebIndex & 0b11111111_11110000_00000000) >>> 12;
            pathIndex = (nebIndex & 0b00000000_00001111_11111111);
        }
        return Identifier.fromNamespaceAndPath(NAMESPACES.get(namespaceIndex), PATHS.get(namespaceIndex).get(pathIndex));
    }
}
