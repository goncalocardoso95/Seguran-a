package com.sc010.server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.sc010.utils.Utils;

public class CatalogoPhotos {

	private ArrayList<Photo> fotos;

	/**
	 * Construtor
	 */
	public CatalogoPhotos() {
		fotos = new ArrayList<Photo>();			
	}

	/**
	 * Get do nome da foto
	 * @param foto - nome da foto
	 * @return nome da foto
	 */
	public Photo getPhoto(String foto) {

		for (int i = 0; i < fotos.size(); i++) {

			if (fotos.get(i).getNome().equals(foto)) {
				return fotos.get(i);
			}
		}
		return null;	
	}

	/**
	 * Popular o ficheiro com o nome e data
	 * @param photos - ficheiro de fotos
	 */
	public void populate(File photos) {	
		try {
			Utils.decifraFile(photos.toPath().toString());
			BufferedReader reader = new BufferedReader(new FileReader(photos + ".decif"));
			String line="";
			Photo photo;
			while((line = reader.readLine()) != null){
				String[] split = line.split(":");
				photo = new Photo(split[0], split[1]+":"+split[2]+":"+split[3]);
				fotos.add(photo);		
			}
			reader.close();
			Utils.cifraOldFile(photos.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Retorna lista de fotos
	 * @return fotos
	 */
	public ArrayList<Photo> listaFotos() {		
		return fotos;
	}

	/**
	 * Existe foto
	 * @param foto - nome da foto
	 * @return true se existe or false se nao existe
	 * @throws IOException
	 */
	public boolean existsPhoto(String foto){
		for (int i = 0; i < fotos.size(); i++) {
			if (fotos.get(i).getNome().contains(foto)) {
				return true;
			}
		}
		return false;
	}

}
