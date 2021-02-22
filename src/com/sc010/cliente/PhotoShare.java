package com.sc010.cliente;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class PhotoShare {

	private static final String IPPort = "(\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}):(\\d{1,5})";
	private static final Pattern PATTERN = Pattern.compile(IPPort);
	static SocketFactory sf = null;
	static SSLSocket listeningSocket = null;

	public static void main(String[] args) {

		File pasta = new File("Clientes");
		if (!pasta.exists()) {
			pasta.mkdir();
		}
		
		File userDir = new File(pasta + "/" + args[0]);
		if(!userDir.exists()) {
			userDir.mkdir();
		}
		// Setup keysure
		System.setProperty("java.security.policy", "client.policy");
		System.setSecurityManager(new SecurityManager());
		System.setProperty("javax.net.ssl.trustStore", "client");
		System.setProperty("javax.net.ssl.trustStorePassword", "paparuco");
		
		// socket, argumentos, arguments, serverAdress
		Socket listeningSocket = null;
		String[] argumentos = args;
		Scanner input = new Scanner(System.in);
		String[] arguments = verificaArgs(argumentos, input);
		String[] serverAdress = arguments[2].split(":");
		
		try {

			sf = SSLSocketFactory.getDefault();
			listeningSocket = (SSLSocket) sf.createSocket(serverAdress[0], Integer.parseInt(serverAdress[1]));

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try {
			ObjectInputStream in = new ObjectInputStream(listeningSocket.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(listeningSocket.getOutputStream());
			out.writeObject(arguments[0]);
			out.writeObject(arguments[1]);

			// faz verificacoes de logIn appos a resposta do servidor
			logIn(in, out, arguments, input);

			System.out.println("escolha uma operacao:");
			System.out.println("[ -a <photos> | -l <userId> | -i <userId> <photo> | -g <userId> \n"
					+ "| -c <comment> <userId> <photo> | -L <userId> <photo> | \n -D <userId> <photo> | -f <followUserIds> | -r <followUserIds> ]");

			String operacao = input.nextLine();
			String[] operacoesArgs = operacao.split(" ");

			switch (operacoesArgs[0]) {
			case "-a":
				operationA(arguments, operacoesArgs, out, in);
				break;
			case "-l":
				operationMiniL(out, in, operacoesArgs);
				break;

			case "-i":
				operationI(operacoesArgs, out, in);
				break;
			case "-g":
				operationG(operacoesArgs, in, out, arguments);
				break;

			case "-c":
				operationC(operacoesArgs, out, in);
				break;

			case "-L":
				operationL(in, out, operacoesArgs);
				break;
			case "-D":
				operationD(out, operacoesArgs, in);
				break;

			case "-f":
				operationF(in, out, operacoesArgs);
				break;

			case "-r":
				operationR(out, operacoesArgs, in);

				break;
			default:
			}

			System.out.println("Nao pode realizar mais operacoes");

			input.close();
			in.close();
			out.close();
		} catch (Exception e) {
			try {
				listeningSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Verifica argumentos de entrada
	 * 
	 * @param args
	 *            - argumentos na consola
	 * @param password
	 *            - password do user
	 * @param localUserID
	 *            - username
	 * @param serverAdress
	 *            - ip:porto para ligacao
	 * @param in
	 *            - scanner de leitura
	 */
	private static String[] verificaArgs(String[] args, Scanner in) {
		String[] result = new String[3];
		if (args.length < 3) { // nao contiver o necessario
			if (args.length <= 1) {
				System.out.println(" PhotoShare <localUserID> <password> <serverAdress>");
			} else if (args.length == 2) {
				result[0] = args[0];
				if (validate(args[1])) {
					result[2] = args[1];
					String pass = "";
					while (pass.equals("")) {
						System.out.println("Falta a password volte a inserir:");
						pass = in.nextLine();
					}
					result[1] = pass;
				} else {
					System.out.println("Falta Ip:Port");

					result[2] = in.nextLine();
				}
			}
		} else {
			result[0] = args[0];
			result[1] = args[1];
			result[2] = args[2];
		}
		return result;
	}

	/**
	 * Verificacao de login do user local
	 * 
	 * @param in
	 *            - InputStream
	 * @param out
	 *            - OutputStream
	 * @param arguments
	 *            - String de argumentos lidos
	 * @param input
	 *            - Scanner
	 */
	public static void logIn(ObjectInputStream in, ObjectOutputStream out, String[] arguments, Scanner input) {
		try {
			String var = (String) in.readObject();

			// user e pass -> OK
			if (var.equals("LOGGED")) {
				System.out.println("Bem Vindo " + arguments[0] + "!");
				// user OK mas password NOK
			} else if (var.equals("WRONG")) {
				// pede a pass ate ficar correta
				System.out.println("palavra passe incorrecta");
				String password = input.nextLine();
				out.writeObject(password);
				while (!((String) in.readObject()).equals("LOGGED")) {
					System.out.println("falhei");
					password = input.nextLine();
					out.writeObject(password);
				}
				// user NOK , entao criar um user novo
			} else if (var.equals("CREATE")) {
				System.out.println(" O user novo foi criado");
				String dirName = "Clientes/" + arguments[0];
				File dir = new File(dirName);
				dir.mkdir();
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Operacao -a
	 * 
	 * @param arguments
	 *            - Recebe todos os argumentos
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param out
	 *            - OutputStream
	 * @param in
	 *            - InputStream
	 */
	public static void operationA(String[] arguments, String[] operacoesArgs, ObjectOutputStream out,
			ObjectInputStream in) {
		// envia nome do ficheiro(exemplo: a.jpg)
		// argumento da foto
		try {
			File foto = new File("Clientes/" + arguments[0] + "/" + operacoesArgs[1]);
			long size = foto.length();
			if (foto.exists()) {
				out.writeObject(operacoesArgs[0]);
				out.writeObject(operacoesArgs[1]);
				String existe = (String) in.readObject();
				if (existe.equals("NAO EXISTE")) {
					FileInputStream inStream1 = new FileInputStream(foto);
					// InputStream inStream2 = new BufferedInputStream(inStream1);
					byte buffer[] = new byte[1024];
					int count = 1024;
					out.writeObject(foto.length());
					while ((count = inStream1.read(buffer, 0, (int) (size < 1024 ? size : 1024))) > 0) {
						out.write(buffer, 0, count);
						size -= count;
						out.flush();
					}
					String transfer = (String) in.readObject();
					if (transfer.equals("TRANSFERIDA")) {
						System.out.println("A foto foi transferida com sucesso");
					}
					inStream1.close();
				} else {
					out.writeObject(operacoesArgs[0]);

					System.out.println("ja existe a foto");
				}
			} else {
				// envia uma operacao que nao existe para poder dar exit
				// out.writeObject("--");
				System.out.println("A foto que quer enviar nao existe");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Operacao -f
	 * 
	 * @param out
	 *            - OutputStream
	 * @param in
	 *            - InputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 */
	public static void operationF(ObjectInputStream in, ObjectOutputStream out, String[] operacoesArgs) {
		try {
			out.writeObject(operacoesArgs[0]);
			out.writeObject(operacoesArgs[1]);
			// Ler reposta do server : adicionado ou ja existente
			String respostaAdd = (String) in.readObject();
			if (respostaAdd.equals("Follower adicionado")) {
				System.out.println(respostaAdd);
			} else if (respostaAdd.equals("Follower ja existe")) {
				System.out.println(respostaAdd);
			} else {
				System.out.println(respostaAdd);
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Operacao -r
	 * 
	 * @param out-
	 *            OutputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param in-
	 *            InputStream
	 */
	public static void operationR(ObjectOutputStream out, String[] operacoesArgs, ObjectInputStream in) {
		try {
			out.writeObject(operacoesArgs[0]);
			out.writeObject(operacoesArgs[1]);
			// Ler reposta do server : removido ou nunca existiu
			String respostaRem = (String) in.readObject();
			// System.out.println(respostaRem);
			if (respostaRem.equals("Follower removido"))
				System.out.println(respostaRem);
			else if (respostaRem.equals("Follower nao esta na lista")) {
				System.out.println(respostaRem);
			} else {
				System.out.println(respostaRem);
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
	 * @param out-
	 *            OutputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param in-
	 *            InputStream
	 */
	public static void operationL(ObjectInputStream in, ObjectOutputStream out, String[] operacoesArgs) {

		try {
			out.writeObject(operacoesArgs[0]);
			// user
			out.writeObject(operacoesArgs[1]);
			// photo
			out.writeObject(operacoesArgs[2]);
			String like = (String) in.readObject();
			if (like.equals("LIKE")) {
				System.out.println("Like efectuado com sucesso");

			} else if (like.equals("JADEULIKE")) {
				System.out.println("Já deu like anteriormente");

			} else if (like.equals("NAO LIKE")) {
				System.out.println("Nao e follower deste user");

			} else if (like.equals("NAO FOTO")) {
				System.out.println("Nao existe a foto que pretende");
			} else {
				System.out.println("User inválido");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Operacao -D
	 * 
	 * @param out-
	 *            OutputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param in-
	 *            InputStream
	 */
	public static void operationD(ObjectOutputStream out, String[] operacoesArgs, ObjectInputStream in) {
		try {
			out.writeObject(operacoesArgs[0]);
			// user
			out.writeObject(operacoesArgs[1]);
			// photo
			out.writeObject(operacoesArgs[2]);
			String dislike = (String) in.readObject();
			if (dislike.equals("DISLIKE")) {
				System.out.println("Dislike efectuado com sucesso");

			} else if (dislike.equals("JADEUDISLIKE")) {
				System.out.println("Já deu dislike anteriormente");

			} else if (dislike.equals("NAO DISLIKE")) {
				System.out.println("Nao e follower deste user");

			} else if (dislike.equals("NAO FOTO")) {
				System.out.println("Nao existe a foto que pretende");
			} else {
				System.out.println("User inválido");
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
	 * @param out-
	 *            OutputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param in-
	 *            InputStream
	 */
	public static void operationC(String[] operacoesArgs, ObjectOutputStream out, ObjectInputStream in) {
		try {
			String comment = concatenateComment(operacoesArgs);
			int len = operacoesArgs.length;
			out.writeObject(operacoesArgs[0]);
			// user
			out.writeObject(comment);

			out.writeObject(operacoesArgs[len - 2]);
			// photo
			out.writeObject(operacoesArgs[len - 1]);
			String comentario = (String) in.readObject();
			if (comentario.equals("COMMENT")) {
				System.out.println("Comentario efectuado com sucesso");
			} else if (comentario.equals("NAO FOLLOWER")) {
				System.out.println("Nao e follower deste user");
			} else if (comentario.equals("NAO FOTO")) {
				System.out.println("Nao existe a foto que pretende");
			} else {
				System.out.println("User inválido");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Operacao -i
	 * 
	 * @param out-
	 *            OutputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param in-
	 *            InputStream
	 */
	public static void operationI(String[] operacoesArgs, ObjectOutputStream out, ObjectInputStream in) {
		try {
			out.writeObject(operacoesArgs[0]);
			// user
			out.writeObject(operacoesArgs[1]);
			// photo
			out.writeObject(operacoesArgs[2]);
			String mostra = (String) in.readObject();
			if (mostra.equals("MOSTRA")) {

				int commentSize = (int) in.readObject();
				System.out.println("Lista de comentarios:");
				for (int i = 0; i < commentSize; i++) {
					String comentario = (String) in.readObject();
					System.out.println(comentario);
				}

				int likesSize = (int) in.readObject();

				int dislikeSize = (int) in.readObject();

				System.out.println("Numero de likes: " + likesSize);
				System.out.println("Numero de dislikes: " + dislikeSize);

			} else if (mostra.equals("NAO FOTO")) {
				System.out.println("Nao existe a foto");
			} else if (mostra.equals("NAO FOLLOWER")) {
				System.out.println("Nao e follower deste user");

			} else {
				System.out.println("User inválido");

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
	 * @param out-
	 *            OutputStream
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param in-
	 *            InputStream
	 */
	public static void operationMiniL(ObjectOutputStream out, ObjectInputStream in, String[] operacoesArgs) {
		try {
			out.writeObject(operacoesArgs[0]);
			out.writeObject(operacoesArgs[1]);
			String result = (String) in.readObject();
			if (result.equals("EXISTE")) {
				int tamanho = (int) in.readObject();
				System.out.println("Lista de fotos:");
				for (int i = 0; i < tamanho; i++) {
					String nomeData = (String) in.readObject();
					System.out.println(nomeData);
				}

			} else if (result.equals("NAO EXISTE USER")) {
				System.out.println("O user que introduziu nao existe");

			} else {
				System.out.println("Nao é follower");
			}
		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Operacao -g
	 * 
	 * @param arguments
	 *            - Recebe todos os argumentos
	 * @param operacoesArgs
	 *            - Recebe a operacao separadamente
	 * @param out
	 *            - OutputStream
	 * @param in
	 *            - InputStream
	 */
	public static void operationG(String[] operacoesArgs, ObjectInputStream in, ObjectOutputStream out,
			String[] arguments) {

		try {
			out.writeObject(operacoesArgs[0]);
			// user
			out.writeObject(operacoesArgs[1]);

			int vector = in.read();
			String msg = (String) in.readObject();
			if (msg.equals("Fotos enviadas")) {
				for (int i = 0; i < vector; i++) {
					String fotografia = (String) in.readObject();
					FileOutputStream outStream1 = new FileOutputStream("Clientes/" + arguments[0] + "/" + fotografia);
					OutputStream outStream2 = new BufferedOutputStream(outStream1);
					byte buffer[] = new byte[1024];
					int count;
					long leng = (long) in.readObject();
					while ((count = in.read(buffer, 0, (int) (leng < 1024 ? leng : 1024))) > 0) {
						outStream1.write(buffer, 0, count);
						leng -= count;
						outStream2.flush();
					}
					outStream2.close();
				}
				System.out.println("Transferencia efectuada com sucesso");

			} else if (msg.equals("Nao Follower")) {
				System.out.println("Nao e follower");
			} else {
				System.out.println("Nao e user");
			}

		} catch (IOException e) {
			System.err.println("erro de leitura");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Validacao de pattern
	 * 
	 * @param ip
	 *            - ip introduzido
	 * @return pattern
	 */
	public static boolean validate(final String ip) {
		return PATTERN.matcher(ip).matches();
	}

	/**
	 * Concatena comentarios
	 * 
	 * @param args
	 *            - argumento comentarios
	 * @return comentario concatenado
	 */
	public static String concatenateComment(String[] args) {
		String comentario = "";
		for (int i = 1; i < args.length - 2; i++) {
			comentario += args[i] + " ";
		}
		return comentario;
	}
}
