package ec.edu.ups.icc.academiceventsapi.category.service;

import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryRequest;
import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    CategoryResponse getById(Long id);

    Page<CategoryResponse> list(Boolean active, String search, Pageable pageable);
}
