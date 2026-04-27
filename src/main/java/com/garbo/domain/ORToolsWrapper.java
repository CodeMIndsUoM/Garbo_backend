package com.garbo.domain;

import com.garbo.core.entity.Bin;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import com.google.protobuf.Duration;

import java.util.*;

public class ORToolsWrapper {

    private static final int SOLVER_TIME_LIMIT_SECONDS = 8;

    static {
        Loader.loadNativeLibraries();
    }

    public Map<Integer, List<Long>> solve(
            double[][] matrix,
            List<Bin> bins,
            int vehicleCount,
            int[] capacities
    ) {

        int depot = 0;
        int nodeCount = matrix.length;

        RoutingIndexManager manager =
                new RoutingIndexManager(nodeCount, vehicleCount, depot);

        RoutingModel routing = new RoutingModel(manager);

        // COST
        final int transitIndex = routing.registerTransitCallback((long from, long to) -> {
            int i = manager.indexToNode(from);
            int j = manager.indexToNode(to);
            return (long) matrix[i][j];
        });

        routing.setArcCostEvaluatorOfAllVehicles(transitIndex);

        // DEMAND
        final int demandIndex = routing.registerUnaryTransitCallback(from -> {
            int node = manager.indexToNode(from);
            return node == depot ? 0 : 1;
        });

        long[] caps = Arrays.stream(capacities).asLongStream().toArray();

        routing.addDimensionWithVehicleCapacity(
                demandIndex,
                0,
                caps,
                true,
                "Capacity"
        );

        // TIME
        routing.addDimension(
                transitIndex,
                0,
                999999,
                true,
                "Time"
        );

        RoutingSearchParameters params =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(
                                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .setLocalSearchMetaheuristic(
                                LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(Duration.newBuilder().setSeconds(SOLVER_TIME_LIMIT_SECONDS).build())
                        .build();

        Assignment sol = routing.solveWithParameters(params);

        Map<Integer, List<Long>> result = new LinkedHashMap<>();

        if (sol == null) return result;

        for (int v = 0; v < vehicleCount; v++) {

            List<Long> route = new ArrayList<>();
            long index = routing.start(v);

            while (!routing.isEnd(index)) {

                int node = manager.indexToNode(index);

                if (node != depot) {
                    route.add(bins.get(node - 1).getId());
                }

                index = sol.value(routing.nextVar(index));
            }

            if (!route.isEmpty()) {
                result.put(v, route);
            }
        }

        return result;
    }
}



















