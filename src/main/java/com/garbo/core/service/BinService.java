package com.garbo.core.service;

import com.garbo.api.dto.BinDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.event.BinChangedEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BinService {

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    //  Add new bin
    public Bin addBin(BinDTO dto) {
        Bin bin = new Bin();   
        bin.setLat(dto.getLat());
        bin.setLng(dto.getLng());
        bin.setFillLevel(dto.getFillLevel());
        bin.setPriority(dto.getPriority());
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        return saved;
    }

    //  Remove bin
    public void deleteBin(Long id) {
        binRepository.deleteById(id);
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
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
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", saved.getId()));
        return saved;
    } 
}
