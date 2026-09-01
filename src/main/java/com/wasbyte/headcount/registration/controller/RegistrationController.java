package com.wasbyte.headcount.registration.controller;

import com.wasbyte.headcount.registration.dto.EmployeeRegistrationRequest;
import com.wasbyte.headcount.registration.dto.RegistrationOrganizationUnitResponse;
import com.wasbyte.headcount.registration.dto.RegistrationResponse;
import com.wasbyte.headcount.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody EmployeeRegistrationRequest request) {
        return registrationService.register(request);
    }

    @GetMapping("/organization-units")
    public List<RegistrationOrganizationUnitResponse> organizationUnits() {
        return registrationService.findActiveOrganizationUnits();
    }
}
