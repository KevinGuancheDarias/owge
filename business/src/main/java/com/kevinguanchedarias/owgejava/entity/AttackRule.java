package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.AttackRuleEntityListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serial;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "attack_rules")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AttackRuleEntityListener.class)
public class AttackRule implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -8949527349751479310L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String name;

    @OneToMany(mappedBy = "attackRule")
    @ToString.Exclude
    private transient List<AttackRuleEntry> attackRuleEntries;
}
