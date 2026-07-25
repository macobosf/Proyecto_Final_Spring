package ec.edu.ups.icc.academiceventsapi.category.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryRequest;
import ec.edu.ups.icc.academiceventsapi.category.dto.CategoryResponse;
import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
import ec.edu.ups.icc.academiceventsapi.category.mapper.CategoryMapper;
import ec.edu.ups.icc.academiceventsapi.category.repository.CategoryRepository;
import ec.edu.ups.icc.academiceventsapi.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl categoryServiceImpl; 
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    
    // Camino feliz: nombre único, la categoría se guarda y se devuelve mapeada.
    @Test
    void create_deriaGuardarCategoria_cuandoNombreNoEstaDuplicado () {
        CategoryRequest request = new CategoryRequest("Tecnología", "Eventos de tecnología");
        when(categoryRepository.existsByNameIgnoreCase("Tecnología")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category("Tecnología", "Eventos de tecnología"));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(
            new CategoryResponse(1L, "Tecnología", "Eventos de tecnología", true, Instant.now(), Instant.now()));
        CategoryResponse response = categoryServiceImpl.create(request);
        assertThat(response).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }   
    
    // Nombre duplicado: debe fallar antes de intentar guardar.
    @Test
    void create_deberiaLanzarExcepcion_cuandoNombreEstaDuplicado() {
        
        CategoryRequest request = new CategoryRequest("Tecnología", "Eventos de tecnología");
        when(categoryRepository.existsByNameIgnoreCase("Tecnología")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> categoryServiceImpl.create(request));
        verify(categoryRepository, never()).save(any());
    }

    // Id existente: debe devolver la categoría mapeada a DTO.
    @Test
    void getById_deberiaRetornarCategoria_cuandoExiste() {
        Category category = new Category("Tecnología", "Eventos de tecnología");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(
        new CategoryResponse(1L, "Tecnología", "Eventos de tecnología", true, Instant.now(), Instant.now()));
        CategoryResponse response = categoryServiceImpl.getById(1L);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Tecnología");

    }

    // Id inexistente: debe lanzar ResourceNotFoundException.
    @Test
    void getById_deberiaLanzarExcepcion_cuandoNoExiste() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryServiceImpl.getById(1L));
    }

    // Nombre único: debe actualizar los datos y devolver la categoría mapeada.
    @Test
    void update_deberiaActualizarCategoria_cuandoNombreNoEstaDuplicado() {
        Category category = new Category("Tecnología", "Eventos de tecnología");
        CategoryRequest request = new CategoryRequest("Ciencia", "Eventos de ciencia");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Ciencia", 1L)).thenReturn(false);
        when(categoryMapper.toResponse(category)).thenReturn(
                new CategoryResponse(1L, "Ciencia", "Eventos de ciencia", true, Instant.now(), Instant.now()));

        CategoryResponse response = categoryServiceImpl.update(1L, request);

        assertThat(response).isNotNull();
        assertThat(category.getName()).isEqualTo("Ciencia");
        assertThat(category.getDescription()).isEqualTo("Eventos de ciencia");
    }

    // Id inexistente: debe lanzar ResourceNotFoundException antes de validar el nombre.
    @Test
    void update_deberiaLanzarExcepcion_cuandoCategoriaNoExiste() {
        CategoryRequest request = new CategoryRequest("Ciencia", "Eventos de ciencia");
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryServiceImpl.update(1L, request));
        verify(categoryRepository, never()).existsByNameIgnoreCaseAndIdNot(any(), any());
    }

    // Nombre duplicado con otra categoría: debe fallar sin modificar la categoría actual.
    @Test
    void update_deberiaLanzarExcepcion_cuandoNombreEstaDuplicado() {
        Category category = new Category("Tecnología", "Eventos de tecnología");
        CategoryRequest request = new CategoryRequest("Ciencia", "Eventos de ciencia");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Ciencia", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryServiceImpl.update(1L, request));
        assertThat(category.getName()).isEqualTo("Tecnología");
    }

    // Id existente: debe marcar la categoría como inactiva (soft delete).
    @Test
    void delete_deberiaDesactivarCategoria_cuandoExiste() {
        Category category = new Category("Tecnología", "Eventos de tecnología");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryServiceImpl.delete(1L);

        assertThat(category.isActive()).isFalse();
    }

    // Id inexistente: debe lanzar ResourceNotFoundException.
    @Test
    void delete_deberiaLanzarExcepcion_cuandoNoExiste() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryServiceImpl.delete(1L));
    }

}
