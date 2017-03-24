package kojonek2.tictactoeserver.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

	public static void main(String[] args) {

		try (
			ServerSocket serverSocket = new ServerSocket(4554);
			Socket clientSocket = serverSocket.accept();
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
		) {
			String inputLine;
			out.println("hello");
			while((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
			}
			
		} catch (Exception e) {
			System.err.println("Error during creating socket or listening for conection");
			e.printStackTrace();
		}

	}

}
