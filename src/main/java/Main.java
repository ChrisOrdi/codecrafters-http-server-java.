import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept();
      System.out.println("Accepted new connection");

      // Read the request line
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String requestLine = in.readLine();
      System.out.println("Request Line: " + requestLine);

      // Extract the URL path
      String[] requestParts = requestLine.split(" ");
      String urlPath = requestParts[1];

      // Prepare the HTTP response
      String httpResponse;
      if ("/".equals(urlPath)) {
        httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
      } else {
        httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
      }

      // Send the HTTP response to the client
      OutputStream clientOutput = clientSocket.getOutputStream();
      clientOutput.write(httpResponse.getBytes("UTF-8"));
      clientOutput.flush();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (clientSocket != null) {
          clientSocket.close();
        }
        if (serverSocket != null) {
          serverSocket.close();
        }
      } catch (IOException e) {
        System.out.println("IOException during close: " + e.getMessage());
      }
    }
  }
}
