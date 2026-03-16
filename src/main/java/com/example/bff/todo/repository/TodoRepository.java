package com.example.bff.todo.repository;

import com.example.bff.todo.entity.TodoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

    List<TodoEntity> findAllByOwnerId(UUID ownerId);

    Optional<TodoEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}

