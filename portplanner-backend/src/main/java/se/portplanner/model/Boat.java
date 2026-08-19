package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "boats")
@Getter
@Setter
public class Boat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "length_m", nullable = false, precision = 6, scale = 2)
    private BigDecimal lengthM;

    @Column(name = "width_m", nullable = false, precision = 6, scale = 2)
    private BigDecimal widthM;

    @Column(name = "draft_m", precision = 6, scale = 2)
    private BigDecimal draftM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;
}
