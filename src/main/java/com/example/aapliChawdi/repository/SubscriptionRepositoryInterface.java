package com.example.aapliChawdi.repository;

import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.entity.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepositoryInterface extends JpaRepository<Subscription, Long> {

    List<Subscription> findByChatId(Long chatId);

    List<Subscription> findByVillage(Village village);

    boolean existsByChatIdAndVillage(
            Long chatId,
            Village village
    );

    void deleteByChatIdAndVillage(
            Long chatId,
            Village village
    );

    @Query(value = """
        SELECT DISTINCT v.*
        FROM villages v
        JOIN subscriptions s
          ON s.village_id = v.id
        """, nativeQuery = true)
    List<Village> findAllSubscribedVillages();
}
