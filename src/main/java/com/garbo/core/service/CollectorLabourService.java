package com.garbo.core.service;

import com.garbo.core.entity.CollectorLabour;
import com.garbo.core.repository.CollectorLabourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectorLabourService {

    private final CollectorLabourRepository labourRepository;

    public List<CollectorLabour> getAll() {
        return labourRepository.findAll();
    }

    public List<CollectorLabour> findByCouncil(String council) {
        return labourRepository.findByCouncilIgnoreCase(council);
    }

    public CollectorLabour save(CollectorLabour labour) {
        return labourRepository.save(labour);
    }

    public void delete(Long id) {
        labourRepository.deleteById(id);
    }
}
