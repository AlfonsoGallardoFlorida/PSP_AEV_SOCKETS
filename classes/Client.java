package es.florida.avaluableSocket;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Scanner;

/**
 * Classe que representa un client que es connecta a un servidor per a
 * intercanviar missatges.
 */
public class Client implements Runnable {

	/**
	 * Socket utilitzat pel client per a establir connexió amb el servidor.
	 */
	private static Socket socket;

	/**
	 * Constructor predeterminat del client.
	 */
	public Client() {

	}

	/**
	 * Mètode principal que inicia el client, es connecta al servidor i gestiona la
	 * comunicació.
	 *
	 * @param args Arguments de la línia de comandes (no s'utilitzen en aquest cas).
	 */
	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		try {
			// Estableix una connexió amb el servidor a través de la direcció "localhost" i
			// el port 5000
			InetSocketAddress direccio = new InetSocketAddress("localhost", 5000);
			socket = new Socket();
			socket.connect(direccio);

			boolean autoritzat = false;

			while (!autoritzat) {
				System.out.println("Usuari:");
				String usuari = sc.nextLine();

				System.out.println("Contrasenya:");
				String contrasenya = sc.nextLine();

				try {
					// Envia les credencials al servidor
					OutputStream os = socket.getOutputStream();
					PrintWriter pw = new PrintWriter(os);
					pw.write(usuari + "\n");
					pw.write(contrasenya + "\n");
					pw.flush();

					// Rep la resposta del servidor
					InputStream is = socket.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader bfr = new BufferedReader(isr);
					String resposta = bfr.readLine();

					// Gestiona la resposta del servidor
					switch (resposta) {
					case "0":
						System.out.println(obtindreTimeStamp() + " - " + "AQUEST USUARI JA ESTA CONNECTAT.");
						break;
					case "1":
						autoritzat = true;
						System.out.println(obtindreTimeStamp() + " - " + "USUARI @" + usuari + ": SESSIO INICIADA.");
						break;
					case "2":
						System.out.println(obtindreTimeStamp() + " - " + "USUARI @" + usuari
								+ ": USUARI O CONTRASENYA INCORRECTES.");
						break;
					default:
						System.out.println(obtindreTimeStamp() + " - " + "Resposta no vàlida del servidor.");
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Error al comunicarse con el servidor.");
					break;
				}
			}

			// Inicia un fil per a rebre i mostrar missatges del servidor
			boolean eixir = false;
			Client c = new Client();
			Thread fil = new Thread(c);
			fil.start();

			System.out.println("\nOPCIONS");
			System.out.println("'?' - Vore els usuaris actius");
			System.out.println("'@usuari + missatge' - Enviar un missatge privat.");
			System.out.println("'missatge' - Enviar un missatge a tots els usuaris actius.");
			System.out.println("'exit' - Eixir del programa. \n");

			while (!eixir) {
				String opcio = sc.nextLine();
				OutputStream os = socket.getOutputStream();
				PrintWriter pw = new PrintWriter(os);
				pw.write(opcio + "\n");
				pw.flush();

				if (opcio.equalsIgnoreCase("exit")) {
					eixir = true;
				}
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("SERVER >>> Error.");
		}
	}

	/**
	 * Obté la data i l'hora actual en el format especificat.
	 *
	 * @return String que representa la data i l'hora actual en format de text.
	 */
	public static String obtindreTimeStamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HH:mm:ss");
		return sdf.format(new Date());
	}

	/**
	 * Mètode que s'executa en un fil per a rebre i mostrar els missatges del
	 * servidor.
	 */
	public void run() {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String llinia;
			while ((llinia = reader.readLine()) != null) {
				System.out.println(llinia);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
