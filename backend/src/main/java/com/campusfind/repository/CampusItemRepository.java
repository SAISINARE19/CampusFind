package com.campusfind.repository;
import com.campusfind.entity.CampusItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CampusItemRepository extends JpaRepository<CampusItem,Long>{
 List<CampusItem> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);
 List<CampusItem> findAllByOrderByCreatedAtDesc();
 List<CampusItem> findByReporterEmailIgnoreCaseOrderByCreatedAtDesc(String reporterEmail);
}