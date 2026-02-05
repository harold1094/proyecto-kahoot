package com.quizlive.service;

import com.quizlive.model.Block;
import com.quizlive.model.User;
import com.quizlive.repository.BlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BlockService {

    @Autowired
    private BlockRepository blockRepository;

    public List<Block> getBlocksByUser(User user) {
        return blockRepository.findByOwnerId(user.getId());
    }

    public Block getBlockById(Long id) {
        return blockRepository.findById(id).orElse(null);
    }
    
    public void saveBlock(Block block, User owner) {
        if (block.getId() != null) {
            // Edición: Recuperamos el original para no perder las preguntas
            Block existingBlock = blockRepository.findById(block.getId()).orElse(null);
            if (existingBlock != null) {
                existingBlock.setName(block.getName());
                existingBlock.setDescription(block.getDescription());
                // No tocamos existingBlock.setQuestions(...) asi las mantenemos
                blockRepository.save(existingBlock);
                return;
            }
        }
        // Creación nueva
        block.setOwner(owner); 
        blockRepository.save(block);
    }

    public void deleteBlock(Long blockId) {
        // Aquí podríamos validar que el usuario es el dueño antes de borrar
        blockRepository.deleteById(blockId);
    }
}
