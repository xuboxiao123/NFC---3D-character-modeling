package com.example.NFC_system.repository;

import com.example.NFC_system.entity.NfcFaceCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NfcFaceCardRepository extends JpaRepository<NfcFaceCard, Long> {

    Optional<NfcFaceCard> findByCode(String code);

    @Query("SELECT MAX(CAST(SUBSTRING(n.code, 2) AS int)) FROM NfcFaceCard n WHERE n.code LIKE 'C%'")
    Integer findMaxCodeNumber();
}