package sockets;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ServidorFrame s= new ServidorFrame();
		s.setVisible(true);
		s.setLocationRelativeTo(null);
		new Thread(s).start();
	}

}
