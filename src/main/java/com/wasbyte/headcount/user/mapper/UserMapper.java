package com.wasbyte.headcount.user.mapper;

import com.wasbyte.headcount.user.dto.CreateUserRequest;
import com.wasbyte.headcount.user.dto.UpdateUserProfileRequest;
import com.wasbyte.headcount.user.dto.UserResponse;
import com.wasbyte.headcount.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toUnmodifiableSet());
        return new UserResponse(
                user.getId(), user.getUsername(), user.getResourceNumber(), user.getGrade(),
                user.getFirstName(), user.getLastName(), user.getMobileNumber(), user.getEmail(),
                user.getCountry(), user.getCity(), user.getOffice(),
                user.getOrganizationUnit() == null ? null : user.getOrganizationUnit().getId(),
                user.getOrganizationUnit() == null ? null : user.getOrganizationUnit().getName(),
                user.getPosition(),
                user.getLineManager() == null ? null : user.getLineManager().getId(),
                user.getLineManager() == null ? null : fullName(user.getLineManager()),
                user.getAddress(), user.getAuthorizedPersonPhoneNumber(), user.getTimeZone(),
                user.getStatus(), user.isEnabled(), user.isEmailVerified(), roles,
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public User toEntity(CreateUserRequest request, String passwordHash) {
        User user = new User();
        applyProfile(user, request.username(), request.resourceNumber(), request.grade(),
                request.firstName(), request.lastName(), request.mobileNumber(), request.email(),
                request.country(), request.city(), request.office(), request.position(),
                request.address(), request.authorizedPersonPhoneNumber(), request.timeZone());
        user.setPasswordHash(passwordHash);
        return user;
    }

    public User toEntity(UpdateUserProfileRequest request) {
        User user = new User();
        applyProfile(user, request.username(), request.resourceNumber(), request.grade(),
                request.firstName(), request.lastName(), request.mobileNumber(), request.email(),
                request.country(), request.city(), request.office(), request.position(),
                request.address(), request.authorizedPersonPhoneNumber(), request.timeZone());
        return user;
    }

    private void applyProfile(User user, String username, String resourceNumber, String grade,
                              String firstName, String lastName, String mobileNumber, String email,
                              String country, String city, String office, String position,
                              String address, String authorizedPhone, String timeZone) {
        user.setUsername(username);
        user.setResourceNumber(resourceNumber);
        user.setGrade(grade);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setMobileNumber(mobileNumber);
        user.setEmail(email);
        user.setCountry(country);
        user.setCity(city);
        user.setOffice(office);
        user.setPosition(position);
        user.setAddress(address);
        user.setAuthorizedPersonPhoneNumber(authorizedPhone);
        user.setTimeZone(timeZone);
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
