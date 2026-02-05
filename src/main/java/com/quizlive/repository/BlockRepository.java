package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.Block;
import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {
    // Para listar "Mis Bloques", necesitamos buscar por Owner ID
    List<Block> findByOwnerId(Long ownerId);
}
