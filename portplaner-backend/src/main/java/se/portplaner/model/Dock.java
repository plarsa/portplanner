package se.portplaner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "docks")
@Getter
@Setter
public class Dock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "dock", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Slip> slips = new ArrayList<>();
}
