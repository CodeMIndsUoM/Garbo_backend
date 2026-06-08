package com.garbo.core.service;

import com.garbo.core.entity.TaskFamily;
import com.garbo.core.repository.TaskFamilyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskFamilyService {

    private final TaskFamilyRepository taskFamilyRepository;

    public TaskFamilyService(TaskFamilyRepository taskFamilyRepository) {
        this.taskFamilyRepository = taskFamilyRepository;
    }

    public List<TaskFamily> getAll() {
        return taskFamilyRepository.findAll();
    }

    public Optional<TaskFamily> getById(Long id) {
        return taskFamilyRepository.findById(id);
    }

    @Transactional
    public TaskFamily create(String code, String name, String description) {
        TaskFamily family = new TaskFamily();
        family.setCode(code.trim().toUpperCase());
        family.setName(name.trim());
        family.setDescription(description);
        return taskFamilyRepository.save(family);
    }

    @Transactional
    public Optional<TaskFamily> update(Long id, String code, String name, String description) {
        return taskFamilyRepository.findById(id).map(family -> {
            if (code != null && !code.isBlank()) {
                family.setCode(code.trim().toUpperCase());
            }
            if (name != null && !name.isBlank()) {
                family.setName(name.trim());
            }
            if (description != null) {
                family.setDescription(description);
            }
            return taskFamilyRepository.save(family);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!taskFamilyRepository.existsById(id)) {
            return false;
        }
        taskFamilyRepository.deleteById(id);
        return true;
    }
}
