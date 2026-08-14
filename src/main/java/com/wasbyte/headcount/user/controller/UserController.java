package com.wasbyte.headcount.user.controller;

import com.wasbyte.headcount.organization.service.OrganizationUnitService;
import com.wasbyte.headcount.user.dto.AssignLineManagerRequest;
import com.wasbyte.headcount.user.dto.AssignOrganizationUnitRequest;
import com.wasbyte.headcount.user.dto.ChangeUserStatusRequest;
import com.wasbyte.headcount.user.dto.CreateUserRequest;
import com.wasbyte.headcount.user.dto.RoleAssignmentRequest;
import com.wasbyte.headcount.user.dto.SetUserActiveRequest;
import com.wasbyte.headcount.user.dto.UpdateUserProfileRequest;
import com.wasbyte.headcount.user.dto.UserResponse;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.mapper.UserMapper;
import com.wasbyte.headcount.user.service.RoleService;
import com.wasbyte.headcount.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final OrganizationUnitService organizationUnitService;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, RoleService roleService,
                          OrganizationUnitService organizationUnitService,
                          UserMapper mapper, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.organizationUnitService = organizationUnitService;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return mapper.toResponse(userService.findById(id));
    }

    @GetMapping("/by-username/{username}")
    public UserResponse getByUsername(@PathVariable String username) {
        return mapper.toResponse(userService.findByUsername(username));
    }

    @GetMapping("/by-email")
    public UserResponse getByEmail(@RequestParam String email) {
        return mapper.toResponse(userService.findByEmail(email));
    }

    @GetMapping("/by-resource-number/{resourceNumber}")
    public UserResponse getByResourceNumber(@PathVariable String resourceNumber) {
        return mapper.toResponse(userService.findByResourceNumber(resourceNumber));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        User user = mapper.toEntity(request, passwordEncoder.encode(request.password()));
        if (request.organizationUnitId() != null) {
            user.setOrganizationUnit(organizationUnitService.findById(request.organizationUnitId()));
        }
        if (request.lineManagerId() != null) {
            user.setLineManager(userService.findById(request.lineManagerId()));
        }
        return mapper.toResponse(userService.create(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserProfileRequest request) {
        return mapper.toResponse(userService.updateProfile(id, mapper.toEntity(request)));
    }

    @PatchMapping("/{id}/organization-unit")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignOrganizationUnit(@PathVariable Long id,
                                               @RequestBody AssignOrganizationUnitRequest request) {
        return mapper.toResponse(userService.assignOrganizationUnit(id, request.organizationUnitId()));
    }

    @PatchMapping("/{id}/line-manager")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignLineManager(@PathVariable Long id,
                                          @RequestBody AssignLineManagerRequest request) {
        return mapper.toResponse(userService.assignLineManager(id, request.lineManagerId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody ChangeUserStatusRequest request) {
        return mapper.toResponse(userService.changeStatus(id, request.status()));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse setActive(@PathVariable Long id,
                                  @Valid @RequestBody SetUserActiveRequest request) {
        return mapper.toResponse(userService.setAccountActive(id, request.active()));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse addRole(@PathVariable Long id,
                                @Valid @RequestBody RoleAssignmentRequest request) {
        return mapper.toResponse(roleService.addRole(id, request.role()));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse removeRole(@PathVariable Long id, @PathVariable String roleName) {
        return mapper.toResponse(roleService.removeRole(id, roleName));
    }
}
