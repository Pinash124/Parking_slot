package com.smartparking.controller;

import com.smartparking.model.requests.UserResponse;
import com.smartparking.model.requests.UserRoleUpdateRequest;
import com.smartparking.model.requests.UserStatusUpdateRequest;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ManagerService managerService;
    private final UserRepository userRepository;

    public AdminController(ManagerService managerService,
                           UserRepository userRepository) {
        this.managerService = managerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(
                userRepository.findAll()
                        .stream()
                        .map(UserResponse::from)
                        .toList());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.from(managerService.updateUserRole(id, request)));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.from(managerService.updateUserStatus(id, request)));
    }

}
