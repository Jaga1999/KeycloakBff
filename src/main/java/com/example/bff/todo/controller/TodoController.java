package com.example.bff.todo.controller;

import com.example.bff.common.api.ApiResponse;
import com.example.bff.todo.dto.TodoCreateRequest;
import com.example.bff.todo.dto.TodoResponse;
import com.example.bff.todo.dto.TodoUpdateRequest;
import com.example.bff.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
@Tag(name = "Todos", description = "Todo management with owner and admin access control")
@PreAuthorize("isAuthenticated()")
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    @Operation(summary = "Create todo", description = "Creates a new todo owned by the current user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Todo created",
            content = @Content(schema = @Schema(implementation = TodoResponse.class)))
    public ResponseEntity<ApiResponse<TodoResponse>> create(
            Authentication authentication,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        log.info("Creating todo for user: {}", currentUserId);
        TodoResponse response = todoService.create(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, HttpStatus.CREATED.value(), "Todo created", response));
    }

    @GetMapping
    @Operation(summary = "List todos", description = "For admins: returns all todos. For users: returns only their own todos.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Todos listed",
            content = @Content(schema = @Schema(implementation = TodoResponse.class)))
    public ResponseEntity<ApiResponse<List<TodoResponse>>> list(Authentication authentication) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        log.debug("Listing todos for user: {} (isAdmin: {})", currentUserId, isAdmin);
        List<TodoResponse> todos = todoService.findAllForUser(currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK.value(), "Todos", todos));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get todo by id", description = "Admin can access any todo; user can access only their own.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Todo returned",
            content = @Content(schema = @Schema(implementation = TodoResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Todo not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<TodoResponse>> getById(Authentication authentication, @PathVariable UUID id) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        log.debug("Fetching todo {} for user: {} (isAdmin: {})", id, currentUserId, isAdmin);
        TodoResponse response = todoService.findByIdForUser(id, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK.value(), "Todo", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update todo", description = "Only admin or owner can update a todo.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Todo updated",
            content = @Content(schema = @Schema(implementation = TodoResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Todo not found or not accessible",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<TodoResponse>> update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        log.info("Updating todo {} for user: {} (isAdmin: {})", id, currentUserId, isAdmin);
        TodoResponse response = todoService.update(id, currentUserId, isAdmin, request);
        return ResponseEntity.ok(ApiResponse.success(null, HttpStatus.OK.value(), "Todo updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete todo", description = "Only admin or owner can delete a todo.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Todo deleted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Todo not found or not accessible",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        UUID currentUserId = (UUID) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);
        log.info("Deleting todo {} for user: {} (isAdmin: {})", id, currentUserId, isAdmin);
        todoService.delete(id, currentUserId, isAdmin);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, HttpStatus.NO_CONTENT.value(), "Todo deleted", null));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
