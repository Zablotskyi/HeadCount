package com.wasbyte.headcount.registration.service;

import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.service.OrganizationUnitService;
import com.wasbyte.headcount.registration.dto.EmployeeRegistrationRequest;
import com.wasbyte.headcount.registration.dto.RegistrationOrganizationUnitResponse;
import com.wasbyte.headcount.registration.dto.RegistrationResponse;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.entity.UserStatus;
import com.wasbyte.headcount.user.service.RoleService;
import com.wasbyte.headcount.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistrationService {

    private static final String EMPLOYEE_ROLE = "EMPLOYEE";

    private final UserService userService;
    private final RoleService roleService;
    private final OrganizationUnitService organizationUnitService;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserService userService, RoleService roleService,
                               OrganizationUnitService organizationUnitService,
                               PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.organizationUnitService = organizationUnitService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(EmployeeRegistrationRequest request) {
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new InvalidOperationException("Паролі не збігаються");
        }
        OrganizationUnit unit = organizationUnitService.findById(request.organizationUnitId());
        if (!unit.isActive()) {
            throw new InvalidOperationException("Обраний організаційний підрозділ неактивний");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setResourceNumber(request.resourceNumber());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPosition(request.position());
        user.setEmail(request.email());
        user.setMobileNumber(request.mobileNumber());
        user.setCountry(request.country());
        user.setCity(request.city());
        user.setOffice(request.office());
        user.setAddress(request.address());
        user.setAuthorizedPersonPhoneNumber(request.authorizedPersonPhoneNumber());
        user.setTimeZone(request.timeZone());
        user.setOrganizationUnit(unit);
        user.setStatus(UserStatus.PENDING_APPROVAL);
        user.setEnabled(false);
        user.getRoles().add(roleService.findByName(EMPLOYEE_ROLE));

        User created = userService.create(user);
        return new RegistrationResponse(created.getId(), created.getUsername(), created.getStatus());
    }

    public List<RegistrationOrganizationUnitResponse> findActiveOrganizationUnits() {
        List<RegistrationOrganizationUnitResponse> result = new ArrayList<>();
        for (OrganizationUnit root : organizationUnitService.getRoots()) {
            appendActiveUnits(root, 0, result);
        }
        return result;
    }

    private void appendActiveUnits(OrganizationUnit unit, int depth,
                                   List<RegistrationOrganizationUnitResponse> result) {
        if (unit.isActive()) {
            result.add(new RegistrationOrganizationUnitResponse(
                    unit.getId(), unit.getName(), unit.getCode(), unit.getType(),
                    unit.getParent() == null ? null : unit.getParent().getId(), depth));
        }
        for (OrganizationUnit child : organizationUnitService.getChildren(unit.getId())) {
            appendActiveUnits(child, depth + 1, result);
        }
    }
}
