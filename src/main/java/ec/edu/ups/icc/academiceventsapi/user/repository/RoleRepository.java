package ec.edu.ups.icc.academiceventsapi.user.repository;

import ec.edu.ups.icc.academiceventsapi.user.entity.Role;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
