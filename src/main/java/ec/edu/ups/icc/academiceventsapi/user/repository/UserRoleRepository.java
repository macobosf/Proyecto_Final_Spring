package ec.edu.ups.icc.academiceventsapi.user.repository;

import ec.edu.ups.icc.academiceventsapi.user.entity.UserRole;
import ec.edu.ups.icc.academiceventsapi.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
