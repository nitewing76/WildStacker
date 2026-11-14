package com.bgsoftware.wildstacker.utils;

import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.utils.data.structures.FastEnumArray;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class GeneralUtils {

    private static final NumberFormat numberFormatter = DecimalFormat.getNumberInstance();

    static {
        numberFormatter.setGroupingUsed(true);
        numberFormatter.setMinimumFractionDigits(2);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setRoundingMode(RoundingMode.FLOOR);
    }

    private GeneralUtils() {
    }

    public static boolean containsOrEmpty(FastEnumArray<Material> fastEnumArray, Material itemType) {
        return fastEnumArray.size() == 0 || fastEnumArray.contains(itemType);
    }

    public static boolean containsOrEmpty(List<String> list, String element) {
        return list.isEmpty() || element.isEmpty() || list.contains(element);
    }

    public static boolean isSameChunk(Location location, Chunk chunk) {
        return chunk.getX() == location.getBlockX() >> 4 && chunk.getZ() == location.getBlockZ() >> 4;
    }

    public static boolean isChunkLoaded(Location location) {
        return location != null && location.getWorld() != null &&
                location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static <T extends StackedObject<?>> Optional<T> getClosest(Location origin, Stream<T> objects) {
        Map<T, Double> distances = new HashMap<>();
        return objects.min((o1, o2) -> {
            double firstDistance = distances.computeIfAbsent(o1, obj -> distance(obj.getLocation(), origin));
            double secondDistance = distances.computeIfAbsent(o2, obj -> distance(obj.getLocation(), origin));
            return Double.compare(firstDistance, secondDistance);
        });
    }

    public static <T extends Entity> Optional<T> getClosestBukkit(Location origin, Stream<T> objects) {
        Map<T, Double> distances = new HashMap<>();
        return objects.min((o1, o2) -> {
            double firstDistance = distances.computeIfAbsent(o1, obj -> distance(obj.getLocation(), origin));
            double secondDistance = distances.computeIfAbsent(o2, obj -> distance(obj.getLocation(), origin));
            return Double.compare(firstDistance, secondDistance);
        });
    }

    public static String format(double number) {
        return numberFormatter.format(number);
    }

    private static double distance(Location loc1, Location loc2) {
        return (loc1 == null || loc2 == null || loc1.getWorld() != loc2.getWorld())
                ? Double.POSITIVE_INFINITY
                : loc1.distanceSquared(loc2);
    }

}