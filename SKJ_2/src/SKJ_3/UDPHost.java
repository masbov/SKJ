package SKJ_3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPHost extends Thread {
	
	public Agent agent = null;
	public DatagramSocket socket = null;
	
	public UDPHost(DatagramSocket socket, Agent agent) {
		this.agent = agent;
		this.socket = socket;
	}

	@Override
	public void run() {
    	byte[] buffer = new byte[1024];
    	
    	while(true) {
    	try {           
        	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        	socket.receive(packet);

        	String inputLine = new String(buffer);

        	if (inputLine != "DISCONNECT") {
        		agent.listenerGetData(inputLine, packet.getPort());
        	} else {
            	agent.disconnect();
        	}
    	} catch (IOException e) {
    		agent.disconnect();
        	System.out.println(e);
    	}
    	}
	}
	
	public void sendAnswer(String answer, Integer port) {
    	byte[] buffer = new byte[1024];

    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		DatagramPacket reply = new DatagramPacket(answer.getBytes(), answer.getBytes().length, packet.getAddress(), port);
		try {
			socket.send(reply);
		} catch (IOException e) {
			agent.disconnect();
			System.out.println(e);
		}
	}
}
