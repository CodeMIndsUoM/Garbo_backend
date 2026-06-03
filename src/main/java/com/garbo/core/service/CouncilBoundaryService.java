package com.garbo.core.service;

import com.garbo.api.dto.CouncilBoundaryDTO;
import com.garbo.core.entity.CouncilBoundary;
import com.garbo.core.repository.CouncilBoundaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouncilBoundaryService {

    private final CouncilBoundaryRepository repository;

    public CouncilBoundaryDTO getBoundary(String council) {
        List<CouncilBoundary> points =
            repository.findByCouncilIgnoreCaseOrderByPointOrderAsc(council);

        if (points.isEmpty()) {
            return null;
        }

        // Depot is the same for all rows of a council — take from first row
        CouncilBoundary first = points.get(0);
        double depotLat = first.getDepotLat() != null ? first.getDepotLat() : 0.0;
        double depotLng = first.getDepotLng() != null ? first.getDepotLng() : 0.0;

        List<CouncilBoundaryDTO.CoordinatePoint> boundaryPoints = points.stream()
            .map(p -> new CouncilBoundaryDTO.CoordinatePoint(p.getLat(), p.getLng()))
            .collect(Collectors.toList());

        return new CouncilBoundaryDTO(council, depotLat, depotLng, boundaryPoints);
    }
}