package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.ObjectRelationToObjectRelationListener;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Table(name = "object_relation__object_relation")
@Entity
@EntityListeners(ObjectRelationToObjectRelationListener.class)
public class ObjectRelationToObjectRelation implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -7645804574908275461L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_relation_id")
    @Fetch(FetchMode.JOIN)
    private ObjectRelation master;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slave_relation_id")
    @Fetch(FetchMode.JOIN)
    private ObjectRelation slave;

    /**
     * @return the id
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the master
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public ObjectRelation getMaster() {
        return master;
    }

    /**
     * @param master the master to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setMaster(ObjectRelation master) {
        this.master = master;
    }

    /**
     * @return the slave
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public ObjectRelation getSlave() {
        return slave;
    }

    /**
     * @param slave the slave to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setSlave(ObjectRelation slave) {
        this.slave = slave;
    }

}
