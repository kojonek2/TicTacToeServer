package kojonek2.tictactoeserver.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection implements Runnable {

	private Socket clientSocket;
	private ServerMain mainServer;

	public ClientConnection(Socket socket, ServerMain mainServer) {
		clientSocket = socket;
		this.mainServer = mainServer;
	}

	@Override
	public void run() {
		try (
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
		) {
			String inputLine;
			out.println("Connected");
			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
			}

		} catch (Exception e) {
			System.err.println("Error during connection with client");
			e.printStackTrace();
		} finally {
			mainServer.connections.remove(Thread.currentThread());
			System.out.println("active connections:" + mainServer.connections.size());
		}
	}

}
