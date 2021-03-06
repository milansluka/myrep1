package ehc.bo.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ehc.bo.Resource;
import ehc.bo.impl.Appointment;
import ehc.bo.impl.AppointmentDao;
import ehc.bo.impl.AppointmentScheduleData;
import ehc.bo.impl.AppointmentScheduler;
import ehc.bo.impl.HealthPoint;
import ehc.bo.impl.Individual;
import ehc.bo.impl.Login;
import ehc.bo.impl.Physician;
import ehc.bo.impl.PhysicianDao;
import ehc.bo.impl.Room;
import ehc.bo.impl.RoomDao;
import ehc.bo.impl.TreatmentType;
import ehc.bo.impl.TreatmentTypeDao;
import ehc.bo.impl.User;
import ehc.hibernate.HibernateUtil;
import ehc.util.DateUtil;

public class CreateAppointmentForNewPerson extends RootTestCase {
	private String treatmentName = "Odstraňovanie pigmentov chrbát";
	private TreatmentTypeDao treatmentTypeDao = TreatmentTypeDao.getInstance();
	private PhysicianDao physicianDao = PhysicianDao.getInstance();
	private RoomDao roomDao = RoomDao.getInstance();

	protected void setUp() throws Exception {
		super.setUp();

		if (!isSystemSet()) {
			setUpSystem();
		}
	}

	public void testApp() {
		String personFirstName = "Jan";
		String personLastName = "Novak";

		Login login = new Login();

		HibernateUtil.beginTransaction();

		User executor = login.login("admin", "admin");
		Individual person = new Individual(executor, personFirstName, personLastName);
		TreatmentType treatmentType = treatmentTypeDao.findByName(treatmentName);
		Date from = DateUtil.date(2016, 4, 20, 10, 0, 0);
		Date to = DateUtil.date(2016, 4, 20, 10, 30, 0);
		
		List<TreatmentType> treatmentTypes = new ArrayList<TreatmentType>();
		treatmentTypes.add(treatmentType);
		
		List<Physician> physicians = physicianDao.getAll();
		List<Room> rooms = roomDao.getAll();
		Physician physician = physicians.get(0);
		Individual physicianPerson = (Individual)physician.getSource();
		String physicianFirstName = physicianPerson.getFirstName();
		String physicianLastName = physicianPerson.getName();
		Room room = rooms.get(0);
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(physician);
		resources.add(room);
		
		AppointmentScheduler appointmentScheduler = new AppointmentScheduler(getWorkTime(), HealthPoint.DEFAULT_TIME_GRID_IN_MINUTES);
        AppointmentScheduleData appointmentScheduleData = new AppointmentScheduleData(from, to, resources);
		Appointment appointment = appointmentScheduler.getAppointment(executor, appointmentScheduleData, treatmentTypes, person);

		/*long appointmentId = (long)HibernateUtil.save(appointment);*/
		long appointmentId = addAppointment(appointment);
		HibernateUtil.commitTransaction();
		
		HibernateUtil.beginTransaction();	
		AppointmentDao appointmentDao = AppointmentDao.getInstance();
		Appointment persistedAppointment = appointmentDao.findById(appointmentId);		
		String appointmentPhysicianFirstName = null;
		String appointmentPhysicianLastName = null;
		
		for (Resource resource : persistedAppointment.getResources()) {
			if (resource instanceof Physician) {
				Physician physicianRole = (Physician) resource;
				Individual individual = (Individual)physicianRole.getSource();
				appointmentPhysicianFirstName = individual.getFirstName();
				appointmentPhysicianLastName = individual.getName();		
			}		
		}		
		HibernateUtil.commitTransaction();
		
		assertTrue(appointmentPhysicianFirstName.equals(physicianFirstName) && 
				appointmentPhysicianLastName.equals(physicianLastName));     
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		tearDownSystem();
	}

}
