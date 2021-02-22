package com.sc010.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.sc010.utils.Utils;

public class PhotoShareServer {
	public static void main(String[] args) throws java.net.SocketException {
		File pasta = new File("servidor");
		if (!pasta.exists()) {
			pasta.mkdir();
		}
		// Setup keysure
		System.setProperty("java.security.policy", "server.policy");
		System.setSecurityManager(new SecurityManager());
		System.setProperty("javax.net.ssl.keyStore", "server");
		System.setProperty("javax.net.ssl.keyStorePassword", "paparuco");
		System.setProperty("javax.net.ssl.trustStore", "server");
		System.setProperty("javax.net.ssl.trustStorePassword", "paparuco");

		System.out.println("Inicializando o servidor");
		PhotoShareServer server = new PhotoShareServer();
		server.startServer();
	}

	/**
	 * Trata da comunicacao com o servidor
	 */
	public void startServer() {
		SSLServerSocket ss = null;
		try {
			Scanner sc = new Scanner(System.in);
			System.out.println("Introduza password de acesso");
			String pw = sc.nextLine();
			Utils.createMac("Users/users.mac", pw);
			sc.close();
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
			ss = (SSLServerSocket) ssf.createServerSocket(23232);
			System.out.println("Servidor a espera de ligacoes no porto " + ss.getLocalPort());

		} catch (IOException e) {
			System.out.println("O servidor PhotoShare so aceita ligacoes do porto : 23232");
			e.printStackTrace();
			System.exit(-1);
		}
		while (true) {
			try {
				ServerThread newServerThread = new ServerThread(ss.accept());
				newServerThread.start();
				// sSoc.close();
			} catch (IOException e) {
				e.printStackTrace();
				if (ss != null && !ss.isClosed()) {
					try {
						ss.close();
					} catch (IOException err) {
						err.printStackTrace();
					}
				}
			}
		}
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket socket2) {
			socket = socket2;
			System.out.println("Entrou novo cliente");
		}

