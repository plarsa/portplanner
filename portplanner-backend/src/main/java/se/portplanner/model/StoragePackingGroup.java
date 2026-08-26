package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "storage_packing_groups")
@Getter
@Setter
public class StoragePackingGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yard_id", nullable = false)
    private StorageYard yard;

    @Column(nullable = false)
    private String name;

    @Column(name = "retrieval_note", length = 1000)
    private String retrievalNote;
}
