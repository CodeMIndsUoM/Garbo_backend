package com.garbo.api.controller;

import com.garbo.api.dto.RouteRequestDTO;
<<<<<<< HEAD
import com.garbo.api.dto.RouteResponseDTO;
import com.garbo.api.dto.RouteResponseDTO.BinStop;
import com.garbo.api.dto.RouteResponseDTO.VehicleRoute;
import com.garbo.core.domain.algorithm.ORToolsWrapper;
import com.garbo.core.domain.algorithm.OSRMClient;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
=======
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.service.route.RouteSessionService;
>>>>>>> kevin-RWS
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< HEAD
import java.util.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private BinRepository binRepository;

    
    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoutes(@RequestBody RouteRequestDTO request) {
        try {
            // 1. Validate depot
            if (!request.hasValidDepot()) {
                return ResponseEntity.badRequest().body(
                        "Missing depot location. Please provide 'depotLat' and 'depotLng'.");
            }

            // 2. Load bins from DB
            List<Bin> bins = binRepository.findAll();
            if (bins.isEmpty()) {
                return ResponseEntity.badRequest().body("No bins found in the database.");
            }
            System.out.println("[Route] Loaded " + bins.size() + " bins from DB.");

            // 3. Build coords: index 0 = depot, indices 1..n = bins
            int totalNodes = bins.size() + 1;
            double[][] coords = new double[totalNodes][2];
            coords[0][0] = request.getDepotLat();
            coords[0][1] = request.getDepotLng();
            for (int i = 0; i < bins.size(); i++) {
                coords[i + 1][0] = bins.get(i).getLat();
                coords[i + 1][1] = bins.get(i).getLng();
            }

            // 4. Get duration matrix from OSRM
            System.out.println("[OSRM] Fetching duration matrix for " + totalNodes + " nodes...");
            double[][] durationMatrix = OSRMClient.getDurationMatrix(coords);
            System.out.println("[OSRM] Duration matrix received.");

            // 5. Validate vehicle inputs
            int vehicleCount = request.getVehicleCount() > 0 ? request.getVehicleCount() : 1;
            int[] vehicleCapacities = request.getValidatedCapacities();

            // 6. Solve VRP — returns vehicleId -> ordered list of bin IDs
            ORToolsWrapper vrp = new ORToolsWrapper();
            Map<Integer, List<Long>> rawRoutes = vrp.solve(
                    durationMatrix, bins, vehicleCount, vehicleCapacities);

            // 7. Build detailed response with bin sequence per vehicle
            //    Quick lookup: binId -> Bin object
            Map<Long, Bin> binLookup = new HashMap<>();
            for (Bin bin : bins) binLookup.put(bin.getId(), bin);

            //    Quick lookup: binId -> node index in duration matrix
            Map<Long, Integer> binIdToNodeIndex = new HashMap<>();
            for (int i = 0; i < bins.size(); i++) {
                binIdToNodeIndex.put(bins.get(i).getId(), i + 1); // node 0 is depot
            }

            Map<Integer, VehicleRoute> detailedRoutes = new LinkedHashMap<>();
            int vehiclesUsed = 0;

            for (int v = 0; v < vehicleCount; v++) {
                List<Long> binIds = rawRoutes.getOrDefault(v, Collections.emptyList());

                // Skip vehicles with no assigned bins
                if (binIds.isEmpty()) continue;
                vehiclesUsed++;

                List<BinStop> binSequence = new ArrayList<>();
                double totalDuration = 0.0;

                for (int s = 0; s < binIds.size(); s++) {
                    long binId = binIds.get(s);
                    Bin bin = binLookup.get(binId);
                    int nodeIndex = binIdToNodeIndex.get(binId);

                    // Travel time from previous stop (or depot if first stop)
                    double durationFromPrev;
                    if (s == 0) {
                        // depot (node 0) -> first bin
                        durationFromPrev = durationMatrix[0][nodeIndex];
                    } else {
                        // previous bin -> current bin
                        int prevNodeIndex = binIdToNodeIndex.get(binIds.get(s - 1));
                        durationFromPrev = durationMatrix[prevNodeIndex][nodeIndex];
                    }

                    totalDuration += durationFromPrev;

                    binSequence.add(new BinStop(
                            s + 1,          // stopOrder is 1-based
                            binId,
                            bin.getLat(),
                            bin.getLng(),
                            durationFromPrev
                    ));
                }

                // Add return trip to depot
                int lastNodeIndex = binIdToNodeIndex.get(binIds.get(binIds.size() - 1));
                totalDuration += durationMatrix[lastNodeIndex][0];

                detailedRoutes.put(v, new VehicleRoute(
                        v,
                        vehicleCapacities[v],
                        totalDuration,
                        binSequence
                ));
            }

            System.out.println("[Route] Optimization complete. Vehicles used: " + vehiclesUsed);
            return ResponseEntity.ok(new RouteResponseDTO(vehiclesUsed, detailedRoutes));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error optimizing routes: " + e.getMessage());
=======
@RestController
@RequestMapping("/api/routes")
@CrossOrigin("*")
public class RouteController {

    @Autowired
    private RouteSessionService routeSessionService;

    private static final long DEFAULT_USER_ID = 42L;

    /**
     * Internally converts request → session-based optimization pipeline
     * and broadcasts real-time updates via WebSocket
     */
    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoutes(@RequestBody RouteRequestDTO request) {

        try {
            // 1. Convert legacy request → session request
            RouteSessionCreateRequestDTO sessionRequest = new RouteSessionCreateRequestDTO();

            // user handling (fallback if not provided)
            sessionRequest.setUserId(
                    request.getUserId() != null
                            ? request.getUserId()
                            : DEFAULT_USER_ID
            );

            // routing configuration
            sessionRequest.setVehicleCount(request.getVehicleCount());
            sessionRequest.setVehicleCapacities(request.getVehicleCapacities());

            // depot location
            sessionRequest.setDepotLat(request.getDepotLat());
            sessionRequest.setDepotLng(request.getDepotLng());

            // IMPORTANT: selected bins from admin dashboard
            sessionRequest.setSelectedBinIds(request.getSelectedBinIds());

            // 2. Call session-based optimization engine
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.optimizeAndBroadcast(sessionRequest);

            // 3. Return latest snapshot (also pushed via WebSocket internally)
            return ResponseEntity.ok(snapshot);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body("Error optimizing routes: " + e.getMessage());
>>>>>>> kevin-RWS
        }
    }
}