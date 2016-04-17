package milansluka.HealthClinic.bo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import junit.framework.TestCase;
import milansluka.HealthClinic.Config;
import milansluka.HealthClinic.Utils;
import milansluka.HealthClinic.bo.impl.Appointment;
import milansluka.HealthClinic.bo.impl.AppointmentsManager;
import milansluka.HealthClinic.bo.impl.Intervention;
import milansluka.HealthClinic.bo.impl.Login;
import milansluka.HealthClinic.bo.impl.Person;
import milansluka.HealthClinic.bo.impl.User;
import milansluka.HealthClinic.bo.impl.UserRight;
import milansluka.HealthClinic.bo.impl.UserRightType;
import milansluka.HealthClinic.bo.impl.UserValidation;

public class CreateAppointmentTest extends TestCase {
	private SessionFactory sessionFactory;

	public void testApp() {

		sessionFactory = new Configuration().configure().buildSessionFactory();

		createUser();
		loginSuccessful();
		loginFailed();
		createAppointment();
	}

	public void createUser() {
		Session session = sessionFactory.openSession();
		session.beginTransaction();

		User user = new User();
		user.setLogin("admin");
		user.setPassword("12345");

		UserValidation validation = new UserValidation(user, sessionFactory);

		if (validation.loginIsValid() && validation.passwordIsValid()) {
			UserRight right = new UserRight();
			right.setType(UserRightType.CREATE_APPOINTMENT);

			user.assignRight(right);

			long userId = (Long) session.save(user);

			session.getTransaction().commit();
			session.close();
		}

		assertTrue(user.getLogin().equals("admin") && user.getPassword().equals("12345"));
	}

	public void loginSuccessful() {
		Session session = new Utils().getSession();
		session.beginTransaction();

		User user = new User();
		user.setLogin("admin");
		user.setPassword("12345");

		Login login = new Login(session);

		assertTrue(login.tryLogin(user));

		session.close();
	}

	public void loginFailed() {
		Session session = new Utils().getSession();
		session.beginTransaction();

		User user = new User();
		user.setLogin("aaaaa");
		user.setPassword("222");

		Login login = new Login(session);

		assertTrue(!login.tryLogin(user));

		session.close();
	}

	public void createAppointment() {

		Person person = new Person();
		person.setFirstName("Milan");
		person.setLastName("Sluka");
		person.setPhone("012345");

		Intervention intervention = new Intervention();
		intervention.setName("some name");
		intervention.setInfo("some info");

		Appointment appointment = new Appointment();
		String strFrom = "17.4.2016 10:00";
		String strTo = "17.4.2016 10:30";

		Utils utils = new Utils();
		Date from = utils.toDate(strFrom);
		Date to = utils.toDate(strTo);

		if (from == null || to == null) {
			assertTrue(false);
			return;
		}

		appointment.setFrom(from);
		appointment.setTo(to);

		appointment.assignPerson(person);
		appointment.assignIntervention(intervention);

		AppointmentsManager manager = new AppointmentsManager(sessionFactory);
		manager.createAppointment(appointment);

	}

}