package com.sc010.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class Utils {

	public static boolean check(String[] args) {
		if (!(args[0].equals("add") || args[0].equals("del") || args[0].equals("update") || args[0].equals("quit"))) {
			return false;
		}
		return true;
	}

	/**
	 * Decifra a password dada utilizando o salt dado.
	 * 
	 * @param password
	 * @param salt
	 * @return String of password decifrada
	 * @throws IOException
	 */
	public static String decifrar(File ficheiro, String user) throws IOException {
		BufferedReader UserReader = new BufferedReader(new FileReader(ficheiro));
		String linha = "";
		String[] User = null;
		// User[0] = user User[1] = salt User[2] = password
		while ((linha = UserReader.readLine()) != null) {
			User = linha.split(":");
			if (user.equals(User[0])) {
				break;
			}

		}

		UserReader.close();
		String password = User[2];
		byte[] salt = new byte[16];
		salt = DatatypeConverter.parseHexBinary(User[1]);
		byte[] ivBytes = { 0x11, 0x37, 0x69, 0x1F, 0x3D, 0x5A, 0x04, 0x18, 0x23, 0x6B, 0x1F, 0x03, 0x1D, 0x1E, 0x1F,
				0x20 };
		String decrypted = null;

		PBEKeySpec keySpec = new PBEKeySpec("Tree Math Water".toCharArray());
		SecretKeyFactory kf;
		try {
			kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");

			SecretKey key = kf.generateSecret(keySpec);

			IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
			PBEParameterSpec spec = new PBEParameterSpec(salt, 20, ivSpec);

			Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.DECRYPT_MODE, key, spec);

			byte[] passwordBytes;

			passwordBytes = DatatypeConverter.parseHexBinary(password);
			decrypted = new String(c.doFinal(passwordBytes));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decrypted;
	}

	public static void cifraFile(File f) throws Exception {
		// gerar uma chave aleat�ria para utilizar com o AES
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		SecretKey key = kg.generateKey();

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, key);

		FileInputStream fis;
		FileOutputStream fos;

		fis = new FileInputStream(f.getPath());
		fos = new FileOutputStream(f + ".cif"); // Rescrever ficheiro cifrado.
		CipherOutputStream cos = new CipherOutputStream(fos, c);
		byte[] b = new byte[16];
		int i = fis.read(b);
		while (i > 0) {
			cos.write(b, 0, i);
			i = fis.read(b);
		}
		fis.close();
		cos.close();

		// Guardar key usada para cifrar
		guardarKey(key, f + ".key");
		if (f.delete())
			System.out.println("Deu delete");

	}

	private static void guardarKey(SecretKey key, String path) throws IOException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, CertificateException {
		FileOutputStream fichKey = new FileOutputStream(path);
		ObjectOutputStream outKey = new ObjectOutputStream(fichKey);

		PublicKey pk = getChavePublica();
		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.WRAP_MODE, pk);

		byte[] keyEncriptada = c.wrap(key);
		outKey.writeObject(keyEncriptada);
		outKey.close();
	}

	private static PublicKey getChavePublica() throws FileNotFoundException, CertificateException {
		FileInputStream fis = new FileInputStream("certServer.cer");
		BufferedInputStream bis = new BufferedInputStream(fis);

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		return cf.generateCertificate(bis).getPublicKey();
	}

	private static PrivateKey getChavePrivada() throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream("server"), "paparuco".toCharArray());
		return (PrivateKey) ks.getKey("myserver", "paparuco".toCharArray());
	}

	public static void decifraFile(String file) throws Exception {
		FileInputStream fis = new FileInputStream(file + ".cif");
		FileOutputStream fos = new FileOutputStream(file + ".decif");

		// Buscar a key dentro do .key
		FileInputStream fiscif = new FileInputStream(file + ".key");
		PrivateKey key = getChavePrivada();
		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.UNWRAP_MODE, key);
		ObjectInputStream ois = new ObjectInputStream(fiscif);
		byte[] keyCif = (byte[]) ois.readObject();
		Key cifKey = c.unwrap(keyCif, "AES", Cipher.SECRET_KEY);

		// Temos a key do .key

		c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, cifKey); // SecretKeySpec subclasse de secretKey

		CipherInputStream cis = new CipherInputStream(fis, c);

		byte[] input = new byte[16];
		int i = cis.read(input);
		while (i > 0) {
			fos.write(input);
			input = new byte[16];
			i = cis.read(input);
		}
		fos.close();
		cis.close();
		ois.close();
		File cif = new File(file + ".cif");
		cif.delete();

		// Fix a babuja
		BufferedReader br = new BufferedReader(new FileReader(file + ".decif"));

		ArrayList<String> lines = br.lines().collect(Collectors.toCollection(ArrayList::new));
		ArrayList<String> filtered = new ArrayList<>();
		for (String s : lines) {
			if (s != null) {
				if (s.charAt(0) == 0) {
					// Issue needs to be fixed
					// filtered.add(s.substring(5));
					// Done
				} else {
					filtered.add(s);
				}
			}
		}

		// Overwrite unfiltered decif file

		br.close();
		BufferedWriter bw = new BufferedWriter(new FileWriter(file + ".decif"));
		for (String s : filtered) {
			bw.write(s + System.lineSeparator());
		}
		bw.close();
	}

	public static void decifraFoto(String file) throws Exception {
		FileInputStream fis = new FileInputStream(file + ".cif");
		FileOutputStream fos = new FileOutputStream(file + ".decif");

		// Buscar a key dentro do .key
		FileInputStream fiscif = new FileInputStream(file + ".key");
		PrivateKey key = getChavePrivada();
		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.UNWRAP_MODE, key);
		ObjectInputStream ois = new ObjectInputStream(fiscif);
		byte[] keyCif = (byte[]) ois.readObject();
		Key cifKey = c.unwrap(keyCif, "AES", Cipher.SECRET_KEY);

		// Temos a key do .key

		c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, cifKey); // SecretKeySpec subclasse de secretKey

		CipherInputStream cis = new CipherInputStream(fis, c);

		byte[] input = new byte[16];
		int i = cis.read(input);
		while (i > 0) {
			fos.write(input);
			input = new byte[16];
			i = cis.read(input);
		}
		fos.close();
		cis.close();
		ois.close();
		File cif = new File(file + ".cif");
		cif.delete();
	}

	public static void cifraKeyServer(File f, byte[] key) throws Exception {
		String name = f.getPath();

		// cifrar chave secreta com a chave publica que est� na keystore
		// 1) obter o certificado
		FileInputStream kfile = new FileInputStream("server.keyStore"); // keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfile, "seguranca023".toCharArray()); // password
		Certificate cert = kstore.getCertificate("server");

		// 2) cifrar chave secreta com chave publica

		Cipher cs = Cipher.getInstance("RSA");
		cs.init(Cipher.WRAP_MODE, cert);
		byte[] chaveCifrada = cs.wrap(new SecretKeySpec(key, "AES"));

		FileOutputStream kos = new FileOutputStream(name);
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(chaveCifrada);
		oos.close();

	}

	public static byte[] decifraKeyServer(File f) throws Exception {
		try {
			ObjectInputStream keyFile = new ObjectInputStream(new FileInputStream(f.getPath()));
			// numero de bytes do a.key - 256
			byte[] keyEncoded2 = new byte[256];

			keyFile.read(keyEncoded2);
			keyFile.close();

			// decifra a chave
			FileInputStream kfile = new FileInputStream("server.keyStore"); // keystore
			KeyStore kstore = KeyStore.getInstance("JKS");
			kstore.load(kfile, "seguranca023".toCharArray()); // password
			Key myPrivatekey = kstore.getKey("server", "seguranca023".toCharArray());

			Cipher c1 = Cipher.getInstance("RSA");
			c1.init(Cipher.UNWRAP_MODE, myPrivatekey);
			Key key = c1.unwrap(keyEncoded2, "AES", Cipher.SECRET_KEY);

			return key.getEncoded();
		} catch (StreamCorruptedException e) {
			System.err.println("Ficheiro de cifraKeyServer foi corrommpido");
			System.exit(-1);
		}
		return null;
	}

	/**
	 * cria o mac e verifica
	 * 
	 * @param filePath
	 *            - caminho do ficheiro mac directoria Users/users.mac
	 * @param pwdAdmin
	 *            - password introduzida pelo administrador
	 */
	public static void createMac(String filePath, String pwdAdmin) {

		try {

			File Ficheiromac = new File(filePath);
			BufferedReader users = new BufferedReader(new FileReader("Users/users.txt"));
			byte[] password = pwdAdmin.getBytes();
			SecretKey key = new SecretKeySpec(password, "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			if (mac == null) {
				System.err.println("erro ao criar ficheiro mac");
				System.exit(-1);
			}

			mac.init(key);

			if (!Ficheiromac.exists()) {

				Ficheiromac.createNewFile();
				String linha = "";
				while ((linha = users.readLine()) != null) {
					byte[] ficheiroUsers = linha.getBytes();
					mac.update(ficheiroUsers);
				}
				mac.doFinal();
				users.close();
				BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
				String macConverter = DatatypeConverter.printHexBinary(mac.doFinal());
				writer.write(macConverter);
				writer.close();
				System.out.println("Ficheiro mac foi criado");

			} else {
				BufferedReader reader = new BufferedReader(new FileReader(Ficheiromac));

				String linha = "";
				while ((linha = users.readLine()) != null) {
					byte[] ficheiroUsers = linha.getBytes();
					mac.update(ficheiroUsers);
				}
				mac.doFinal();
				users.close();

				String macConverter = DatatypeConverter.printHexBinary(mac.doFinal());

				String comparacao = reader.readLine();
				if (macConverter.equals(comparacao)) {
					System.out.println("mac foi validado");
					reader.close();

				} else {
					System.out.println("mac invalido");
					reader.close();
					System.exit(-1);

				}

			}

		} catch (NoSuchAlgorithmException | InvalidKeyException | IOException | IllegalArgumentException e) {
			if (e instanceof IllegalArgumentException) {
				System.out.println("erro");
				System.exit(-1);
			}
		}

	}

	/**
	 * 
	 * @param pathKS
	 * @param password
	 * @return
	 */
	public SecretKey loadKeystore(String pathKS, char[] password) {
		KeyStore ks = null;
		try {
			// Cria uma keystore.
			ks = KeyStore.getInstance("JKS");
			File keystore = new File(pathKS);

			// Se a keystore existe, dar load desse path.
			if (keystore.exists()) {
				ks.load(new FileInputStream(pathKS), password);

			} else {
				// Se nï¿½o existe dar load com path a null e depois store para escrever no
				// ficheiro.
				ks.load(null, password);
				ks.store(new FileOutputStream(keystore), password);
			}
			// Cria a secretkey com a password dada e compara com a getKey da keystore
			SecretKey key = (SecretKey) ks.getKey("decifraTudo", password);

			// verifica se existe um mac
			if (key == null) {
				key = KeyGenerator.getInstance("HmacSHA256").generateKey();
				Certificate cert = ks.getCertificate("myserver");
				Certificate[] certarray = { cert };
				ks.setKeyEntry("decifraTudo", key, password, certarray);
				ks.store(new FileOutputStream("Users/users.keystore"), password);
				System.out.println("Mac criado.");
			} else {
				System.out.println("jah existe o mac");
				return key;

			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
				| UnrecoverableKeyException e) {
			if (e instanceof IOException) {
				if (e.getMessage().contains("password was incorrect")) {
					System.out.println("Password errada");
					System.exit(-1);
				}
			}
			e.printStackTrace();
		}
		return null;

	}

	public static void cifraOldFile(String file) throws InvalidKeyException, NoSuchAlgorithmException, IOException,
			NoSuchPaddingException, IllegalBlockSizeException, CertificateException {
		// gerar uma chave aleat�ria para utilizar com o AES
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		SecretKey key = kg.generateKey();

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, key);

		FileInputStream fis;
		FileOutputStream fos;

		fis = new FileInputStream(file + ".decif");
		fos = new FileOutputStream(file + ".cif", true); // , true Rescrever ficheiro cifrado.
		CipherOutputStream cos = new CipherOutputStream(fos, c);
		byte[] b = new byte[16];
		int i = fis.read(b);
		while (i > 0) {
			cos.write(b, 0, i);
			i = fis.read(b);
		}
		fis.close();
		cos.close();

		// Guardar key usada para cifrar
		guardarKey(key, file + ".key");
		File decif = new File(file + ".decif");
		if (decif.delete())
			System.out.println("Deu delete");
	}

}
