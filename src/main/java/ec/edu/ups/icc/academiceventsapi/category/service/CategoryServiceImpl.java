package ec.edu.ups.icc.academiceventsapi.category.service;

import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryRequest;
import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryResponse;
import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import ec.edu.ups.icc.academiceventsapi.category.mapper.CategoryMapper;
import ec.edu.ups.icc.academiceventsapi.category.repository.CategoryRepository;
import ec.edu.ups.icc.academiceventsapi.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe una categoría con ese nombre.");
        }

        Category category = new Category(request.name(), request.description());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findByIdOrThrow(id);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Ya existe una categoría con ese nombre.");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findByIdOrThrow(id);
        category.setActive(false);
    }

    @Override
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findByIdOrThrow(id));
    }

    @Override
    public Page<CategoryResponse> list(Boolean active, String search, Pageable pageable) {
        String namePattern = (search == null || search.isBlank()) ? null : "%" + search.toLowerCase() + "%";
        return categoryRepository.search(active, namePattern, pageable).map(categoryMapper::toResponse);
    }

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la categoría solicitada."));
    }
}
