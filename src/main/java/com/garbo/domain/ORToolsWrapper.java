package com.garbo.domain;

import com.garbo.core.entity.Bin;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;

import java.util.*;

public class ORToolsWrapper {

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



















/*package com.garbo.domain;

import com.garbo.core.entity.Bin;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import com.google.protobuf.Duration;
import java.util.*;

public class ORToolsWrapper {

    static {
        Loader.loadNativeLibraries();
    }

    /**
     * Solve the Vehicle Routing Problem using OR-Tools.
     *
     * @param durationMatrix  n x n matrix (index 0 = depot, 1..n = bins)
     * @param bins            list of bins from DB
     * @param vehicleCount    number of vehicles
     * @param capacities      capacity per vehicle
     * @return vehicleId -> ordered list of bin IDs
     */  /* 
    public Map<Integer, List<Long>> solve(
            double[][] durationMatrix,
            List<Bin> bins,
            int vehicleCount,
            int[] capacities
    ) {
        int nodeCount = durationMatrix.length; // depot + bins
        int depot = 0;

        // 1. Create Routing Index Manager 
        // Nodes: 0 = depot, 1..bins.size() = bins
        RoutingIndexManager manager = new RoutingIndexManager(
                nodeCount,
                vehicleCount,
                depot
        );

        // 2. Create Routing Model 
        RoutingModel routing = new RoutingModel(manager);

        // 3. Register Distance/Duration Callback
        // OR-Tools uses long (integer), so we scale duration seconds by 100
        final long[][] scaledMatrix = new long[nodeCount][nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                scaledMatrix[i][j] = Math.round(durationMatrix[i][j] * 100);
            }
        }

        final int transitCallbackIndex = routing.registerTransitCallback(
                (long fromIndex, long toIndex) -> {
                    int from = manager.indexToNode((int) fromIndex);
                    int to = manager.indexToNode((int) toIndex);
                    return scaledMatrix[from][to];
                }
        );

        // Set the cost (arc cost) evaluator for all vehicles
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        //  4. Add Capacity Constraint 
        // Each bin has a demand of 1 (you can change this to bin.getFillLevel() etc.)
        final int demandCallbackIndex = routing.registerUnaryTransitCallback(
                (long fromIndex) -> {
                    int node = manager.indexToNode((int) fromIndex);
                    return node == depot ? 0 : 1; // depot has 0 demand, each bin = 1
                }
        );

        long[] vehicleCapacitiesLong = new long[vehicleCount];
        for (int i = 0; i < vehicleCount; i++) {
            vehicleCapacitiesLong[i] = capacities[i];
        }

        routing.addDimensionWithVehicleCapacity(
                demandCallbackIndex,
                0,                      // null slack
                vehicleCapacitiesLong,  // per-vehicle capacities
                true,                   // start cumul at zero
                "Capacity"
        );

        //  5. Add Time Dimension (optional but improves solution quality)
        routing.addDimension(
                transitCallbackIndex,
                0,          // no waiting time
                999999999,  // max time per vehicle
                true,       // start cumul at zero
                "Time"
        );

        //  6. Set Search Parameters 
        RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                        .setTimeLimit(Duration.newBuilder().setSeconds(10).build()) // 10 sec limit
                        .build();

        // 7. Solve 
        Assignment solution = routing.solveWithParameters(searchParameters);

        // 8. Extract Routes 
        Map<Integer, List<Long>> routes = new LinkedHashMap<>();

        if (solution == null) {
            System.err.println("[ORTools] No solution found! Check if bins are reachable or increase vehicle count/capacity.");
            // Return empty routes per vehicle so response is still valid
            for (int v = 0; v < vehicleCount; v++) {
                routes.put(v, new ArrayList<>());
            }
            return routes;
        }

        System.out.println("[ORTools] Solution found!");

        for (int v = 0; v < vehicleCount; v++) {
            List<Long> binIds = new ArrayList<>();
            long index = routing.start(v);

            while (!routing.isEnd(index)) {
                int nodeIndex = manager.indexToNode((int) index);

                // Skip depot node (index 0)
                if (nodeIndex != depot) {
                    // Convert node index back to bin ID
                    // Node 1 = bins.get(0), Node 2 = bins.get(1), etc.
                    Bin bin = bins.get(nodeIndex - 1);
                    binIds.add(bin.getId());
                }

                index = solution.value(routing.nextVar(index));
            }

            // Only include vehicles that were actually assigned bins
            if (!binIds.isEmpty()) {
                routes.put(v, binIds);
                System.out.println("[ORTools] Vehicle " + v + " -> " + binIds.size() + " bins: " + binIds);
            }
        }

        return routes;
    }
}
*/