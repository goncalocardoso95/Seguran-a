package com.sc010.manusers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.bind.DatatypeConverter;

/**
 * @author sc010
 *
 */
public class CatalogoUser {
	private static final String usersFile = "Users/users.txt";

	private byte[] encrypted;
	private String encryptedtext;
	private String decrypted;

	private ArrayList<User> users;
	private File db;

	/**
	 * Construtor
	 */
	public CatalogoUser() {
		users = new ArrayList<User>();

		db = new File(usersFile);
		populate(db);
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

	/**
	 * Popular o catalogo com users
	 * 
	 * @param utilizadores
	 *            - ficheiro de users
	 * @throws IOException
	 */
	public void populate(File utilizadores) {

		try {
			BufferedReader reader = new BufferedReader(new FileReader(utilizadores));
			String line = "";
			User user;
			while ((line = reader.readLine()) != null) {
				// Ler linha a linha e separar por dois pontos.
				// Ex: felipe:salt:oaksdfnoij132l%
				String[] split = line.split(":");

				String username, password;

				// Username esta limpo no texto.
				username = split[0];

				// Salt e password hash split[1],[2]
				// Como isto vai ficar => password = decypher(split[2], ivGenerator(sr),
				// split[1].getBytes());
				password = split[2];

				// Ja esta decifrada podemos adicionar.
				user = new User(username, password);

				// All done addiciona ao catalogo.
				users.add(user);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Adiciona utilizador e a sua password ao catalogo de utilizadores. De forma
	 * segura.
	 * 
	 * @param user
	 *            Utilizador a ser adicionado
	 * @param password
	 *            Password a ser adicionada
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws IOException
	 * @throws InvalidAlgorithmParameterException
	 */
	public void add(String user, String password)
			throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException, InvalidAlgorithmParameterException {

		// Criar utilizador.
		User u = new User(user, password);

		if (this.find(user)) {
			System.out.println("Utilizador ja existe");

		} else {
			// System.out.println("Utilizador nao existe");
			this.lista().add(u);
			String dirName = "Clientes/" + user;
			File dir = new File(dirName);
			dir.mkdir();
			// Cypher and persist
			// Create salt
			SecureRandom sr = new SecureRandom();

			byte[] salt = saltGenerator(sr);
			byte[] ivBytes = ivGenerator();
			String saltWord = DatatypeConverter.printHexBinary(salt);
			// String ivWord = DatatypeConverter.printHexBinary(ivBytes);

			try {
				FileWriter fw = new FileWriter(this.db, true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(user + ":" + saltWord + ":" + cypher(password, ivBytes, salt));
				bw.newLine();
				bw.close();
				fw.close();
				System.out.printf("Adicionado %s %s\n", user, password);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gera um random salt com um array de bytes randomizado de forma segura.
	 * 
	 * @param sr
	 *            Gerador de numeros random seguro
	 * @return byte[] ivBytes
	 */
	public byte[] saltGenerator(SecureRandom sr) {
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt;
	}

	/**
	 * Gera um random IV com um array de bytes randomizado de forma segura.
	 * 
	 * @param sr
	 *            Gerador de numeros random seguro
	 * @return byte[] salt
	 */
	public byte[] ivGenerator() {
		byte[] ivBytes = { 0x11, 0x37, 0x69, 0x1F, 0x3D, 0x5A, 0x04, 0x18, 0x23, 0x6B, 0x1F, 0x03, 0x1D, 0x1E, 0x1F,
				0x20 };
		return ivBytes;
	}

	/**
	 * Apaga o user do catalogo e do ficheiro de users
	 * 
	 * @param user
	 *            Username do user a ser apagado
	 */
	public void del(String user) {
		boolean exists = false;
		for (User u : this.lista()) {
			if (u.getUserName().equals(user)) {
				try {
					File tempFile = new File("myTempFile.txt");

					BufferedReader reader = new BufferedReader(new FileReader(this.db));
					BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

					String currentLine;

					while ((currentLine = reader.readLine()) != null) {
						// trim newline when comparing with lineToRemove
						String trimmedLine = currentLine.trim();
						String[] userpass = trimmedLine.split(":");
						if (userpass[0].equals(user))
							continue;
						writer.write(currentLine + System.getProperty("line.separator"));
					}
					writer.close();
					reader.close();
					Files.move(tempFile.toPath(), this.db.toPath(), StandardCopyOption.REPLACE_EXISTING);
					
					//Remover na memoria
					this.lista().remove(u);
					System.out.printf("Removido %s\n", user);
					exists = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;

			}

		}
		if (!exists)
			System.out.println("User nao existe");
	}

	/**
	 * Update da password do user dado, com a password dada
	 * 
	 * @param username
	 *            User a ser feito o update
	 * @param password
	 *            Nova password
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public void update(String username, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {

		// Criar utilizador.
		User u = new User(username, password);

		if (this.lista().contains(u)) {
			System.out.println("Utilizador ja existe");
		} else {
			for (User user : this.lista()) {
				if (user.getUserName().equals(username)) {
					user.setPwd(password);
				}
			}
		}
		// update to file.
		try {
			File tempFile = new File("myTempFile.txt");

			BufferedReader br = new BufferedReader(new FileReader(this.db));
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));

			boolean found = false;
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				// trim newline when comparing with lineToRemove
				String trimmedLine = currentLine.trim();
				String[] userpass = trimmedLine.split(":");
				if (userpass[0].equals(username)) {
					found = true; // Se encontramos o nosso user tudo bem.
					continue;
				}
				bw.write(currentLine + System.getProperty("line.separator"));
			}
			// So damos update se o user exite nao �.
			if (found) {
				// Create salt
				SecureRandom sr = new SecureRandom();
				byte[] salt = saltGenerator(sr);
				byte[] ivBytes = ivGenerator();
				String saltWord = DatatypeConverter.printHexBinary(salt);

				String userLine = u.getUserName() + ":" + saltWord + ":" + cypher(password, ivBytes, salt);
				System.out.println("User updated");
				bw.write(userLine);
				bw.newLine();
			} else {
				System.out.println("User nao encontrado");
			}
			bw.close();
			br.close();
			Files.move(tempFile.toPath(), this.db.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Cifra uma password.
	 * 
	 * @param password
	 *            string com a password a cifrar
	 * @param ivBytes
	 *            array de bytes para servir de IV
	 * @param salt
	 *            suposto array de bytes randomizados
	 * @return password cifrarda numa String
	 */
	public String cypher(String password, byte[] ivBytes, byte[] salt) {
		PBEKeySpec keySpec = new PBEKeySpec("Tree Math Water".toCharArray());
		SecretKeyFactory kf;
		// String cifra= "";
		try {
			kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");

			SecretKey key = kf.generateSecret(keySpec);

			IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
			PBEParameterSpec spec = new PBEParameterSpec(salt, 20, ivSpec);

			Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.ENCRYPT_MODE, key, spec);

			byte[] encrypted = c.doFinal(password.getBytes());

			encryptedtext = DatatypeConverter.printHexBinary(encrypted);
		} catch (Exception e) {
			System.out.println("houve algum erro ao cifrar");
		}
		return encryptedtext;
	}

	/**
	 * Decifra uma password cifrada dada a key.
	 * 
	 * @param cifrado
	 *            String com a password cifrada
	 * @param ivBytes
	 *            Parametros IV randomizados
	 * @param salt
	 *            Salt
	 * @return Password Original
	 * @throws IOException
	 */
	public String decypher(String cifrado, byte[] ivBytes, byte[] salt) throws IOException {
		PBEKeySpec keySpec = new PBEKeySpec("Tree Math Water".toCharArray());
		SecretKeyFactory kf;
		try {
			kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");

			SecretKey key = kf.generateSecret(keySpec);

			IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
			PBEParameterSpec spec = new PBEParameterSpec(salt, 20, ivSpec);

			Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.DECRYPT_MODE, key, spec);

			encrypted = DatatypeConverter.parseHexBinary(cifrado);
			decrypted = new String(c.doFinal(encrypted));

		} catch (Exception e) {
			System.out.println("houve algum erro ao decifrar");
		}

		return decrypted;

	}

}