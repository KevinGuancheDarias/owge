package com.kevinguanchedarias.owgejava.entity;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serial;
import java.util.List;

/**
 * Represents an alliance of players
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Entity
@Table(name = "alliances")
public class Alliance extends CommonEntityWithImage<Integer> {
    @Serial
    private static final long serialVersionUID = -3191006475065220996L;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private UserStorage owner;

    @OneToMany(mappedBy = "alliance")
    private List<UserStorage> users;

    /**
     * @return the owner
     * @since 0.7.0
     */
    public UserStorage getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     * @since 0.7.0
     */
    public void setOwner(UserStorage owner) {
        this.owner = owner;
    }

    /**
     * @return the users
     * @since 0.7.0
     */
    public List<UserStorage> getUsers() {
        return users;
    }

    /**
     * @param users the users to set
     * @since 0.7.0
     */
    public void setUsers(List<UserStorage> users) {
        this.users = users;
    }

}
