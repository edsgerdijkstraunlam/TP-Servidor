package sockets;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import dijkstra_paqueteEnvio.PaqueteEnvio;

import javax.swing.JComboBox;
import javax.swing.JFrame;
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
//	private ObjectInputStream entrada;
	private ServerSocket server; // Prepara un puente para que se conecten otros sockets
	private Socket socket; // Prepara un puente para conectarse a un serverSocket
	private int puertoClienteAServidor = 9998; // El puerto por el que acceden los datos
	private int puertoServidorACliente = 9996;
	private int puertoParaConexionesActivas = 9994;
	private ArrayList<PaqueteEnvio> listaDeSockets;
	private ArrayList<Integer> tiempoDeSockets;
	private JComboBox<String> cb;
	private JTextArea textArea;

	private boolean hilo1Work = false;

	private boolean hilo2Work = false;

	public ServidorFrame() {
		listaDeSockets = new ArrayList<PaqueteEnvio>();
		tiempoDeSockets = new ArrayList<Integer>();

		new Thread() {

			public void run() {

				Gson gson= new Gson();
				while (true) {
					try {
						hilo1Work = false;
						ServerSocket ser = new ServerSocket(puertoParaConexionesActivas);
						Socket conex = ser.accept();
						DataInputStream in = new DataInputStream(conex.getInputStream());
						String json= in.readUTF();
						PaqueteEnvio paq = gson.fromJson(json, PaqueteEnvio.class);
						// String nick = im.readUTF();
						while (hilo2Work)
							Thread.sleep(200);
						hilo1Work = true;
						int cantElem = listaDeSockets.size();
						boolean find = false;
						for (int i = 0; i < cantElem; i++) {
							PaqueteEnvio sock = listaDeSockets.get(i);
							if (sock.getNick().equals(paq.getNick())) {
								find = true;
								tiempoDeSockets.set(i, 3);
								break;
							}

						}
						if (!find) {

							textArea.append("\nConectado usuario: " + paq.getNick() + "---->" + paq.getIp() + "\n");

							listaDeSockets.add(paq);
							tiempoDeSockets.add(3);
							cb.addItem(paq.getNick());
						}

						ser.close();
					} catch (IOException | InterruptedException  e2) {
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
			e1.printStackTrace();
		} // Intenta crear el puente del lado del servidor

		textArea.setText("Esperando Conexion...\n");

		while (true) {
			// socket = new Socket();
			try {

				socket = server.accept(); // Espera a que alguien se quiera conectar y lo guarda
				// String direccion = socket.getInetAddress().toString().replace("/", "");
				// textArea.append("Conectado usuario: " + direccion + "\n");

				entrada = new DataInputStream(socket.getInputStream());

				Gson gson=new Gson();
				String json=entrada.readUTF();
				PaqueteEnvio paquete = gson.fromJson(json, PaqueteEnvio.class);

			//	ArrayList<PaqueteEnvio> users = listaDeSockets;

				ArrayList<PaqueteEnvio> users = new ArrayList<PaqueteEnvio>();
				users.addAll(listaDeSockets);
				
				
				Socket s;
				int cant= users.size();
				for (int i=0;i <cant;i++) {
					PaqueteEnvio usuario= users.get(i);
					if (!paquete.getNick().equals(usuario.getNick())) {
						
						s = new Socket(usuario.getIp(), puertoServidorACliente);
						DataOutputStream o = new DataOutputStream(s.getOutputStream());
						json=gson.toJson(paquete);
						o.writeUTF(json);
						s.close();
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
