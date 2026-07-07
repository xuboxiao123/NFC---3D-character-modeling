package com.example.NFC_system.repository;

import java.util.List;

import com.example.NFC_system.entity.Character;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    public List<Character> findByUserId(Long userId);

    public List<Character> findByUserIdOrderBySortOrderAsc(Long userId);
}