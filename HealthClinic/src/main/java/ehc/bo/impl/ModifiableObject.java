package ehc.bo.impl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class ModifiableObject extends BaseObject {
	private User modifiedBy;
	private Date modifiedOn;
	

	protected ModifiableObject() {
		super();
	}
	public ModifiableObject(User executor) {
		super(executor);
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "modifiedby")
	public User getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(User modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	
	@Column(name = "modifiedon")
	public Date getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	
	

}
