package com.kevinguanchedarias.sgtjava.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "galaxies")
public class Galaxy implements SimpleIdEntity {
	private static final long serialVersionUID = -230625496064517670L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;
	private Long sectors;
	private Long quadrants;

	@Column(name = "order_number")
	private Integer orderNumber;

	@OneToMany(mappedBy = "galaxy")
	@Cascade({ CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DELETE })
	private List<Planet> planets;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getSectors() {
		return sectors;
	}

	public void setSectors(Long sectors) {
		this.sectors = sectors;
	}

	public Long getQuadrants() {
		return quadrants;
	}

	public void setQuadrants(Long quadrants) {
		this.quadrants = quadrants;
	}

	public Integer getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}

	public List<Planet> getPlanets() {
		return planets;
	}

	public void setPlanets(List<Planet> planets) {
		this.planets = planets;
	}

}
