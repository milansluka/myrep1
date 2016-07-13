package ehc.bo.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ehc.bo.Resource;

public class AppointmentProposal {
	Map<ResourceType, List<Resource>> resources = new HashMap<>();
    TreatmentType treatmentType;
    Date from;
    Date to;
     
	public AppointmentProposal(Map<ResourceType, List<Resource>> resources, TreatmentType treatmentType, Date from, Date to) {
		super();
		this.resources = resources;
		this.treatmentType = treatmentType;
		this.from = from;
		this.to = to;
	}
	
	public Map<ResourceType, List<Resource>> getResources() {
		return resources;
	}
	public void setResources(Map<ResourceType, List<Resource>> resources) {
		this.resources = resources;
	}
	public TreatmentType getTreatmentType() {
		return treatmentType;
	}
	public void setTreatmentType(TreatmentType treatmentType) {
		this.treatmentType = treatmentType;
	}
	public Date getFrom() {
		return from;
	}
	public void setFrom(Date from) {
		this.from = from;
	}
	public Date getTo() {
		return to;
	}
	public void setTo(Date to) {
		this.to = to;
	}
    
    
}
