package com.example.expense_tracker.repository;

import com.example.expense_tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // Базовых CRUD методов из JpaRepository (save, findById, deleteById) пока достаточно
}