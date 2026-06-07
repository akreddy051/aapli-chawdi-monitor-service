package com.example.aapliChawdi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "villages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"district", "taluka", "village"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String district;

    private String taluka;

    private String village;
}