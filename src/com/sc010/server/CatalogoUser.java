package com.sc010.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.sc010.utils.Utils;

public class CatalogoUser {

	private ArrayList<User> users;
	private File db;

	/**
	 * Construtor
	 */
	public CatalogoUser() {
		users = new ArrayList<User>();

		db = new File("Users/users.txt");
		populate(db);
	}

	/**
	 * Popular o catalogo com users
	 * 
	 * @param utilizadores
	 *            - ficheiro de users
	 * @throws IOException
	 */
	public void populate(File utilizadores) {

		try {

			// Ler
			BufferedReader reader = new BufferedReader(new FileReader(utilizadores));
			String line = "";
			User user;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(":");
				// Decifrar split[2] com split[1]
				String password = Utils.decifrar(new File("users/users.txt"), split[0]);

				// Persist files
				File userDir = new File("servidor/" + split[0]);
				if (!userDir.exists())
					userDir.mkdir();

				// Init followers file
				File userFollowers = new File("servidor/" + split[0] + "/followers.txt");
				File userFollowersCif = new File("servidor/" + split[0] + "/followers.txt.cif");
				if (!userFollowersCif.exists()) {
					FileOutputStream followersOut = new FileOutputStream(userFollowers, true);
					followersOut.close();
					Utils.cifraFile(userFollowers);
				}

				// Init lista Fotos
				File listaFotos = new File("servidor/" + split[0] + "/listaFotos.txt");
				File listaFotosCif = new File("servidor/" + split[0] + "/listaFotos.txt.cif");
				if (!listaFotosCif.exists()) {
					FileOutputStream listaFotosOut = new FileOutputStream(listaFotos, true);
					listaFotosOut.close();
					Utils.cifraFile(listaFotos);
				}

				user = new User(split[0], password);
				users.add(user);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Lista de users
	 * 
	 * @return users
	 */
	public ArrayList<User> lista() {
		return users;
	}

	/**
	 * Find user name
	 * 
	 * @param user
	 *            - nome do user
	 * @return boolean de verificacao
	 */
	public boolean find(String user) {
		for (int i = 0; i < users.size(); i++) {
			if (user.equals(users.get(i).getUserName()))
				return true;
		}
		return false;
	}

	/**
	 * Get nome do user
	 * 
	 * @return UserName
	 */
	public User getUser(String username) {
		for (int i = 0; i < users.size(); i++) {
			if (username.equals(users.get(i).getUserName()))
				return users.get(i);
		}
		return null;
	}

	/**
	 * Verifica se password esta correta
	 * 
	 * @param user
	 *            - user name
	 * @param pwd
	 *            - passwrod
	 * @return boolean de verificacao
	 */
	public boolean pwdCerta(String user, String pwd) {
		for (int i = 0; i < users.size(); i++) {
			if (user.equals(users.get(i).getUserName())) {
				if (pwd.equals(users.get(i).getPassword()))
					return true;
			}
		}
		return false;
	}

	/**
	 * Get password do user user
	 * 
	 * @param user
	 *            - user name
	 * @return users.get(i).getPassword()
	 */
	public String getUserPwd(String user) {
		for (int i = 0; i < users.size(); i++) {
			if (user.equals(users.get(i).getUserName())) {
				return users.get(i).getPassword();
			}
		}
		return null;
	}

}
