package nl.soutenet.vertx.mod.deploy;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class VertxDeployModTest {

  //  104857600
    @Test
    public void testSendData() throws IOException {
        Long control1size = 17787356234l;
        Long control2size = 123187261l;

        String sentence1 = "{test:"+ String.format("%10d", control1size)+"}";


        System.out.println(sentence1);


        String modifiedSentence;
        Socket clientSocket = new Socket("localhost", 5678);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        while(true) {
            outToServer.writeBytes(sentence1);
        }

/*        modifiedSentence = inFromServer.readLine();
        System.out.println("FROM SERVER: " + modifiedSentence);
        clientSocket.close();*/
    }
}
