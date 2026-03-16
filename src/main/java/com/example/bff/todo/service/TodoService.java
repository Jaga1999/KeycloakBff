package com.example.bff.todo.service;

import com.example.bff.common.exception.TodoNotFoundException;
import com.example.bff.todo.dto.TodoCreateRequest;
import com.example.bff.todo.dto.TodoResponse;
import com.example.bff.todo.dto.TodoUpdateRequest;
import com.example.bff.todo.entity.TodoEntity;
import com.example.bff.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoResponse create(UUID currentUserId, TodoCreateRequest request) {
        log.debug("Creating new todo for user: {}", currentUserId);
        Instant now = Instant.now();
        TodoEntity todo = new TodoEntity();
        todo.setId(UUID.randomUUID());
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setOwnerId(currentUserId);
        todo.setCreatedBy(currentUserId);
        todo.setUpdatedBy(currentUserId);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        TodoEntity saved = todoRepository.save(todo);
        log.info("Todo created: {} for owner {}", saved.getId(), currentUserId);
        return toResponse(saved);
    }

    public List<TodoResponse> findAllForUser(UUID currentUserId, boolean isAdmin) {
        log.debug("Finding todos for user: {} (isAdmin: {})", currentUserId, isAdmin);
        List<TodoEntity> todos = isAdmin ? todoRepository.findAll() : todoRepository.findAllByOwnerId(currentUserId);
        return todos.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TodoResponse findByIdForUser(UUID id, UUID currentUserId, boolean isAdmin) {
        log.trace("Finding todo by ID: {} for user: {}", id, currentUserId);
        TodoEntity todo = todoRepository.findById(id).orElseThrow(() -> {
            log.warn("Todo {} not found", id);
            return new TodoNotFoundException(id);
        });
        if (!isAdmin && !todo.getOwnerId().equals(currentUserId)) {
            log.warn("Access denied to todo {} for user {}", id, currentUserId);
            throw new TodoNotFoundException(id);
        }
        return toResponse(todo);
    }

    public TodoResponse update(UUID id, UUID currentUserId, boolean isAdmin, TodoUpdateRequest request) {
        log.debug("Updating todo: {} for user: {}", id, currentUserId);
        TodoEntity todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if (!isAdmin && !todo.getOwnerId().equals(currentUserId)) {
            log.warn("Update access denied to todo {} for user {}", id, currentUserId);
            throw new TodoNotFoundException(id);
        }
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setUpdatedBy(currentUserId);
        todo.setUpdatedAt(Instant.now());
        TodoEntity saved = todoRepository.save(todo);
        log.info("Todo updated: {}", id);
        return toResponse(saved);
    }

    public void delete(UUID id, UUID currentUserId, boolean isAdmin) {
        log.debug("Deleting todo: {} by user: {}", id, currentUserId);
        TodoEntity todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if (!isAdmin && !todo.getOwnerId().equals(currentUserId)) {
            log.warn("Delete access denied to todo {} for user {}", id, currentUserId);
            throw new TodoNotFoundException(id);
        }
        todoRepository.delete(todo);
        log.info("Todo deleted: {}", id);
    }

    private TodoResponse toResponse(TodoEntity todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getOwnerId(),
                todo.getCreatedBy(),
                todo.getUpdatedBy(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
