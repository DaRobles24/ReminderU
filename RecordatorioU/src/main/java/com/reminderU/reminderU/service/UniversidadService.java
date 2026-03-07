package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Universidad;
import com.reminderU.reminderU.repository.UniversidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UniversidadService {

    private final UniversidadRepository universidadRepository;

    public UniversidadService(UniversidadRepository universidadRepository) {
        this.universidadRepository = universidadRepository;
    }

    public List<Universidad> listarActivas() {
        return universidadRepository.findByActivaTrue();
    }

    public Optional<Universidad> buscarPorId(Long id) {
        return universidadRepository.findById(id);
    }
}