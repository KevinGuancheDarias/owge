package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "mission_information")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MissionInformation implements Serializable {
    @Serial
    private static final long serialVersionUID = -2285741740473228591L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    @ToString.Exclude
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relation_id")
    @Fetch(FetchMode.JOIN)
    private ObjectRelation relation;

    private Double value;

    public void setValue(Integer value) {
        this.value = Double.valueOf(value);
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
