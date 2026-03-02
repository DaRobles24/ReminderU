package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Rubro;
import com.reminderU.reminderU.repository.RubroRepository;
import com.reminderU.reminderU.service.RubroService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RubroServiceImpl implements RubroService {

    private final RubroRepository rubroRepository;

    public RubroServiceImpl(RubroRepository rubroRepository) {
        this.rubroRepository = rubroRepository;
    }

    @Override
    public List<Rubro> listarRubros() {
        return rubroRepository.findAll();
    }

    @Override
    public Optional<Rubro> buscarPorId(Long id) {
        return rubroRepository.findById(id);
    }

    @Override
    public Rubro guardarRubro(Rubro rubro) {
        return rubroRepository.save(rubro);
    }

    @Override
    public void eliminarRubro(Long id) {
        rubroRepository.deleteById(id);
    }
}
