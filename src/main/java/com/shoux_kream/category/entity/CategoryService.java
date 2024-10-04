package com.shoux_kream.category.entity;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional //카테고리 추가
    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = categoryDto.toEntity();
        Category savedCategory = categoryRepository.save(category);
        return new CategoryDto(savedCategory);
    }



    @Transactional //카테고리 수정
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
        category.updateCategory(categoryDto.getName());
        return new CategoryDto(category);
    }

    @Transactional //카테고리 삭제
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
