package com.example.NFC_system.repository;

import com.example.NFC_system.entity.CharacterVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterVersionRepository extends JpaRepository<CharacterVersion, Long> {

    List<CharacterVersion> findByCharacterId(Long characterId);

    List<CharacterVersion> findByCharacterIdOrderBySortOrderAsc(Long characterId);

    void deleteByCharacterId(Long characterId);
}