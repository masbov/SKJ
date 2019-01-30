
package SKJ_3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Agent extends Thread {
	
	public static void main(String args[]) throws Exception {		
		String redirectHost = "insert here";
		String relayHost = "insert here";
		int relayPort = 12345;
		int[] listenPorts = {2567, 6547, 1764};

		new Agent(redirectHost, relayHost, relayPort, listenPorts).start();
    }
	
	private List<UDPHost> udpChannels = new ArrayList<>();
	
	private String redirectHost = "";
	private String relayHost = "";
	private int relayPort = 0;
	private int[] listenPorts = {};
	private PrintWriter out = null;

	Agent(String redirectHost, String relayHost, int relayPort, int[] listenPorts) {
		this.redirectHost = redirectHost;
		this.relayHost = relayHost;
		this.relayPort = relayPort;
		this.listenPorts = listenPorts;
	}

	public void run() {
        communicateTCPwithRelay();
	}

	public void relayConfigured() {
		try {
			for (int i = 0; i < listenPorts.length; i++) {
				int port = listenPorts[i];
				SocketAddress localport = new InetSocketAddress(port);
				DatagramChannel udpserver = DatagramChannel.open();
				udpserver.socket().bind(localport);
				
				UDPHost listener = new UDPHost(udpserver.socket(), this);
				udpChannels.add(listener);
				listener.start();
				listener.sendAnswer("OK!!!", port);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listenerGetData(String data, Integer port) {
		sendData(data, port);
	}

	public void disconnect() {
		disconnectTCPwithRelay();
	}
	
	public void relayAnswered(String answer, Integer port) {
		for(int i = 0; i < udpChannels.size(); i++) {
			UDPHost thread = udpChannels.get(i);
			if (thread.socket.getPort() == port) {
				thread.sendAnswer(answer, port);
			}
		}
	}
	
	public void communicateTCPwithRelay() {
		Socket socket = null;
		BufferedReader in = null;
				
		try {
			socket = new Socket(relayHost, relayPort);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}
		catch (UnknownHostException e) {
			System.out.println(e.toString());
		}
		catch (IOException exception) {
			System.out.println(exception);
		}
		try {
			out.println(redirectHost);
			relayConfigured();
			
			String line;

			while((line = in.readLine()) != null)
			{
				String [] segments = line.split(";");
				String port = segments[0];
				String value = segments[1];
				relayAnswered(value, Integer.valueOf(port));
			}
	    }
	    catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
		try {
			socket.close();
		}
		catch (IOException e) {
			disconnect();
			System.out.println("Cannot close the socket");
		}
	}
	
	public void sendData(String data, Integer port) {
		out.println(port + ";" + data);
	}
	
	public void disconnectTCPwithRelay() {
		out.println("DISCONNECT");
	}
}
