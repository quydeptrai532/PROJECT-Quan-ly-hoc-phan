package com.edu.registration.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mon_tien_quyet", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"mon_hoc_id", "mon_tien_quyet_id"})
})
public class PrerequisiteSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_id", nullable = false)
    private Subject monHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_tien_quyet_id", nullable = false)
    private Subject monTienQuyet;

    public PrerequisiteSubject() {
    }

    public PrerequisiteSubject(Subject monHoc, Subject monTienQuyet) {
        this.monHoc = monHoc;
        this.monTienQuyet = monTienQuyet;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subject getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(Subject monHoc) {
        this.monHoc = monHoc;
    }

    public Subject getMonTienQuyet() {
        return monTienQuyet;
    }

    public void setMonTienQuyet(Subject monTienQuyet) {
        this.monTienQuyet = monTienQuyet;
    }
}
