package SKJ_3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

enum ExecutableCommand {
	configuration,
	receivingData,
	disconnecting
}

public class Relay extends Thread {
	
	public static void main(String args[]) throws Exception {
		final int relayPort = 12345;
		Relay relay = new Relay(relayPort);
		relay.start();
        
    }
	
	public int port = 0;
	public String remoteHost = "";
	private BufferedReader in = null;
	private PrintWriter out = null;
	private ExecutableCommand currentCommand = ExecutableCommand.configuration;
	private ServerSocket server = null;
	private DatagramSocket udpServer;
	private Socket client = null;
	
	Relay(int port) {
		this.port = port;
		
		try {
			server = new ServerSocket(port); 
		}
		catch (IOException e) {
			disconnected();
			System.out.println(e);
		}
		
		try {
			udpServer = new DatagramSocket(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
        startTCPwithAgent();
	}

	public void tcpConnectionConfigurated(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public void receivedData(int port, String message) {
		configureWith(port, message, remoteHost);
		System.out.println("Message: " + message);
	}

	public void disconnected() {
		System.out.println("Disconnected");
	}

	public void receivedAnswer(String response, int port) {
		System.out.println("Response: " + response);
		sendResponse(response, port);
	}
		
	public void sendResponse(String data, int port) {
		sendDataToAgent(data, port);
	}
	
	public void listenSocket() {
		while(true) {
			try {
				client = server.accept();
			}
			catch (IOException e) {
				System.out.println("Accept failed");
				System.exit(-1);
			}
			
			startTCPmessaging();
		}
	}

	public void startTCPwithAgent() {
		listenSocket();
	}

	public void startTCPmessaging() {
		try {
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out = new PrintWriter(client.getOutputStream(), true);
						
			String line;
			while((line = in.readLine()) != null && !line.isEmpty())
			{
				if (line.equals("DISCONNECT")) {
					currentCommand = ExecutableCommand.disconnecting;
				}
				
				switch (currentCommand) {
				case configuration:
					currentCommand = ExecutableCommand.receivingData;
					tcpConnectionConfigurated(line);
					break;
				case receivingData:
					String [] segments = line.split(";");
					String port = segments[0];
					String value = segments[1];
					receivedData(Integer.parseInt(port), value);
					break;
				case disconnecting:
					currentCommand = ExecutableCommand.configuration;
					out.close();
			        in.close();
			        disconnected();
					break;
				}				
			}
		} catch (IOException e1) {
			disconnected();
		}
		
		try {
			client.close();
		} catch (IOException e) {
			disconnected();
		}
	}
	
	public void sendDataToAgent(String data, Integer port) {
		System.out.println("Sent to agent");
		out.println(data + ";" + String.valueOf(port));
	}
	
	public void configureWith(int port, String message, String remoteHost) {
		try {
			InetAddress address = InetAddress.getByName(remoteHost);
					
			String concatenated = String.valueOf(port) + " " + message;
			byte[] queryBuff = concatenated.getBytes();
			DatagramPacket query = new DatagramPacket(queryBuff, queryBuff.length, address, port);
			
			udpServer.send(query);
						
			byte[] buff = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buff, buff.length);
			
			udpServer.receive(packet);
			
			String answer = new String(packet.getData(), 0, packet.getLength()).trim();
			
			receivedAnswer(answer, packet.getPort());
			
			udpServer.close();
		}  catch (IOException e) {
			e.printStackTrace();
		}
	}
}
