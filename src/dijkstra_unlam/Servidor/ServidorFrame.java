package dijkstra_unlam.Servidor;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import dijkstra_paqueteEnvio.Pedido;
import dijkstra_paqueteEnvio.Usuarios;
import login.HibernateApp;
import login.Usuario;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.google.gson.Gson;

import javax.swing.JTextArea;

public class ServidorFrame extends JFrame implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JPanel contentPane;
	private DataInputStream entrada;
	// private ObjectInputStream entrada;
	private ServerSocket server; // Prepara un puente para que se conecten otros sockets
	private Socket socket; // Prepara un puente para conectarse a un serverSocket
	private int puertoClienteAServidor = 9998; // El puerto por el que acceden los datos
	private int puertoServidorACliente = 9996;
	private int puertoParaConexionesActivas = 9994;
	private int puertoParaIngresoDeUsuarios = 9890;
	private int puertoParaValidacionDeUsuarios = 9894;
	private int puertoParaControlDeSalas = 9895;

	private ArrayList<String> listaDeSalas;
	private ArrayList<Usuarios> listaDeSockets;
	private ArrayList<Integer> tiempoDeSockets;

	private JComboBox<String> cb;
	private JTextArea textArea;

	private boolean hilo1Work = false;

	private boolean hilo2Work = false;

	public ServidorFrame() {
		listaDeSockets = new ArrayList<Usuarios>();
		tiempoDeSockets = new ArrayList<Integer>();
		listaDeSalas = new ArrayList<String>();
		listaDeSalas.add("Sala Principal");
		HibernateApp app = new HibernateApp();

		new Thread() {
			public void run() {
				while (true) {
					Socket s;
					ArrayList<Usuarios> lista = new ArrayList<Usuarios>();
					lista.addAll(listaDeSockets);
					try {

						for (Usuarios usuarios : lista) {

							s = new Socket(usuarios.getIp(), puertoParaControlDeSalas);
							DataOutputStream d;
							d = new DataOutputStream(s.getOutputStream());
							Gson gson = new Gson();
							String json = gson.toJson(listaDeSalas);
							d.writeUTF(json);
							s.close();

						}

					} catch (IOException | NullPointerException e) {
					}

				}
			}
		}.start();

		new Thread() {
			public void run() {

				Gson gson = new Gson();
				while (true) {
					try {

						ServerSocket ser = new ServerSocket(puertoParaIngresoDeUsuarios);
						Socket conex = ser.accept();
						DataInputStream in = new DataInputStream(conex.getInputStream());
						String json = in.readUTF();
						Pedido paq = gson.fromJson(json, Pedido.class);
						conex.close();
						boolean encontrado = false;

						if (paq.getPedido() == (Pedido.ingresar)) {

							conex = new Socket(paq.getIp(), puertoParaValidacionDeUsuarios);

							DataOutputStream o = new DataOutputStream(conex.getOutputStream());

							if (app.existeUsuario(paq.getUsuario())) {

								if (app.existeUsuarioyPass(paq.getUsuario(), paq.getPassword()))
									o.writeUTF("CONNECT");
								else
									o.writeUTF("NOTPASS");
							}

							else
								o.writeUTF("NOTFOUND");

							/*
							 * for (String user : usuariosRegistrados) { if (user.equals(paq.getUsuario()))
							 * {
							 * 
							 * if (passRegistrados.get(i).equals(paq.getPassword())) {
							 * o.writeUTF("CONNECT"); } else { o.writeUTF("NOTPASS"); } encontrado = true;
							 * break; } i++;
							 * 
							 * } if (!encontrado) { o.writeUTF("NOTFOUND"); }
							 */
						}

						else if (paq.getPedido() == Pedido.nuevo) {

							conex = new Socket(paq.getIp(), puertoParaValidacionDeUsuarios);

							DataOutputStream o = new DataOutputStream(conex.getOutputStream());

							// Usuario u= new Usuario(paq.getUsuario(),paq.getUsuario(),paq.getPassword());

							if (app.existeUsuario(paq.getUsuario())) {
								o.writeUTF("EXIST");

								encontrado = true;
								break;
							}

							/*
							 * for (String user : usuariosRegistrados) { if (user.equals(paq.getUsuario()))
							 * {
							 * 
							 * o.writeUTF("EXIST");
							 * 
							 * encontrado = true; break; } i++;
							 * 
							 * }
							 */

							if (!encontrado) {

								Usuario u = new Usuario();
								u.setNombreUsuario(paq.getUsuario());
								u.setPassword(paq.getPassword());
								u.setId(paq.getUsuario());

								app.nuevoUsuario(u);
								// UsuariosRegistrados.add(paq.getUsuario());
								// passRegistrados.add(paq.getPassword());
								o.writeUTF("ADD");
							}
						} else if (paq.getPedido() == Pedido.crearSala) {
							listaDeSalas.add(paq.getSala());

						}
						conex.close();
						ser.close();
					} catch (IOException e) {
					}

				}
			}
		}.start();

		new Thread() {

			public void run() {

				Gson gson = new Gson();
				while (true) {
					try {
						hilo1Work = false;
						ServerSocket ser = new ServerSocket(puertoParaConexionesActivas);
						Socket conex = ser.accept();
						DataInputStream in = new DataInputStream(conex.getInputStream());
						String json = in.readUTF();
						Usuarios paq = gson.fromJson(json, Usuarios.class);
						// String nick = im.readUTF();
						while (hilo2Work)
							Thread.sleep(200);
						hilo1Work = true;
						int cantElem = listaDeSockets.size();
						boolean find = false;
						for (int i = 0; i < cantElem; i++) {
							Usuarios sock = listaDeSockets.get(i);
							if (sock.getNick().equals(paq.getNick())) {
								find = true;
								tiempoDeSockets.set(i, 3);
								if (!sock.getSala().equals(paq.getSala()))
									listaDeSockets.get(i).setSala(paq.getSala());
								break;
							}

						}
						if (!find) {

							textArea.append("\nConectado Usuario: " + paq.getNick() + "---->" + paq.getIp() + "\n");

							listaDeSockets.add(paq);
							tiempoDeSockets.add(3);
							cb.addItem(paq.getNick());
						}

						ser.close();
					} catch (BindException e) {
						JOptionPane.showMessageDialog(null,
								"El punto de acceso se encuentra ocupado. Cierre todas las ventanas de esta aplicacion",
								"Error", JOptionPane.ERROR_MESSAGE);
						System.exit(ERROR);
					} catch (IOException | InterruptedException e2) {

						e2.printStackTrace();
					}

					// listaDeSockets.addAll(listaDeSockets2);
					// listaDeSockets2.clear();

				}
			}
		}.start();

		new Thread() {
			public void run() {
				while (true) {
					try {
						hilo2Work = false;
						Thread.sleep(2000);

						while (hilo1Work)
							Thread.sleep(300);
						hilo2Work = true;
						for (int i = 0; i < listaDeSockets.size(); i++) {
							int time = tiempoDeSockets.get(i);
							time--;
							tiempoDeSockets.set(i, time);
							if (time <= 0) {
								textArea.append("\nUsuario: " + listaDeSockets.get(i).getNick() + " desconectado\n");
								listaDeSockets.remove(i);
								tiempoDeSockets.remove(i);
								cb.removeItemAt(i);
							}

						}
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}

				}
			}
		}.start();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		cb = new JComboBox<String>();
		cb.setBounds(this.getWidth() - (this.getWidth() / 3), 30, 80, 20);
		contentPane.add(cb);

		textArea = new JTextArea();
		textArea.setBounds(10, 60, this.getWidth() - 40, this.getHeight() - 115);
		contentPane.add(textArea);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setBounds(10, 60, this.getWidth() - 40, this.getHeight() - 115);
		this.add(scroll);

		JFrame f = this;
		this.addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(ComponentEvent e) {

				// Se obtienen las dimensiones en pixels de la pantalla.
				// Se obtienen las dimensiones en pixels de la ventana.
				Dimension ventana = f.getSize();

				textArea.setSize(ventana.width - 40, ventana.height - 115);
				scroll.setSize(ventana.width - 40, ventana.height - 115);

			}

		});

		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}

	public void run() {

		try {
			server = new ServerSocket(puertoClienteAServidor);
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null,
					"El punto de acceso se encuentra ocupado. Cierre todas las ventanas de esta aplicacion", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		} // Intenta crear el puente del lado del servidor

		textArea.setText("Esperando Conexion...\n");

		while (true) {
			// socket = new Socket();
			try {

				socket = server.accept(); // Espera a que alguien se quiera conectar y lo guarda

				entrada = new DataInputStream(socket.getInputStream());

				Gson gson = new Gson();
				String json = entrada.readUTF();
				Usuarios paquete = gson.fromJson(json, Usuarios.class);

				// ArrayList<PaqueteEnvio> users = listaDeSockets;

				ArrayList<Usuarios> users = new ArrayList<Usuarios>();
				users.addAll(listaDeSockets);

				Socket s;
				int cant = users.size();
				for (int i = 0; i < cant; i++) {
					Usuarios usuario = users.get(i);
					if ((!paquete.getNick().equals(usuario.getNick()) && paquete.getSala().equals(usuario.getSala()))
							|| (paquete.getNotificarA() != null && paquete.getNotificarA().equals(usuario.getNick()))) {

						try {
							s = new Socket(usuario.getIp(), puertoServidorACliente);
							DataOutputStream o = new DataOutputStream(s.getOutputStream());
							json = gson.toJson(paquete);
							o.writeUTF(json);

							s.close();
						} catch (IOException e1) {
						}
					}
				}

				textArea.append(paquete.getNick() + "---->" + paquete.getMensaje() + "--" + paquete.getIp() + "\n");

			} catch (IOException e) {
				e.printStackTrace();
			} finally {

				try {

					entrada.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
