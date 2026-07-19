package ec.edu.ups.icc.academiceventsapi.category.mapper;

import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryResponse;
import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
