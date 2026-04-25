package com.garbo.core.service;

import com.garbo.core.entity.BinCollector;
import com.garbo.core.repository.BinCollectorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BinCollectorService {

    private final BinCollectorRepository binCollectorRepository;

    public BinCollectorService(BinCollectorRepository binCollectorRepository) {
        this.binCollectorRepository = binCollectorRepository;
    }

    public BinCollector saveBinCollector(BinCollector b) {
        return this.binCollectorRepository.save(b);
    }

    public List<BinCollector> getAll() {
        return this.binCollectorRepository.findAll();
    }

    public List<BinCollector> findByCouncil(String council) {
        return this.binCollectorRepository.findByAssignedCouncil(council);
    }
}
