package com.kevinguanchedarias.owgejava.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.util.Date;

@Entity
@Table(name = "mission_reports")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MissionReport implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = 129057515438080621L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "json_body", nullable = false)
    private String jsonBody;

    @OneToOne(mappedBy = "report", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserStorage user;

    @Column(name = "report_date")
    private Date reportDate;

    @Column(name = "user_read_date")
    private Date userReadDate;

    @Column(name = "is_enemy")
    private Boolean isEnemy = false;

    public MissionReport(String jsonBody, Mission mission) {
        this.jsonBody = jsonBody;
        this.mission = mission;
    }
}
