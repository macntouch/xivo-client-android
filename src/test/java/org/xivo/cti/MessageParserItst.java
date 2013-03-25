package org.xivo.cti;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.xivo.cti.message.CtiMessage;
import org.xivo.cti.message.LoginAck;
import org.xivo.cti.message.LoginPassAck;

public class MessageParserItst {
    public static final int XIVO_DEFAULT_PORT = 5003;
    
    private Thread thread = null;
    private String host = "192.168.56.101";
    private int port = 5003;
    private Socket networkConnection = null;
    private BufferedReader inputBuffer = null;
    private MessageParser messageParser;
    private MessageFactory messageFactory;


	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory();
		messageParser = new MessageParser();
	}

	@Test
	public void test_login_sequence() throws IOException, JSONException, TimeoutException {
		connectToServer();
		
		sendLogin("marice");
		LoginAck loginAck = (LoginAck) waitForLoginAck();
		assertNotNull("unable to get login acknowledge",loginAck);
		
		String sessionId = loginAck.sesssionId;
		
		sendLoginPass("sapr",sessionId);

		disconnect();
	}

	private void sendLogin(String username) throws IOException {
		
		JSONObject jsonObject = messageFactory.createLoginId(username);
		PrintStream output = new PrintStream(networkConnection.getOutputStream());
		System.out.println(jsonObject.toString());
		output.println(jsonObject.toString());
	}
	
	private void sendLoginPass(String password, String sessionId) {
		JSONObject jsonObject = messageFactory.createLoginPass(password,sessionId);
		
	}
	
	private CtiMessage waitForLoginAck() throws IOException, JSONException{
		int i = 0;
		boolean found = false;
		while(!found) {
			String line = inputBuffer.readLine();
			System.out.println(i+line);
			if (line == null) {
				return null;
			}
			CtiMessage ctiMessage = messageParser.parse(new JSONObject(line));
			if (ctiMessage.getClass() == LoginAck.class) {
				return ctiMessage;
			}
			if (i > 100) {
				return null;
			}
			i++;
		}
		return null;
	}
	
    private void connectToServer() {
        stopThread();
        while (thread != null && thread.isAlive()) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                System.out.println("Wait interrupted");
            }
        }
        try {
        	System.out.println("Connecting to " + host + " " + port);
            networkConnection = new Socket(host, port);
        } catch (UnknownHostException e) {
        	System.out.println("Unknown host " + host);
        } catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("connected .....");
        try {
            inputBuffer = new BufferedReader(
                    new InputStreamReader(networkConnection.getInputStream()));
            
        } catch (IOException e) {
			e.printStackTrace();
        }
    }
    

    private void disconnect() {
    	System.out.println("disconnecting");
    	stopThread();
        if (inputBuffer != null) {
            BufferedReader tmp = inputBuffer;
            inputBuffer = null;
        }
        if (networkConnection != null) {
            try {
                try {
                    networkConnection.shutdownOutput();
                    networkConnection.shutdownInput();
                } catch (IOException e) {
                	System.out.println("Input and output were already closed");
                }
                if (networkConnection != null) {
                    Socket tmp = networkConnection;
                    tmp.close();
                    networkConnection = null;
                }
            } catch (IOException e) {
            	System.out.println("Error while cleaning up the network connection");
                e.printStackTrace();
            }
        }
    }

    
    private void stopThread() {
        if (thread != null) {
            Thread dead = thread;
            thread = null;
            dead.interrupt();
        }
    	
    }
}
