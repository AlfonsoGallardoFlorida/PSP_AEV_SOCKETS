package es.florida.avaluableSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa un servidor que escolta connexions i gestiona
 * peticions.
 */
public class Server {

	/**
	 * Socket utilitzat pel servidor.
	 */
	@SuppressWarnings("unused")
	private static Socket socket;

	/**
	 * Llista de peticions gestionades pel servidor.
	 */
	public static List<Peticio> llistaPeticions = new ArrayList<Peticio>();

	/**
	 * Mètode principal que inicialitza i gestiona el servidor.
	 *
	 * @param args Arguments de la línia de comandes (no s'utilitzen en aquest cas).
	 * @throws IOException Llançada en cas d'error d'entrada/sortida.
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {

		System.err.println("SERVER >>> Arranca servidor, espera peticio");
		ServerSocket socketEscolta = null;
		try {
			// Intenta crear un servidor de sockets a l'port 5000
			socketEscolta = new ServerSocket(5000);
		} catch (IOException e) {
			System.err.println("SERVER >>> Error");
			return;
		}

		// Bucle infinit que escolta noves connexions i gestiona peticions
		while (true) {
			Socket connexio = socketEscolta.accept();
			System.err.println("SERVER >>> Connexio rebuda - Llança Peticio");
			Peticio p = new Peticio(connexio, llistaPeticions);
			llistaPeticions.add(p);
			Thread fil = new Thread(p);
			fil.start();
		}
	}
}
