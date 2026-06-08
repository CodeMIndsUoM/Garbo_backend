package com.garbo.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.CouncilBoundaryDTO;
import com.garbo.core.entity.CouncilBoundary;
import com.garbo.core.repository.CouncilBoundaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouncilBoundaryService {

    private final CouncilBoundaryRepository repository;
    private final ObjectMapper objectMapper;

    public CouncilBoundaryDTO getBoundary(String council) {
        Optional<CouncilBoundary> boundaryOpt = repository.findByCouncilIgnoreCase(council);
        if (boundaryOpt.isEmpty()) {
            return null;
        }

        CouncilBoundary boundary = boundaryOpt.get();
        double depotLat = boundary.getDepotLat() != null ? boundary.getDepotLat() : 0.0;
        double depotLng = boundary.getDepotLng() != null ? boundary.getDepotLng() : 0.0;

        List<CouncilBoundaryDTO.CoordinatePoint> boundaryPoints;
        try {
            boundaryPoints = objectMapper.readValue(
                boundary.getBoundaryPoints(),
                new TypeReference<List<CouncilBoundaryDTO.CoordinatePoint>>() {}
            );
        } catch (Exception e) {
            boundaryPoints = new ArrayList<>();
        }

        return new CouncilBoundaryDTO(council, depotLat, depotLng, boundaryPoints);
    }
}