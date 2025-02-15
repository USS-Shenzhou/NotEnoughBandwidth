package cn.ussshenzhou.notenoughbandwidth.managers;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
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
public class PacketTypeIndexManager {
    private static volatile boolean initialized = false;
    private static final ArrayList<String> NAMESPACES = new ArrayList<>();
    private static final ArrayList<ArrayList<String>> PATHS = new ArrayList<>();
    private static final Object2IntMap<String> NAMESPACE_MAP = new Object2IntOpenHashMap<>();
    private static final HashMap<Integer, Object2IntMap<String>> PATH_MAPS = new HashMap<>();
    private static final VarHandle PAYLOAD_REGISTRATIONS;

    static {
        try {
            var lookup = MethodHandles.lookup();
            var privateLookup = MethodHandles.privateLookupIn(NetworkRegistry.class, lookup);
            PAYLOAD_REGISTRATIONS = privateLookup.findStaticVarHandle(NetworkRegistry.class,"PAYLOAD_REGISTRATIONS", Map.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init(NetworkPayloadSetup setup) {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER && initialized) {
            return;
        }
        synchronized (NAMESPACES) {
            PacketAggregationManager.init();

            initialized = false;
            NAMESPACES.clear();
            PATHS.clear();
            NAMESPACE_MAP.clear();
            PATH_MAPS.clear();

            List<ResourceLocation> types = new ArrayList<>(setup.channels().get(ConnectionProtocol.PLAY).keySet());
            types.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));
            AtomicInteger namespaceIndex = new AtomicInteger();
            @SuppressWarnings("unchecked")
            var registration = ((Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>>) PAYLOAD_REGISTRATIONS.get()).get(ConnectionProtocol.PLAY);
            types.forEach(type -> {
                if (!registration.containsKey(type) || registration.get(type).optional()) {
                    return;
                }
                if (!NAMESPACE_MAP.containsKey(type.getNamespace())) {
                    NAMESPACE_MAP.put(type.getNamespace(), namespaceIndex.get());
                    NAMESPACES.add(type.getNamespace());
                    PATHS.add(new ArrayList<>());
                    namespaceIndex.getAndIncrement();
                }
                PATH_MAPS.compute(namespaceIndex.get() - 1, (namespaceId1, pathMap) -> {
                    if (pathMap == null) {
                        pathMap = new Object2IntOpenHashMap<>();
                    }
                    pathMap.put(type.getPath(), pathMap.size());
                    return pathMap;
                });
                PATHS.get(namespaceIndex.get() - 1).add(type.getPath());
            });

            var logger = LogUtils.getLogger();
            if (logger.isDebugEnabled()) {
                logger.debug("PacketTypeIndexManager initialized.");
                NAMESPACE_MAP.forEach((namespace, id) -> {
                    logger.debug("namespace: {} id: {}", namespace, id);
                    PATH_MAPS.get(id).forEach((path, id1) -> logger.debug("- path: {} id: {}", path, id1));

                });
            }
            if (NAMESPACES.size() > 4096 || PATHS.stream().anyMatch(l -> l.size() > 4096)) {
                throw new RuntimeException("There are too many namespaces and/or paths (Max 4096 namespaces, 4096 paths for each namespace).");
            }
            initialized = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean contains(ResourceLocation type) {
        if (!initialized) {
            return false;
        }
        return NAMESPACE_MAP.containsKey(type.getNamespace()) && PATH_MAPS.get(NAMESPACE_MAP.getInt(type.getNamespace())).containsKey(type.getPath());
    }

    public static int getIndex(ResourceLocation type) {
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

    public static int getIndexNotTight(ResourceLocation type) {
        if (initialized && contains(type)) {
            int namespaceIndex = NAMESPACE_MAP.getInt(type.getNamespace());
            int pathIndex = PATH_MAPS.get(namespaceIndex).getInt(type.getPath());
            return 0x80000000 | (namespaceIndex << 12) | (pathIndex);
        }
        return 0;
    }

    public static ResourceLocation getResourceLocation(int index, boolean tight) {
        if (!initialized) {
            return null;
        }
        int namespaceIndex, pathIndex;
        if (tight) {
            namespaceIndex = (index & 0b11111111_00000000) >>> 8;
            pathIndex = (index & 0b00000000_11111111);
        } else {
            namespaceIndex = (index & 0b11111111_11110000_00000000) >>> 12;
            pathIndex = (index & 0b00000000_00001111_11111111);
        }
        return ResourceLocation.fromNamespaceAndPath(NAMESPACES.get(namespaceIndex), PATHS.get(namespaceIndex).get(pathIndex));
    }
}
