package ehc.bo.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ehc.bo.impl.AppointmentProposal;
import ehc.bo.impl.Individual;
import ehc.bo.impl.IndividualDao;
import ehc.bo.impl.Login;
import ehc.bo.impl.AppointmentProposalUtil;
import ehc.bo.impl.TreatmentType;
import ehc.bo.impl.TreatmentTypeDao;
import ehc.bo.impl.User;
import ehc.hibernate.HibernateUtil;
import ehc.util.DateUtil;


//There are no other planned appointments
public class ScheduleAppointmentForGivenTime_AllResourcesAvailable_Success extends RootTestCase {
	private String personFirstName = "Jan";
	private String personLastName = "Novak";
	private String treatmentName = "Odstraňovanie pigmentov chrbát";
	private Date when = DateUtil.date(2016, 7, 7, 8, 0, 0);
	private Date to = DateUtil.date(2016, 7, 7, 9, 20, 0);
	private TreatmentTypeDao treatmentTypeDao = TreatmentTypeDao.getInstance();
	private IndividualDao individualDao = IndividualDao.getInstance();
	
	protected void setUp() throws Exception {
		super.setUp();

		if (!isSystemSet()) {
			setUpSystem();
		}
		
		HibernateUtil.beginTransaction();
		Individual existingPerson = individualDao.findByFirstAndLastName(personFirstName, personLastName);
		HibernateUtil.commitTransaction();
		if (existingPerson == null) {
			Login login = new Login();
			
			HibernateUtil.beginTransaction();
			User executor = login.login("admin", "admin");
			Individual person = new Individual(executor, personFirstName, personLastName);
			HibernateUtil.save(person);		
			HibernateUtil.commitTransaction();
			
		}
	}

	public void testApp() {
		Login login = new Login();
		
		HibernateUtil.beginTransaction();
		User executor = login.login("admin", "admin");
		Individual person = individualDao.findByFirstAndLastName(personFirstName, personLastName);
		TreatmentType treatmentType = treatmentTypeDao.findByName(treatmentName);
		List<TreatmentType> treatmentTypes = new ArrayList<>();
		treatmentTypes.add(treatmentType);
		AppointmentProposalUtil resourcesUtil = new AppointmentProposalUtil(getWorkTime());
        List<AppointmentProposal> appointmentProposals = resourcesUtil.getAppointmentProposals(when, to, treatmentTypes, 1);
        HibernateUtil.commitTransaction();
        
        AppointmentProposal appointmentProposal = appointmentProposals.get(0); 
        
     /*   assertTrue(appointmentProposal.getFrom().equals(when));*/  
            
        assertTrue(appointmentProposal.getFrom().equals(when) && 
        		appointmentProposal.getResources().size() == 4);      
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		tearDownSystem();
	}

}
