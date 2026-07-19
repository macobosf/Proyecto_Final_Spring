package ec.edu.ups.icc.academiceventsapi.category.repository;

import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);
}
