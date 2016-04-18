package milansluka.HealthClinic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Utils {
	public Date toDate(String str) {
		DateFormat format = new SimpleDateFormat(Config.DATETIME_FORMAT);
		Date date = null;

		try {
			date = format.parse(str);
		} catch (ParseException e) {

		}

		return date;
	}

	public String cryptWithMD5(String pass) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] passBytes = pass.getBytes();
			md.reset();
			byte[] digested = md.digest(passBytes);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < digested.length; i++) {
				sb.append(Integer.toHexString(0xff & digested[i]));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			/*
			 * Logger.getLogger(CryptWithMD5.class.getName()).log(Level.SEVERE,
			 * null, ex);
			 */
		}
		return null;
	}

	public Session getSession() {
		SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
		return sessionFactory.openSession();
	}
}
