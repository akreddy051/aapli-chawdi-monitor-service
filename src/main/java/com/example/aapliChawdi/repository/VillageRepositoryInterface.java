package com.example.aapliChawdi.repository;

import com.example.aapliChawdi.entity.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VillageRepositoryInterface extends JpaRepository<Village, Long> {

    Optional<Village> findByDistrictAndTalukaAndVillage(
            String district,
            String taluka,
            String village
    );

    @Query("SELECT DISTINCT v.district FROM Village v")
    List<String> findDistinctDistricts();

    @Query("SELECT DISTINCT v.taluka FROM Village v WHERE v.district = :district")
    List<String> findDistinctTalukasByDistrict(@Param("district") String district);

    @Query("SELECT v.village FROM Village v WHERE v.district = :district AND v.taluka = :taluka")
    List<String> findVillagesByDistrictAndTaluka(
            @Param("district") String district,
            @Param("taluka") String taluka);

    long countByDistrictAndTaluka(String district, String taluka);
    Boolean existsByDistrictAndTalukaAndVillage(String district, String taluka, String village);
}
