package es.florida.avaluableSocket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Classe que representa una petició gestionada per un fil en el servidor.
 */
public class Peticio implements Runnable {

	/**
	 * Socket associat a la petició.
	 */
	private Socket socket;

	/**
	 * Nom d'usuari associat a la petició.
	 */
	private String user;

	/**
	 * Llista de peticions compartida entre els fils del servidor.
	 */
	public List<Peticio> llistaPeticions = new ArrayList<Peticio>();

	/**
	 * Constructor que inicialitza la petició amb el socket i la llista de
	 * peticions.
	 *
	 * @param socket          Socket associat a la petició.
	 * @param llistaPeticions Llista de peticions compartida entre els fils del
	 *                        servidor.
	 */
	public Peticio(Socket socket, List<Peticio> llistaPeticions) {
		this.socket = socket;
		this.llistaPeticions = llistaPeticions;
	}

	/**
	 * Obté el nom d'usuari associat a la petició.
	 *
	 * @return Nom d'usuari associat a la petició.
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Mètode principal que s'executa en un fil per a gestionar la comunicació amb
	 * el client.
	 */
	public void run() {
		try {
			boolean autoritzat = false;
			InputStream is = socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader bfr = new BufferedReader(isr);

			while (!autoritzat) {
				String usuari = bfr.readLine();
				String contrasenya = bfr.readLine();

				autoritzat = comprovarAutoritzacio(usuari, contrasenya);
				String missatge = "";
				int estat;

				if (autoritzat) {
					if (usuariConnectat(usuari)) {
						estat = 0;
						autoritzat = false;
					} else {
						estat = 1;
						this.user = usuari;
					}
				} else {
					estat = 2;
				}

				switch (estat) {
				case 0:
					missatge = "0";
					System.err.println(obtindreTimeStamp() + " - " + "SERVER >>> " + usuari + ": JA ESTA CONECTAT");
					break;
				case 1:
					missatge = "1";
					System.err.println(obtindreTimeStamp() + " - " + "SERVER >>> " + usuari + ": AUTORITZAT");
					break;
				case 2:
					missatge = "2";
					System.err.println(obtindreTimeStamp() + " - " + "SERVER >>> " + usuari
							+ ": ERROR: USUARI O CONTRASENYA INCORRECTES");
					break;
				}

				OutputStream os = socket.getOutputStream();
				PrintWriter pw = new PrintWriter(os);
				pw.write(missatge + "\n");
				pw.flush();
			}

			boolean eixir = false;

			while (!eixir) {
				String opcio = bfr.readLine();
				System.out.println(obtindreTimeStamp() + " - " + this.user + ": " + opcio);

				if (opcio.equals("?")) {
					OutputStream os = socket.getOutputStream();
					PrintWriter pw = new PrintWriter(os);
					pw.write("\nUSUARIS ACTIUS:\n");

					StringBuilder users = new StringBuilder();

					synchronized (llistaPeticions) {
						for (Peticio peticio : llistaPeticions) {
							if (peticio.getUser() != null) {
								users.append(peticio.user).append(" | ");
							}
						}
					}
					if (users.length() > 0) {
						users.setLength(users.length() - 3);
					}

					pw.write(users.toString() + "\n");
					pw.flush();

				} else if (opcio.startsWith("@")) {
					OutputStream os = socket.getOutputStream();
					PrintWriter pw = new PrintWriter(os);

					String[] parts = opcio.split(" ", 2);
					if (parts.length == 2) {
						String usuari = parts[0].substring(1);
						String missatge = parts[1].trim();

						synchronized (llistaPeticions) {
							if (!verificarMissatgeBuit(missatge)) {
								if (usuariConnectat(usuari)) {
									for (Peticio peticio : llistaPeticions) {
										if (peticio.getUser() != null && peticio.getUser().equals(usuari)
												&& !peticio.getUser().equals(user)) {
											peticio.enviarMissatge(" - @" + user + ": " + missatge);
										}
									}
								} else {
									pw.write("ERROR: L'usuari " + usuari + " no està connectat.\n");
									pw.flush();
								}
							} else {
								pw.write("ERROR: El missatge no pot estar en blanc\n");
								pw.flush();
							}
						}
					} else {
						pw.write("ERROR: Format incorrecte. Utilitza @usuari missatge\n");
						pw.flush();
					}

				} else if (opcio.equalsIgnoreCase("exit")) {
					synchronized (llistaPeticions) {
						int userDesconectat = -1;
						for (int i = 0; i < llistaPeticions.size(); i++) {
							if (llistaPeticions.get(i).getUser() != null) {
								if (llistaPeticions.get(i).getUser().equals(user)) {
									userDesconectat = i;
									break;
								}
							}
						}
						if (userDesconectat >= 0)
							System.err.println(
									obtindreTimeStamp() + " - " + "SERVER >>> " + this.user + ": S'HA DESCONECTAT");
						llistaPeticions.remove(userDesconectat);
					}
					eixir = true;

				} else {
					OutputStream os = socket.getOutputStream();
					PrintWriter pw = new PrintWriter(os);
					synchronized (llistaPeticions) {
						if (!verificarMissatgeBuit(opcio)) {
							for (Peticio peticio : llistaPeticions) {
								if (peticio.getUser() != null) {
									if (!peticio.getUser().equals(user)) {
										peticio.enviarMissatge(" - @" + user + ": " + opcio);
									}
								}
							}
						} else {
							pw.write("ERROR: El missatge no pot estar en blanc\n");
							pw.flush();
						}
					}
				}
			}
			this.socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(obtindreTimeStamp() + " - " + "SERVER >>> Error.");
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
	 * Comprova si un usuari determinat està connectat actualment.
	 *
	 * @param usuari Nom d'usuari a comprovar.
	 * @return True si l'usuari està connectat, False altrament.
	 */
	public boolean usuariConnectat(String usuari) {
		boolean connectat = false;
		for (Peticio peticio : llistaPeticions) {
			if (peticio.getUser() != null) {
				if (peticio.getUser().equals(usuari))
					connectat = true;
			}
		}
		return connectat;
	}

	/**
	 * Comprova l'autorització d'un usuari utilitzant el fitxer d'autoritzacions.
	 *
	 * @param usuari Nom d'usuari a comprovar.
	 * @param contra Contrasenya associada a l'usuari.
	 * @return True si l'usuari està autoritzat, False altrament.
	 */
	@SuppressWarnings("resource")
	public boolean comprovarAutoritzacio(String usuari, String contra) {
		String rutaAutoritzats = new File("autoritzats.txt").getAbsolutePath();
		File autoritzacions = new File(rutaAutoritzats);
		boolean autoritzat = false;

		try {
			FileReader fr = new FileReader(autoritzacions);
			BufferedReader br = new BufferedReader(fr);

			String line = br.readLine();
			while (line != null) {
				String[] obj = line.split(";");
				if (obj[0].equals(usuari) && obj[1].equals(contra)) {
					autoritzat = true;
				}
				line = br.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("SERVER >>> Error.");
		}

		return autoritzat;
	}

	/**
	 * Envia un missatge a l'usuari associat a la petició.
	 *
	 * @param missatge Missatge a enviar a l'usuari.
	 * @throws IOException Llançada en cas d'error d'entrada/sortida.
	 */
	public void enviarMissatge(String missatge) throws IOException {
		OutputStream os = socket.getOutputStream();
		PrintWriter pw = new PrintWriter(os);

		if (!missatge.trim().isEmpty()) {
			pw.write(obtindreTimeStamp() + missatge + "\n");
			pw.flush();
		} else {
			pw.write("ERROR: No pots enviar un missatge buit.\n");
			pw.flush();
		}
	}

	/**
	 * Verifica si un missatge està buit.
	 *
	 * @param missatge Missatge a verificar.
	 * @return True si el missatge està buit, False altrament.
	 */
	public boolean verificarMissatgeBuit(String missatge) {
		return (missatge == null || missatge.trim().isEmpty());
	}
}
