package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "critical_attack_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalAttackEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = -6479956929235556987L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "critical_attack_id")
    private CriticalAttack criticalAttack;

    @Enumerated(EnumType.STRING)
    private AttackableTargetEnum target;

    private Integer referenceId;

    private Float value;

}
