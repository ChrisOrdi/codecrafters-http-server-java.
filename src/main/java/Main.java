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

      while (true) {
        clientSocket = serverSocket.accept();
        System.out.println("Accepted new connection");

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String requestLine = in.readLine();
        System.out.println("Request Line: " + requestLine);

        if (requestLine == null) {
          continue;
        }

        String[] requestParts = requestLine.split(" ");
        String urlPath = requestParts[1];

        String httpResponse;
        if (urlPath.startsWith("/echo/")) {
          String echoString = urlPath.substring(6);
          String responseBody = echoString;
          String contentType = "text/plain";
          int contentLength = responseBody.length();

          httpResponse = "HTTP/1.1 200 OK\r\n" +
                  "Content-Type: " + contentType + "\r\n" +
                  "Content-Length: " + contentLength + "\r\n" +
                  "\r\n" +
                  responseBody;
        } else if ("/".equals(urlPath)) {
          httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
        } else {
          httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }

        OutputStream clientOutput = clientSocket.getOutputStream();
        clientOutput.write(httpResponse.getBytes("UTF-8"));
        clientOutput.flush();
        clientSocket.close();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (serverSocket != null) {
          serverSocket.close();
        }
      } catch (IOException e) {
        System.out.println("IOException during close: " + e.getMessage());
      }
    }
  }
}
