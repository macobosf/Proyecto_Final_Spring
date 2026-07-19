package ec.edu.ups.icc.academiceventsapi.user.mapper;

import ec.edu.ups.icc.academiceventsapi.user.dto.UserResponse;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import ec.edu.ups.icc.academiceventsapi.user.entity.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus().name(),
                user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .map(role -> role.getName().name())
                        .toList(),
                user.getCreatedAt()
        );
    }
}
