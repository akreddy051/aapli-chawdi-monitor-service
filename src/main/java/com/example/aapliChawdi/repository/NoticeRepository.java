package com.example.aapliChawdi.repository;

import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    boolean existsByMutationNo(String mutationNo);

    List<Notice> findByVillageAndProcessedAtIsNull(Village village);

    List<Notice> findByVillage(Village village);

    // fetch last 10 processed notices for a village, newest first
    List<Notice> findTop10ByVillageAndProcessedAtIsNotNullOrderByProcessedAtDesc(
            Village village);

    List<Notice> findByVillageAndProcessedAtIsNotNullOrderByProcessedAtDesc(
            Village village);
}
