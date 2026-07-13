package com.vyaparsetu.catalog.repository;

import com.vyaparsetu.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
