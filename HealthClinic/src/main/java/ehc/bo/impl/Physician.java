package ehc.bo.impl;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

@Entity
@PrimaryKeyJoinColumn(name = "id")
public class Physician extends ResourcePartyRole {
	PhysicianType type;
	
	protected Physician() {
		super();
	}

	public Physician(User executor, PhysicianType type, Party source, Party target) {
		super(executor, source, target);
		this.type = type;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "physician_type_id")
	public PhysicianType getType() {
		return type;
	}

	public void setType(PhysicianType type) {
		this.type = type;
	}

	public boolean isCompetent(PhysicianType type) {
		return true;
	}

	public boolean isCompetent(TreatmentType treatmentType) {
		return true;
	}
	
	public void addSkill(Skill skill) {
		getType().addSkill(skill);
	}
}
