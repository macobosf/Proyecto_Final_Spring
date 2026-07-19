package ec.edu.ups.icc.academiceventsapi.user.repository;

import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
