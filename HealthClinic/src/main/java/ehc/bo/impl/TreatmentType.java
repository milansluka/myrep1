package ehc.bo.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Query;
import org.hibernate.Session;

@Entity
@Table(name = "treatment_type")
public class TreatmentType extends BaseObject{
	
	String name;
	String info;
	String category;
	
	double price;
	
	List<RoomAssignment> possibleRoomAssignments;
	List<Device> requiredDevices;
	List<Appointment> appointments;
/*	List<Skill> requiredPhysicianSkills;
	int requiredCountOfNurses;*/
	PhysicianType physicianType;
/*	NurseType nurseType;*/
	
	protected TreatmentType() {
		super();
		appointments = new ArrayList<Appointment>();
	}
	
	public TreatmentType(User executor, String name, String category, double price, PhysicianType physicianType) {
		super(executor);
		appointments = new ArrayList<Appointment>();
		this.name = name;
		this.category = category;
		this.physicianType = physicianType;
		this.price = price;
	}
	
	public String getCategory() {
		return category;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@OneToMany(mappedBy = "treatmentType")
	public List<Appointment> getAppointments() {
		return appointments;
	}
	public void setAppointments(List<Appointment> appointments) {
		this.appointments = appointments;
	}
	
    @OneToMany(mappedBy = "treatmentType")
	public List<RoomAssignment> getPossibleRoomAssignments() {
		return possibleRoomAssignments;
	}

	public void setPossibleRoomAssignments(List<RoomAssignment> possibleRoomAssignments) {
		this.possibleRoomAssignments = possibleRoomAssignments;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "physician_type_id")
	public PhysicianType getPhysicianType() {
		return physicianType;
	}

	public void setPhysicianType(PhysicianType physicianType) {
		this.physicianType = physicianType;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	
	public void addAppointment(Appointment appointment) {
		getAppointments().add(appointment);
	}
	
	public static TreatmentType getTreatmentType(long id, Session session) {
		String hql = "FROM TreatmentType t WHERE t.id = :id";
		Query query = session.createQuery(hql);
		query.setParameter("id", id);

		List results = query.list();
		
		return (TreatmentType) results.get(0);		
	}
	
	public static TreatmentType getTreatmentType(String type, Session session) {
		String hql = "FROM TreatmentType t WHERE t.type = :type";
		Query query = session.createQuery(hql);
		query.setParameter("type", type);

		List results = query.list();
		
		return (TreatmentType) results.get(0);		
	}
}
