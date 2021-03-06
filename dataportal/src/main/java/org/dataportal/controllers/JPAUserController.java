/**
 * 
 */
package org.dataportal.controllers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.dataportal.SystemSingleton;
import org.dataportal.model.User;

/**
 * @author Micho Garcia
 * 
 */
public class JPAUserController {

	public static final String NOT_CONFIRMED = "NOT_CONFIRMED";
	public static final String ACTIVE = "ACTIVE";
	public static final String BLOCKED = "BLOCKED";
	public static final String NONEXISTENT = "NONEXISTENT";

	private static final char[] HEX = new String("0123456789abcdef")
			.toCharArray();
	private static final char[] CHARS = new String(
			"0123456789abcdefghijklmnopqrstuvwxyz").toCharArray();

	/**
	 * 
	 * Create an entitymanager
	 * 
	 * @return EntityManager
	 */
	public EntityManager getEntityManager() {
		EntityManagerFactory entityFactory = Persistence
				.createEntityManagerFactory(SystemSingleton
						.getPersistenceUnit());
		return entityFactory.createEntityManager();
	}

	/**
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean insert(User user) throws Exception {

		boolean inserted = false;
		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		try {
			transaction.begin();
			manager.persist(user);
			transaction.commit();
			inserted = true;
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null)
				manager.close();
		}
		return inserted;
	}

	/**
	 * @param user
	 * @return
	 */
	public User existsInto(User user) {

		User userInto = null;
		EntityManager manager = getEntityManager();
		userInto = manager.find(User.class, user.getId());
		manager.close();

		return userInto;
	}

	public String save(User user) throws Exception {
		String now = Long.toString(new Date().getTime());
		String hash = hex_md5(user.getId() + now);
		user.setState(NOT_CONFIRMED);
		user.setHash(hash);

		boolean inserted = insert(user);
		if (!inserted)
			return null;
		else
			return hash;
	}

	private String hex_md5(String stringToHash) throws NoSuchAlgorithmException {
		MessageDigest md;

		md = MessageDigest.getInstance("MD5");
		byte[] bytes = md.digest(stringToHash.getBytes());
		StringBuilder sb = new StringBuilder(2 * bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			int low = (int) (bytes[i] & 0x0f);
			int high = (int) ((bytes[i] & 0xf0) >> 4);
			sb.append(HEX[high]);
			sb.append(HEX[low]);
		}
		return sb.toString();

	}

    public String createRandomPass(int length) {
		Random rnd = new Random(new Date().getTime());
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(CHARS[rnd.nextInt(CHARS.length)]);
		}
		return sb.toString();
	}

	public String setHash(User user, String state) throws Exception {
		String now = Long.toString(new Date().getTime());
		String hash = hex_md5(user.getId() + now);
		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		try {			
			User userInto = manager.find(User.class, user.getId());;
			transaction.begin();
			if (userInto != null) {
				userInto.setHash(hash);
				userInto.setState(state);
			}
			transaction.commit();
			return hash;
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null)
				manager.close();
		}
	}

	private User getUserByHash(EntityManager manager, String hash) {
        String strQuery = "SELECT u FROM User u WHERE u.hash = :hash";
        TypedQuery<User> query = manager.createQuery(strQuery, User.class);
        List<User> users = query.setParameter("hash", hash).getResultList();
        if (users.size() == 1)
            return users.get(0);
		else
			return null;
	}

	private User getUserByIdAndPassword(EntityManager manager, String id,
			String password) {
        String strQuery = "SELECT u FROM User u WHERE u.id = :id AND u.password = :password";
		TypedQuery<User> query = manager.createQuery(strQuery, User.class);
		query.setParameter("id", id);
		query.setParameter("password", password);
        List<User> users = query.getResultList();
        if (users.size() == 1)
            return users.get(0);
		else
			return null;
	}

    public User setPass(String hash, String pass) throws Exception {
		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		try {
            User user = getUserByHash(manager, hash);
            if (user != null) {
                String password = hex_md5(user.getId() + ":" + pass);
				transaction.begin();
                user.setHash("");
                user.setPassword(password);
				transaction.commit();
			}
            return user;
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null) {
				manager.close();
			}
		}
	}

	public boolean isActive(User user) {
        String state = this.getState(user);
        return state.equals(ACTIVE);
	}

	public String activate(String hash) throws Exception {
		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		User userInto = getUserByHash(manager, hash);
		try {
			if (userInto != null) {
				transaction.begin();
				userInto.setHash("");
				userInto.setState(ACTIVE);
				transaction.commit();
			} else {
				return null;
			}
			return userInto.getId();
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null)
				manager.close();
		}
	}

	public boolean exists(User user) {
		User userInto = existsInto(user);
		if (userInto != null)
			return true;
		else
			return false;
	}

	public String getState(User user) {
		EntityManager manager = getEntityManager();
		User userInto = getUserByIdAndPassword(manager, user.getId(),
				user.getPassword());
		if (userInto != null)
			return userInto.getState();
		else
			return NONEXISTENT;
	}

	public boolean changePass(User user, String newPassword) throws Exception {
		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		User userInto = getUserByIdAndPassword(manager, user.getId(),
				user.getPassword());
		try {
			if (userInto != null) {
				transaction.begin();
                userInto.setPassword(newPassword);
				transaction.commit();
			} else {
				return false;
			}
			return true;
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null)
				manager.close();
		}
	}

	public void changeState(User user) throws Exception {
		User userInto = existsInto(user);
		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		try {
			if (userInto != null) {
				transaction.begin();
				userInto.setState(user.getState());
				transaction.commit();
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null)
				manager.close();
		}
	}

	public void delete(User user) throws Exception {

		EntityManager manager = getEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		try {
			transaction.begin();
			User userInto = manager.find(User.class, user.getId());
			if (userInto != null) {
				manager.remove(userInto);
				transaction.commit();
			} else {
				transaction.rollback();
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (manager != null)
				manager.close();
		}
	}
}
