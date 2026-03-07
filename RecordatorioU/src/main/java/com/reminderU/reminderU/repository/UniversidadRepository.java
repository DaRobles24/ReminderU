package com.reminderU.reminderU.repository;

import com.reminderU.reminderU.modelo.Universidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UniversidadRepository extends JpaRepository<Universidad, Long> {

    List<Universidad> findByActivaTrue();
}