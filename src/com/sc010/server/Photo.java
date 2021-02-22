package com.sc010.server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.sc010.utils.Utils;

public class Photo {

	private String nome;
	private String data;
	private ArrayList<String> likes;
	private ArrayList<String> dislikes;
	private ArrayList<String> comentarios;


	/**
	 * Construtor
	 * @param nome - nome da foto
	 * @param data - data da publicacao
	 */
	public Photo(String nome,String data) {
		this.nome = nome;
		this.data = data;
		likes = new ArrayList<String>();
		dislikes = new ArrayList<String>();
		comentarios = new ArrayList<String>();


	}

	/**
	 * Get nome
	 * @return nome
	 */ 
	public String getNome() {
		return nome;
	}

	/**
	 * Get data
	 * @return
	 */
	public String getData() {
		return data;
	}

	/**
	 * Popular ficheiro de likes
	 * @param userLikes - ficheiros de likes
	 * @throws Exception 
	 */
	public void populateLikes(File userLikes) throws Exception{
		try {
			Utils.decifraFile(userLikes.getPath());
			BufferedReader reader = new BufferedReader(new FileReader(userLikes + ".decif"));
			String line="";
			while((line = reader.readLine()) != null){
				likes.add(line);
			}
			reader.close();
			Utils.cifraOldFile(userLikes.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Popular ficheiro de dislikes
	 * @param userLikes - ficheiros de likes
	 * @throws Exception 
	 */
	public void populateDislikes(File userLikes) throws Exception{

		try {
			Utils.decifraFile(userLikes.getPath());
			BufferedReader reader = new BufferedReader(new FileReader(userLikes + ".decif"));
			String line="";
			while((line = reader.readLine()) != null){
				dislikes.add(line);
			}
			reader.close();	
			Utils.cifraOldFile(userLikes.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Popular ficheiro de comentarios
	 * @param userLikes
	 * @throws Exception 
	 */
	public void populateComments(File userLikes) throws Exception{

		try {
			Utils.decifraFile(userLikes.getPath());
			BufferedReader reader = new BufferedReader(new FileReader(userLikes + ".decif"));
			String line="";
			while((line = reader.readLine()) != null){
				comentarios.add(line);
			}
			reader.close();
			Utils.cifraOldFile(userLikes.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Get lista de likes
	 * @return likes
	 */
	public ArrayList<String> getlistPhotoLikes(){		
		return likes;
	}

	/**
	 * Get lista de dislikes
	 * @return dislikes
	 */
	public ArrayList<String> getlistPhotoDislikes(){		
		return dislikes;
	}

	/**
	 * Get lista de comentarios
	 * @return comentarios
	 */
	public ArrayList<String> getlistPhotoComments(){		
		return comentarios;
	}

	/**
	 * Tamanho lista dislikes
	 * @return dislikes.size()
	 */
	public int  tamanholistPhotoDislikes(){		
		return dislikes.size();
	}

	/**
	 * Tamanho lista likes
	 * @return likes.size()
	 */
	public int  tamanholistPhotoLikes(){		
		return likes.size();
	}

	/**
	 * Tamanho lista comentarios
	 * @return comentarios.size()
	 */
	public int  tamanholistPhotoComments(){		
		return comentarios.size();
	}

	/**
	 * Verifica like
	 * @param user - user like
	 * @return true se deu like or false
	 */
	public boolean deuLike(String user){
		for (int i = 0; i < likes.size(); i++) {
			if (likes.get(i).equals(user)) 
				return true;	
		}
		return false;
	}
	
	/**
	 * Verifica dislike
	 * @param user- user dislike
	 * @return true se deu like or false
	 */
	public boolean deuDislike(String user){
		for (int i = 0; i < dislikes.size(); i++) {
			if (dislikes.get(i).equals(user)) 
				return true;		
		}

		return false;

	}
	
	/**
	 * Imprime
	 */
	public void imprime() {
		for (int i = 0; i < likes.size(); i++) {
			System.out.println("pos"+i+" "+likes.get(i));
		}

	}
}
