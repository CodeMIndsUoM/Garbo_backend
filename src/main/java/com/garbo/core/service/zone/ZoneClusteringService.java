package com.garbo.core.service.zone;

import com.garbo.core.entity.Bin;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Geographic zone assignment via K-means on bin coordinates (no ML).
 * Used when creating bins (auto zone) and when splitting auto-route drafts.
 */
@Service
public class ZoneClusteringService {

    private static final int MIN_ZONES = 2;
    private static final int MAX_ZONES = 8;
    private static final int K_MEANS_ITERATIONS = 20;

    /** Assign a 1-based zone label for a new bin from its coordinates within a council. */
    public String assignZoneForCoordinates(String council, double lat, double lng, List<Bin> councilBins) {
        List<double[]> points = new ArrayList<>();
        List<Integer> binIndex = new ArrayList<>();

        for (int i = 0; i < councilBins.size(); i++) {
            Bin b = councilBins.get(i);
            if (b.getLatitude() == null || b.getLongitude() == null) {
                continue;
            }
            points.add(new double[] { b.getLatitude(), b.getLongitude() });
            binIndex.add(i);
        }
        points.add(new double[] { lat, lng });
        int newPointIndex = points.size() - 1;

        if (points.size() < MIN_ZONES) {
            return "1";
        }

        int k = resolveZoneCount(points.size());
        int[] labels = kMeans(points, k);
        return String.valueOf(labels[newPointIndex] + 1);
    }

    /**
     * Split eligible bins into route groups: geographic clusters, then cap by vehicle capacity.
     */
    public List<List<Bin>> splitIntoRouteGroups(List<Bin> eligible, int maxBinsPerRoute) {
        if (eligible == null || eligible.isEmpty()) {
            return List.of();
        }
        if (maxBinsPerRoute <= 0) {
            maxBinsPerRoute = 25;
        }

        List<Bin> sorted = eligible.stream()
                .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                .sorted(binPriorityComparator())
                .collect(Collectors.toCollection(ArrayList::new));

        if (sorted.isEmpty()) {
            return List.of();
        }

        int targetRoutes = (int) Math.ceil((double) sorted.size() / maxBinsPerRoute);
        int k = Math.max(1, Math.min(targetRoutes, sorted.size()));

        List<double[]> points = sorted.stream()
                .map(b -> new double[] { b.getLatitude(), b.getLongitude() })
                .toList();
        int[] labels = kMeans(points, k);

        Map<Integer, List<Bin>> byCluster = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            byCluster.computeIfAbsent(labels[i], key -> new ArrayList<>()).add(sorted.get(i));
        }

        List<List<Bin>> routes = new ArrayList<>();
        for (List<Bin> cluster : byCluster.values()) {
            appendChunks(routes, cluster, maxBinsPerRoute);
        }
        return routes;
    }

    private void appendChunks(List<List<Bin>> routes, List<Bin> cluster, int maxBinsPerRoute) {
        if (cluster.size() <= maxBinsPerRoute) {
            routes.add(new ArrayList<>(cluster));
            return;
        }
        for (int i = 0; i < cluster.size(); i += maxBinsPerRoute) {
            routes.add(new ArrayList<>(cluster.subList(i, Math.min(i + maxBinsPerRoute, cluster.size()))));
        }
    }

    private int resolveZoneCount(int pointCount) {
        int heuristic = (int) Math.ceil(Math.sqrt(pointCount / 5.0));
        return Math.max(MIN_ZONES, Math.min(MAX_ZONES, heuristic));
    }

    private int[] kMeans(List<double[]> points, int k) {
        int n = points.size();
        k = Math.max(1, Math.min(k, n));
        double[][] centroids = initCentroids(points, k);
        int[] labels = new int[n];

        for (int iter = 0; iter < K_MEANS_ITERATIONS; iter++) {
            boolean changed = false;
            for (int i = 0; i < n; i++) {
                int nearest = nearestCentroid(points.get(i), centroids);
                if (labels[i] != nearest) {
                    labels[i] = nearest;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
            centroids = recomputeCentroids(points, labels, k);
        }
        return labels;
    }

    private double[][] initCentroids(List<double[]> points, int k) {
        double[][] centroids = new double[k][2];
        int n = points.size();
        for (int i = 0; i < k; i++) {
            int idx = (i * n) / k;
            centroids[i][0] = points.get(idx)[0];
            centroids[i][1] = points.get(idx)[1];
        }
        return centroids;
    }

    private int nearestCentroid(double[] point, double[][] centroids) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int c = 0; c < centroids.length; c++) {
            double d = distSq(point, centroids[c]);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private double[][] recomputeCentroids(List<double[]> points, int[] labels, int k) {
        double[][] centroids = new double[k][2];
        int[] counts = new int[k];
        for (int i = 0; i < points.size(); i++) {
            int label = labels[i];
            centroids[label][0] += points.get(i)[0];
            centroids[label][1] += points.get(i)[1];
            counts[label]++;
        }
        for (int c = 0; c < k; c++) {
            if (counts[c] == 0) {
                centroids[c][0] = points.get(c % points.size())[0];
                centroids[c][1] = points.get(c % points.size())[1];
            } else {
                centroids[c][0] /= counts[c];
                centroids[c][1] /= counts[c];
            }
        }
        return centroids;
    }

    private double distSq(double[] a, double[] b) {
        double dLat = a[0] - b[0];
        double dLng = a[1] - b[1];
        return dLat * dLat + dLng * dLng;
    }

    private Comparator<Bin> binPriorityComparator() {
        return Comparator
                .comparingInt((Bin b) -> fillPriority(b.getStatus()))
                .thenComparing(b -> b.getPriority() != null ? b.getPriority() : "medium",
                        Comparator.comparingInt(this::priorityRank));
    }

    private int fillPriority(String status) {
        String norm = normalizeStatus(status);
        if ("full".equals(norm)) return 0;
        if ("half".equals(norm)) return 1;
        return 2;
    }

    private int priorityRank(String priority) {
        if (priority == null) return 1;
        return switch (priority.toLowerCase(Locale.ROOT)) {
            case "high" -> 0;
            case "low" -> 2;
            default -> 1;
        };
    }

    private String normalizeStatus(String status) {
        if (status == null) return "notchecked";
        return status.toLowerCase(Locale.ROOT).replace("_", "");
    }
}
