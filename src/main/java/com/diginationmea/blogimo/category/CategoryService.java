package com.diginationmea.blogimo.category;

import com.diginationmea.blogimo.category.dto.CategoryRequest;
import com.diginationmea.blogimo.category.dto.CategoryResponse;
import com.diginationmea.blogimo.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public Page<CategoryResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(CategoryService::toResponse);
    }

    public CategoryResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        Category category = new Category();
        category.setName(req.name());
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = find(id);
        category.setName(req.name());
        repository.flush();
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
        repository.flush();
    }

    private Category find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
