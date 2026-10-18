package com.campusfind.repository;

import com.campusfind.entity.CampusClaim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CampusClaimRepository extends JpaRepository<CampusClaim, Long> {
  List<CampusClaim> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
  boolean existsByItem_IdAndClaimantEmailIgnoreCaseAndKindIgnoreCaseAndStatusIgnoreCase(
      Long itemId, String claimantEmail, String kind, String status);
  @Transactional
  long deleteByItem_Id(Long itemId);
}
