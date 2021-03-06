package ehc.bo.test;

import org.hibernate.Hibernate;

import ehc.bo.impl.Individual;
import ehc.bo.impl.IndividualDao;
import ehc.bo.impl.Login;
import ehc.bo.impl.Nurse;
import ehc.bo.impl.NurseDao;
import ehc.bo.impl.ResourcePartyRole;
import ehc.bo.impl.Skill;
import ehc.bo.impl.SkillDao;
import ehc.bo.impl.User;
import ehc.hibernate.HibernateUtil;

public class AddSkillsToNurse extends RootTestCase {
	private NurseDao nurseDao = NurseDao.getInstance();
	private IndividualDao individualDao = IndividualDao.getInstance();
	private SkillDao skillDao = SkillDao.getInstance();

	protected void setUp() throws Exception {
		super.setUp();
		
		if (!isSystemSet()) {
			setUpSystem();		
		}
	}
	
	public void testApp() {
		Login login = new Login();
		
		HibernateUtil.beginTransaction();
		User executor = login.login("admin", "admin");
		Individual individual = individualDao.findByFirstAndLastName("Zuzana", "Vanková");
		Nurse nurse = null;
		for (ResourcePartyRole role : individual.getReservableSourceRoles()) {
			if (role instanceof Nurse) {
				nurse = (Nurse)role;
				break;			
			}		
		}
		long nurseId = nurse.getId();
		
		Skill skill1 = skillDao.findByName("test skill1");
		Skill skill2 = skillDao.findByName("test skill2");
		
		if (skill1 == null) {
			skill1 = new Skill(executor, "test skill1");
		} else {
			Hibernate.initialize(skill1.getResourceTypes());
		}
		if (skill2 == null) {
			skill2 = new Skill(executor, "test skill2");		
		} else {
			Hibernate.initialize(skill2.getResourceTypes());		
		}
		
	    nurse.addSkill(skill1);
	    nurse.addSkill(skill2);
	    HibernateUtil.saveOrUpdate(nurse);		
		HibernateUtil.commitTransaction();
		
		HibernateUtil.beginTransaction();
		nurse = nurseDao.findById(nurseId);
		Hibernate.initialize(nurse.getType().getSkills());	
		HibernateUtil.commitTransaction();
		
		assertTrue(nurse.getType().getSkills().get(0).getName().equals("test skill1") &&
				nurse.getType().getSkills().get(1).getName().equals("test skill2"));		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		tearDownSystem();
	}
}
