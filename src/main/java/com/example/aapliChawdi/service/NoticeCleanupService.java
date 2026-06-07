package com.example.aapliChawdi.service;

import com.example.aapliChawdi.entity.Notice;
import com.example.aapliChawdi.entity.Village;
import com.example.aapliChawdi.repository.NoticeRepository;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeCleanupService {

    private final NoticeRepository noticeRepository;
    private final VillageRepositoryInterface villageRepository;

    private static final int MAX_NOTICES_PER_VILLAGE = 20;

    public void cleanupForVillage(Village village) {
        List<Notice> notices = noticeRepository
                .findByVillageAndProcessedAtIsNotNullOrderByProcessedAtDesc(
                        village);

        if (notices.size() <= MAX_NOTICES_PER_VILLAGE) {
            return;
        }

        // keep first 20, delete the rest
        List<Notice> toDelete = notices.subList(
                MAX_NOTICES_PER_VILLAGE, notices.size());

        log.info("Deleting {} old notices for village {}",
                toDelete.size(), village.getVillage());

        noticeRepository.deleteAll(toDelete);
    }
}
