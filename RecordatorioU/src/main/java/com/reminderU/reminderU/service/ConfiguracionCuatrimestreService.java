package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.ConfiguracionCuatrimestre;
import com.reminderU.reminderU.repository.ConfiguracionCuatrimestreRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ConfiguracionCuatrimestreService {

    private final ConfiguracionCuatrimestreRepository repo;

    public ConfiguracionCuatrimestreService(ConfiguracionCuatrimestreRepository repo) {
        this.repo = repo;
    }

    public ConfiguracionCuatrimestre getConfiguracion() {
        Optional<ConfiguracionCuatrimestre> opt = repo.findTopByOrderByIdAsc();
        return opt.orElse(null);
    }

    public ConfiguracionCuatrimestre save(ConfiguracionCuatrimestre c) {
        return repo.save(c);
    }
}
