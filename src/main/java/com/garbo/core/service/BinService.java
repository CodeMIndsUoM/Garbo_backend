package com.garbo.core.service;

import com.garbo.api.dto.BinDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BinService {

    private final BinRepository binRepository;

    public BinService(BinRepository binRepository) {
        this.binRepository = binRepository;
    }

    //  Add new bin
    public Bin addBin(BinDTO dto) {
        Bin bin = new Bin();
        
        bin.setLat(dto.lat);
        bin.setLng(dto.lng);
        bin.setFillLevel(dto.fillLevel);
        bin.setPriority(dto.priority);

        return binRepository.save(bin);
    }

    //  Remove bin
    public void deleteBin(Long id) {
        binRepository.deleteById(id);
    }

    //  Get all bins (for map)
    public List<Bin> getAllBins() {
        return binRepository.findAll();
    }

    //  Get bin details
    public Bin getBinById(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bin not found"));
    }

    //  Update bin priority
    public Bin updatePriority(Long id, String priority) {
        Bin bin = getBinById(id);
        bin.setPriority(priority);
        return binRepository.save(bin);
    }

    
}
