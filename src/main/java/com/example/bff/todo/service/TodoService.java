package com.example.bff.todo.service;

import com.example.bff.common.exception.TodoNotFoundException;
import com.example.bff.todo.dto.TodoCreateRequest;
import com.example.bff.todo.dto.TodoResponse;
import com.example.bff.todo.dto.TodoUpdateRequest;
import com.example.bff.todo.entity.TodoEntity;
import com.example.bff.todo.repository.TodoRepository;
import com.example.bff.todo.mapper.TodoMapper;
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
    private final TodoMapper todoMapper;

    public TodoResponse create(UUID currentUserId, TodoCreateRequest request) {
        log.debug("Creating new todo for user: {}", currentUserId);
        Instant now = Instant.now();
        TodoEntity todo = todoMapper.toEntity(request);
        todo.setId(UUID.randomUUID());
        todo.setOwnerId(currentUserId);
        todo.setCreatedBy(currentUserId);
        todo.setUpdatedBy(currentUserId);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        TodoEntity saved = todoRepository.save(todo);
        log.info("Todo created: {} for owner {}", saved.getId(), currentUserId);
        return todoMapper.toDto(saved);
    }

    public List<TodoResponse> findAllForUser(UUID currentUserId, boolean isAdmin) {
        log.debug("Finding todos for user: {} (isAdmin: {})", currentUserId, isAdmin);
        List<TodoEntity> todos = isAdmin ? todoRepository.findAll() : todoRepository.findAllByOwnerId(currentUserId);
        return todos.stream().map(todoMapper::toDto).collect(Collectors.toList());
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
        return todoMapper.toDto(todo);
    }

    public TodoResponse update(UUID id, UUID currentUserId, boolean isAdmin, TodoUpdateRequest request) {
        log.debug("Updating todo: {} for user: {}", id, currentUserId);
        TodoEntity todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if (!isAdmin && !todo.getOwnerId().equals(currentUserId)) {
            log.warn("Update access denied to todo {} for user {}", id, currentUserId);
            throw new TodoNotFoundException(id);
        }
        todoMapper.updateEntityFromDto(request, todo);
        todo.setUpdatedBy(currentUserId);
        todo.setUpdatedAt(Instant.now());
        TodoEntity saved = todoRepository.save(todo);
        log.info("Todo updated: {}", id);
        return todoMapper.toDto(saved);
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
}
