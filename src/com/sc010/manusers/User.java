package com.sc010.manusers;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class User {

	private String UserName;
	private String password;
	private ArrayList<String> followers;


	/**
	 * Construtor
	 * @param userName
	 * @param password
	 */
	public User(String userName,String password ) {
		this.UserName = userName;
		this.password = password;
		followers = new ArrayList<String>();

	}

	/**
	 * Get nome do user
	 * @return UserName 
	 */
	public String getUserName() {
		return UserName;
	}

	/**
	 * Get password do user
	 * @return password
	 */
	public String getPassword() {
		return password;

	}

	/**
	 * Get followers do user
	 * @return followers
	 */
	public ArrayList<String> getFollowersList() {//String UserName
		return followers;	
	}

	/**
	 * ExistsFollower
	 * @param username - nome do user a procurar
	 * @return true or false
	 */
	public boolean existsFollower(String username) {
		for (int i = 0; i < followers.size(); i++) {
			if (username.equals(followers.get(i)))
				return true;
		}
		return false;
	}

	/**
	 * Followers
	 */
	public void Follower() {
		for (int i = 0; i < followers.size(); i++) {
			System.out.println("LISTA:  " + followers.get(i));
		}

	}

	/**
	 * Popular o ficheiro de followers
	 * @param follow - ficheiros de followers
	 */
	public void populateFollowers(File follow){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(follow));
			String line="";	
			while((line = reader.readLine()) != null){
				followers.add(line);
				System.out.println("followers: " + line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * RemoveFollowers
	 * @param follow- ficheiro de followers
	 * @param follower - follower a remover
	 */
	public void removeFollowers(File follow,String follower){

		try {
			BufferedReader reader = new BufferedReader(new FileReader(follow));
			String line="";

			while((line = reader.readLine()) != null){
				if (follower.equals(line)) {
					followers.remove(line);
				}			
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Criar ficheiro com follower removido
	 * @param removed - ficheiro novo com follower removido
	 * @param inUser - userAtual
	 * @throws IOException
	 */
	public void CreateFileRemoved(File removed,String inUser){

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("servidor/"+inUser+"/"+removed.getName(), true)); 
			for (int i = 0; i < followers.size(); i++) {
				writer.write(followers.get(i));
				writer.newLine();
			}
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Imprime followers
	 */
	public void imprime() {
		for (int i = 0; i < followers.size(); i++) {
			System.out.println(followers.get(i));
		}

	}

	/**
	 * Atualiza a password antiga para a dada
	 * @param pw Password nova
	 */
	public void setPwd(String pw) {
		this.password = pw;
	}

}