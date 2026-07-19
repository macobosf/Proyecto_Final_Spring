package ec.edu.ups.icc.academiceventsapi.category.repository;

import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT c FROM Category c
            WHERE (:active IS NULL OR c.active = :active)
            AND (:namePattern IS NULL OR LOWER(c.name) LIKE :namePattern)
            """)
    Page<Category> search(@Param("active") Boolean active, @Param("namePattern") String namePattern, Pageable pageable);
}