		/**
		 * Run do server
		 */
		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				String inUser = "";
				String inPasswd = "";
				try {
					// Autenticacao do user + password que vem do cliente
					inUser = (String) inStream.readObject();
					inPasswd = (String) inStream.readObject();
					CatalogoUser catUser = new CatalogoUser();
					CatalogoPhotos photos = new CatalogoPhotos();
					autenticarUser(catUser, inUser, inPasswd, outStream, inStream);
					String operacao = (String) inStream.readObject();
					switch (operacao) {
					case "-a":
						operationA(inUser, inStream, outStream);
						break;
					case "-l":
						operationMiniL(inStream, outStream, catUser, inUser, photos);
						break;

					case "-i":
						operationI(inStream, outStream, catUser, inUser, photos);
						break;
					case "-g":
						operationG(inStream, outStream, catUser, inUser);
						break;
					case "-c":
						operationC(inStream, outStream, catUser, inUser, photos);
						break;
					case "-L":
						operationL(inStream, outStream, catUser, inUser, photos);
						break;
					case "-D":
						operationD(inStream, outStream, catUser, inUser, photos);
						break;
					case "-f":
						operationF(inStream, inUser, catUser, outStream);
						break;
					case "-r":
						operatioR(inStream, outStream, catUser, inUser);
						break;
					default:

					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				outStream.close();
				inStream.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Autenticacao do user
	 * 
	 * @param inUser
	 *            - user name
	 * @param inPasswd
	 *            - user password
	 * @param outStream
	 *            - stream out
	 * @param inStream
	 *            - stream in
	 * @return frase - retorno a dar ao client
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void autenticarUser(CatalogoUser catUser, String inUser, String inPasswd, ObjectOutputStream outStream,
			ObjectInputStream inStream) {
		try {
			String frase = "";
			// user existe
			if (catUser.find(inUser)) {
				if (catUser.pwdCerta(inUser, inPasswd)) {
					frase = "LOGGED";
					outStream.writeObject(frase);
				} else {// password errada
					frase = "WRONG";
					outStream.writeObject(frase);
					while (!catUser.getUserPwd(inUser).equals(inStream.readObject())) {
						frase = "WRONG";
						outStream.writeObject(frase);
					}
					outStream.writeObject("LOGGED");
				}
			}
		} catch (Exception e) {
			System.err.println("erro de autenticacao");
		}
	}

	/**
	 * Operacao -a
	 * 
	 * @param inUser
	 *            - user recebido na operacao
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 */
	public static void operationA(String inUser, ObjectInputStream inStream, ObjectOutputStream outStream) {
		try {
			String dirName = "servidor/" + inUser;
			File dir = new File(dirName);
			if (!dir.exists())
				dir.mkdir();
			String photo = (String) inStream.readObject();
			String temp = dirName + "/" + photo;
			boolean check = new File(temp + ".cif").exists();

			if (check == false) {
				// Se nao existe, criar e guardar na pasta
				outStream.writeObject("NAO EXISTE");
				// receber o necessario do cliente e criar
				FileOutputStream outStream1 = new FileOutputStream(temp);
				byte buffer[] = new byte[1024];
				int count;
				long size = (long) inStream.readObject();
				while ((count = inStream.read(buffer, 0, (int) (size < 1024 ? size : 1024))) > 0) {
					outStream1.write(buffer, 0, count);
					size -= count;
					outStream1.flush();
				}

				// Foto esta guardada.
				// Vamos cifrar a foto.
				outStream1.close();
				Utils.cifraFile(new File(temp));

				File like = new File("servidor/" + inUser + "/" + getNameFile(photo) + "Likes.txt");
				like.createNewFile();
				Utils.cifraFile(like);
				File dislike = new File("servidor/" + inUser + "/" + getNameFile(photo) + "Dislikes.txt");
				dislike.createNewFile();
				Utils.cifraFile(dislike);
				File comments = new File("servidor/" + inUser + "/" + getNameFile(photo) + "Comments.txt");
				comments.createNewFile();
				Utils.cifraFile(comments);
				outStream.writeObject("TRANSFERIDA");

				Utils.decifraFile("servidor/" + inUser + "/listaFotos.txt");

				BufferedReader br = new BufferedReader(new FileReader("servidor/" + inUser + "/listaFotos.txt.decif"));
				DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				Date today = Calendar.getInstance().getTime();
				String reportDate = df.format(today);
				String dateToPrintToFile = reportDate;
				BufferedWriter writer = new BufferedWriter(
						new FileWriter("servidor/" + inUser + "/listaFotos.txt.decif", true));
				writer.write(photo + ":" + dateToPrintToFile + System.getProperty("line.separator"));
				writer.close();
				br.close();

				Utils.cifraOldFile("servidor/" + inUser + "/listaFotos.txt");
			} else {
				System.out.println("existe o ficheiro");
				outStream.writeObject("EXISTE");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -f
	 * 
	 * @param inUser
	 *            - user recebido na operacao
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 */
	public static void operationF(ObjectInputStream inStream, String inUser, CatalogoUser catUser,
			ObjectOutputStream outStream) {
		try {
			String followerAdd = (String) inStream.readObject();
			User uAdd = catUser.getUser(inUser);
			System.out.println(inUser);
			if (catUser.find(followerAdd) == true) {// encontrar se o user exist na lista users

				if (uAdd.existsFollower(followerAdd) == true) {
					outStream.writeObject("Follower ja existe");
				} else {
					Utils.decifraFile("servidor/" + inUser + "/followers.txt");
					BufferedWriter writer = new BufferedWriter(
							new FileWriter("servidor/" + inUser + "/followers.txt.decif", true));
					writer.write(followerAdd);
					writer.newLine();
					writer.close();
					outStream.writeObject("Follower adicionado");
					Utils.cifraOldFile("servidor/" + inUser + "/followers.txt");
				}
			} else {
				outStream.writeObject("follower nao existe");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -r
	 * 
	 * @param inStream-
	 *            reader do cliente
	 * @param outStream-
	 *            writer para o cliente
	 * @param catUser-
	 *            catalogo de users
	 * @param inUser-
	 *            user recebido na operacao
	 * @throws Exception
	 */
	public static void operatioR(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser) throws Exception {
		try {
			String followerRemove = (String) inStream.readObject();
			User uRemove = catUser.getUser(inUser);
			File followRem = new File("servidor/" + inUser + "/followers.txt");
			if (catUser.find(followerRemove) == true) { // encontrar se o user exist na lista user
				if (uRemove.existsFollower(followerRemove) == true) {

					uRemove.removeFollowers(followRem, followerRemove);
					System.out.println("removido:");
					uRemove.imprime();
					File followCif = new File("servidor/" + inUser + "/followers.txt.cif");
					followCif.delete();
					File removidos = new File("servidor/" + inUser + "/followers.txt");
					removidos.createNewFile();
					uRemove.CreateFileRemoved(removidos, inUser);
					outStream.writeObject("Follower removido");
				} else {
					outStream.writeObject("Follower nao esta na lista");
				}
			} else {
				outStream.writeObject("Follower nao existe");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -L
	 * 
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 * @param inUser
	 *            - user recebido na operacao
	 * @param photos
	 *            - catalogo de fotos
	 */
	public static void operationL(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser, CatalogoPhotos photos) {
		try {

			String user = (String) inStream.readObject();
			String photoL = (String) inStream.readObject();
			File listaFotos = new File("servidor/" + user + "/listaFotos.txt");
			// verificar se e user
			if (catUser.find(user) == true) {
				User userLike = catUser.getUser(user);
				// verificamos se e follower
				if (userLike.existsFollower(inUser) == true) {
					photos.populate(listaFotos);
					if (photos.existsPhoto(photoL) == true) {
						// Se a foto existe, entao temos que a decifrar.
						File fichLikes = new File(
								"servidor/" + user + "/" + photoL.substring(0, photoL.indexOf(".")) + "Likes.txt");
						// Decifrar os likes
						Utils.decifraFile(fichLikes.toString());

						// Likes decifrados vamos ver se ja existe um user
						boolean found = false;
						BufferedReader br = new BufferedReader(new FileReader(fichLikes + ".decif"));
						for (String s : br.lines().collect(Collectors.toList())) {
							if (s.contains(inUser))
								found = true;
						}
						br.close();
						if (!found) {
							// Escrever no ficheiro .decif uma nova linha com o nome do user
							BufferedWriter bw = new BufferedWriter(new FileWriter(fichLikes + ".decif"));
							bw.write(inUser);
							bw.newLine();
							bw.close();

							Utils.cifraOldFile(fichLikes.getPath());
							outStream.writeObject("LIKE");
						} else {
							Utils.cifraOldFile(fichLikes.getPath());
							outStream.writeObject("JADEULIKE");
						}
					} else {
						outStream.writeObject("NAO FOTO");
					}
				} else {
					outStream.writeObject("NAO LIKE");
				}
			} else {
				outStream.writeObject("NAO USER");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (NoSuchPaddingException e1) {
			e1.printStackTrace();
		} catch (InvalidKeyException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			e1.printStackTrace();
		} catch (InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -D
	 * 
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 * @param inUser
	 *            - user recebido na operacao
	 * @param photos
	 *            - catalogo de fotos
	 * @throws Exception
	 */
	public static void operationD(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser, CatalogoPhotos photos) throws Exception {
		try {
			String userD = (String) inStream.readObject();
			String photoD = (String) inStream.readObject();
			File listaFotos = new File("servidor/" + userD + "/listaFotos.txt");

			// verificar se e user
			if (catUser.find(userD) == true) {
				User userDislike = catUser.getUser(userD);

				// verificamos se e follower
				if (userDislike.existsFollower(inUser) == true) {
					photos.populate(listaFotos);
					if (photos.existsPhoto(photoD) == true) {

						// Se a foto existe, entao temos que a decifrar.
						File fichDislikes = new File(
								"servidor/" + userD + "/" + photoD.substring(0, photoD.indexOf(".")) + "Dislikes.txt");
						// Decifrar os dislikes
						Utils.decifraFile(fichDislikes.toString());

						// Dislikes decifrados vamos ver se ja existe um user
						boolean found = false;
						BufferedReader br = new BufferedReader(new FileReader(fichDislikes + ".decif"));
						for (String s : br.lines().collect(Collectors.toList())) {
							if (s.contains(inUser))
								found = true;
						}
						br.close();

						if (!found) {
							// Escrever no ficheiro .decif uma nova linha com o nome do user
							BufferedWriter bw = new BufferedWriter(new FileWriter(fichDislikes + ".decif"));
							bw.write(inUser);
							bw.newLine();
							bw.close();
							Utils.cifraOldFile(fichDislikes.getPath());
							outStream.writeObject("DISLIKE");
						} else {
							Utils.cifraOldFile(fichDislikes.getPath());
							outStream.writeObject("JADEUDISLIKE");
						}
					} else {
						outStream.writeObject("NAO FOTO");
					}
				} else {
					outStream.writeObject("NAO DISLIKE");
				}
			} else {
				outStream.writeObject("NAO USER");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -c
	 * 
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 * @param inUser
	 *            - user recebido na operacao
	 * @param photos
	 *            - catalogo de fotos
	 */
	public static void operationC(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser, CatalogoPhotos photos) {
		try {
			String comentario = (String) inStream.readObject();
			String userC = (String) inStream.readObject();
			String photoC = (String) inStream.readObject();
			User userCcomment = catUser.getUser(userC);
			// verificar se e user
			if (catUser.find(userC) == true) {
				// verificamos se e follower
				if (userCcomment.existsFollower(inUser) == true) {
					File listaFotosC = new File("servidor/" + userC + "/listaFotos.txt");
					photos.populate(listaFotosC);
					if (photos.existsPhoto(photoC) == true) {
						Utils.decifraFile("servidor/" + userC + "/" + getNameFile(photoC) + "Comments.txt");
						BufferedWriter writer = new BufferedWriter(new FileWriter(
								"servidor/" + userC + "/" + getNameFile(photoC) + "Comments.txt.decif", true));
						writer.write(comentario);
						writer.newLine();
						writer.close();

						Utils.cifraOldFile("servidor/" + userC + "/" + getNameFile(photoC) + "Comments.txt");
						outStream.writeObject("COMMENT");
					} else {
						outStream.writeObject("NAO FOTO");
					}
				} else {
					outStream.writeObject("NAO FOLLOWER");
				}
			} else {
				outStream.writeObject("NAO USER");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/// TODO devolve ao cliente o numero de likes , dislikes e os comentarios
	/**
	 * Operacao -i
	 * 
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 * @param inUser
	 *            - user recebido na operacao
	 * @param photos
	 *            - catalogo de fotos
	 * @throws Exception
	 */
	public static void operationI(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser, CatalogoPhotos photos) throws Exception {
		try {
			String userID = (String) inStream.readObject();
			String foto = (String) inStream.readObject();
			User userCatalog = catUser.getUser(userID);
			if (catUser.find(userID) == true) {
				if (userCatalog.existsFollower(inUser) == true) {
					File listaFotosC = new File("servidor/" + userID + "/listaFotos.txt");
					photos.populate(listaFotosC);
					if (photos.existsPhoto(foto) == true) {
						outStream.writeObject("MOSTRA");
						// populate Likes
						Photo phototempL = photos.getPhoto(foto);
						File ficheiroLikes = new File("servidor/" + userID + "/" + getNameFile(foto) + "Likes.txt");
						phototempL.populateLikes(ficheiroLikes);

						// populate Dislikes
						Photo phototempD = photos.getPhoto(foto);
						File ficheiroDislikes = new File(
								"servidor/" + userID + "/" + getNameFile(foto) + "Dislikes.txt");

						phototempD.populateDislikes(ficheiroDislikes);

						// populate comentarios
						Photo phototempC = photos.getPhoto(foto);
						File ficheiroComments = new File(
								"servidor/" + userID + "/" + getNameFile(foto) + "Comments.txt");
						phototempC.populateComments(ficheiroComments);
						ArrayList<String> comentarios = phototempC.getlistPhotoComments();
						outStream.writeObject(phototempC.tamanholistPhotoComments());
						for (int i = 0; i < comentarios.size(); i++) {
							outStream.writeObject(comentarios.get(i));
						}
						outStream.writeObject(phototempL.tamanholistPhotoLikes());
						outStream.writeObject(phototempD.tamanholistPhotoDislikes());

					} else {
						outStream.writeObject("NAO FOTO");
					}
				} else {
					outStream.writeObject("NAO FOLLOWER");
				}
			} else {
				outStream.writeObject("NAO USER");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -l
	 * 
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 * @param inUser
	 *            - user recebido na operacao
	 * @param photos
	 *            - catalogo de fotos
	 */
	public static void operationMiniL(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser, CatalogoPhotos photos) {
		try {
			String userPhotos = (String) inStream.readObject();
			// catalogo para fotos
			User userList = catUser.getUser(userPhotos);
			if (catUser.find(userPhotos) == true) {
				File followers = new File("servidor/" + userPhotos + "/followers.txt");
				userList.populateFollowers(followers);
				if (userList.existsFollower(inUser) == true) {
					outStream.writeObject("EXISTE");
					File photoList = new File("servidor/" + userPhotos + "/listaFotos.txt");
					photos.populate(photoList);
					ArrayList<Photo> fotos = photos.listaFotos();
					System.out.println("TAMANHO DO SIZE: " + photos.listaFotos().size());
					outStream.writeObject(photos.listaFotos().size());
					for (int i = 0; i < photos.listaFotos().size(); i++) {
						outStream.writeObject(fotos.get(i).getNome() + " - " + fotos.get(i).getData());
					}
				} else {
					outStream.writeObject("NAO EXISTE");
				}
			} else {
				outStream.writeObject("NAO EXISTE USER");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/// TODO devolve para o cliente as fotos
	/**
	 * Operacao -g
	 * 
	 * @param inStream
	 *            - reader do cliente
	 * @param outStream
	 *            - writer para o cliente
	 * @param catUser
	 *            - catalogo de users
	 * @param inUser
	 *            - user recebido na operacao
	 */
	public static void operationG(ObjectInputStream inStream, ObjectOutputStream outStream, CatalogoUser catUser,
			String inUser) {
		try {
			String userG = (String) inStream.readObject();
			User u = catUser.getUser(userG);
			if (catUser.find(userG) == true) {
				File followC = new File("servidor/" + userG + "/followers.txt");
				u.populateFollowers(followC);
				u.imprime();
				if (u.existsFollower(inUser)) {
					File folderDir = new File("servidor/" + userG);
					String[] folderFiles = folderDir.list();

					ArrayList<String> listaDeFotos = getPhotoFiles(folderFiles);
					outStream.write(listaDeFotos.size());
					outStream.writeObject("Fotos enviadas");

					for (int i = 0; i < listaDeFotos.size(); i++) {
						outStream.writeObject(listaDeFotos.get(i).substring(0, listaDeFotos.get(i).length() - 4));
						File file = new File("servidor/" + userG + "/"
								+ listaDeFotos.get(i).substring(0, listaDeFotos.get(i).length() - 4));
						if (file.getPath().contains("Comments.txt")) {
							Utils.decifraFile(file.getPath());
						} else {
							Utils.decifraFoto(file.getPath());
						}

						// falta ler o fichiero para terminar de decifrar
						File fileDecif = new File("servidor/" + userG + "/"
								+ listaDeFotos.get(i).substring(0, listaDeFotos.get(i).length() - 4) + ".decif");
						long size = fileDecif.length();
						FileInputStream inStream1 = new FileInputStream(fileDecif);
						byte buffer[] = new byte[1024];
						int count = 1024;
						outStream.writeObject(size);
						while ((count = inStream1.read(buffer, 0, (int) (size < 1024 ? size : 1024))) > 0) {
							outStream.write(buffer, 0, count);
							size -= count;
							outStream.flush();
						}
						inStream1.close();
						Utils.cifraOldFile(file.getPath());
					}
				} else {
					outStream.writeObject("Nao Follower");
				}
			} else {
				outStream.writeObject("Nao e user");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Adiciona ficheiros imagem para um arrayList
	 * 
	 * @param ficheiros
	 * @return files
	 */
	private static ArrayList<String> getPhotoFiles(String[] ficheiros) {
		ArrayList<String> files = new ArrayList<String>();

		for (int i = 0; i < ficheiros.length; i++) {
			Pattern p = Pattern.compile("(gif.cif|jpg.cif|jpeg.cif|tiff.cif|png.cif)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(ficheiros[i]);
			if (m.find() || ficheiros[i].endsWith("Comments.txt.cif")) {
				files.add(ficheiros[i]);
			}

		}
		return files;
	}

	/**
	 * Get nome do file
	 * 
	 * @param photo
	 * @return str
	 */
	private static String getNameFile(String photo) {
		int ind = photo.indexOf(".");
		String str = photo.substring(0, ind);
		return str;

	}
}
