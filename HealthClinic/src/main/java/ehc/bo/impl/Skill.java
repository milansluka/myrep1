package ehc.bo.impl;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
public class Skill extends ModifiableObject {
	String name;
	List<PhysicianType> physicianTypes;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
