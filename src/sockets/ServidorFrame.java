package sockets;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import dijkstra_paqueteEnvio.PaqueteEnvio;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextArea;

public class ServidorFrame extends JFrame implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JPanel contentPane;
	
	ObjectInputStream entrada;
	ServerSocket server; // Prepara un puente para que se conecten otros sockets
	Socket socket; // Prepara un puente para conectarse a un serverSocket
	int puerto = 9998; // El puerto por el que acceden los datos
	int puerto2 = 9996;
	int puertoParaConexionesActivas = 9990;
	DataOutputStream salida;// Flujo de datos para conectar con el puente de salida y enviar el mensaje
	ArrayList<PaqueteEnvio> listaDeSockets;
	ArrayList<Integer> tiempoDeSockets;
	JComboBox<String> cb;
	private JTextArea textArea;

	boolean hilo1Work = false;

	boolean hilo2Work = false;

	public void setTextArea(String text) {
		this.textArea.setText(text);
	}

	public ServidorFrame() {
		listaDeSockets = new ArrayList<PaqueteEnvio>();
		tiempoDeSockets = new ArrayList<Integer>();

		new Thread() {

			public void run() {

				while (true) {
					try {
						hilo1Work = false;
						ServerSocket ser = new ServerSocket(puertoParaConexionesActivas);
						Socket conex = ser.accept();
						ObjectInputStream im = new ObjectInputStream(conex.getInputStream());
						
						PaqueteEnvio paq=(PaqueteEnvio) im.readObject();
						//String nick = im.readUTF();
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
							}

						}
						if (!find) {
							
							textArea.append("\nConectado usuario: " + paq.getNick() + "\n");
							
							listaDeSockets.add(paq);
							tiempoDeSockets.add(3);
							cb.addItem(paq.getNick());
						}

						ser.close();
					} catch (IOException | InterruptedException | ClassNotFoundException e2) {
						// TODO Auto-generated catch block
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
						// TODO Auto-generated catch block
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
		textArea.setBounds(10, 60, 424, 250);
		contentPane.add(textArea);

		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}

	public void run() {

		try {
			server = new ServerSocket(puerto); // Intenta crear el puente del lado del servidor

			textArea.setText("Esperando Conexion...\n");

			while (true) {
				// socket = new Socket();

				socket = server.accept(); // Espera a que alguien se quiera conectar y lo guarda
				//String direccion = socket.getInetAddress().toString().replace("/", "");
				//textArea.append("Conectado usuario: " + direccion + "\n");
				

			

				entrada = new ObjectInputStream(socket.getInputStream());
			
				PaqueteEnvio paquete= (PaqueteEnvio)entrada.readObject();

				ArrayList<PaqueteEnvio>users=listaDeSockets;
				
				for (PaqueteEnvio usuario : users) {
					Socket s= new Socket(usuario.getIp(),puerto2);
					ObjectOutputStream o= new ObjectOutputStream(s.getOutputStream());
					o.writeObject(paquete);
					s.close();
				}
				
				
				textArea.append(paquete.getNick() + "---->" + paquete.getMensaje() + "--" + paquete.getIp()+"\n");

				/*
				 * DataInputStream recibe = new DataInputStream(socket.getInputStream());
				 * String[] mensajeCompuesto = recibe.readUTF().split("&"); String ips =
				 * mensajeCompuesto[0]; String mensaj = mensajeCompuesto[1];
				 * 
				 * 1) GetImputStream obtiene el flujo de datos de entrada, que es por donde
				 * viene el mensaje y lo crea como un nuevo ImputStreamReader 2) BufferReader
				 * obtiene el mensaje que viaja por el imputStreamReader y lo guarda en entrada
				 */

				/*
				 * try { socket = new Socket(ips, puerto2);// Intenta creal un puente con la ip
				 * a la que se desea // enviar el mensaje } catch (Exception e) {
				 * System.out.println("IP no valida"); // Si no lo logra Imprime que la ip no es
				 * valida (lo imprime // solo en la consola asi que es para probar solamente) }
				 * 
				 * salida = new DataOutputStream(socket.getOutputStream()); // Crea un flujo de
				 * salida conectada al // puente con el cliente al que se va a // enviar el
				 * mensaje salida.writeUTF(mensaj);// Se envia el mensaje
				 */
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {

				// No estoy muy seguro si esto esta bien. En el bloque finally(asi salte o no
				// una excepcion), se cierran todos los sockets y los flujos de datos
				// salida.close();
				entrada.close();
				server.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
