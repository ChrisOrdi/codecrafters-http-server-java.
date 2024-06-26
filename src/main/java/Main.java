import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);

      while (true) {
        try (Socket clientSocket = serverSocket.accept()) {
          System.out.println("Accepted new connection");

          BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          String requestLine = in.readLine();
          System.out.println("Request Line: " + requestLine);

          if (requestLine == null) {
            continue;
          }

          // Read headers
          Map<String, String> headers = new HashMap<>();
          String headerLine;
          while (!(headerLine = in.readLine()).isEmpty()) {
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
              headers.put(headerParts[0].toLowerCase(), headerParts[1]);
            }
          }

          String[] requestParts = requestLine.split(" ");
          String method = requestParts[0];
          String urlPath = requestParts[1];

          String httpResponse;
          if (method.equals("GET")) {
            if (urlPath.equals("/")) {
              httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
            } else if (urlPath.startsWith("/echo/")) {
              String message = urlPath.substring(6);
              String responseBody = message;
              String contentType = "text/plain";
              int contentLength = responseBody.length();

              httpResponse = "HTTP/1.1 200 OK\r\n" +
                      "Content-Type: " + contentType + "\r\n" +
                      "Content-Length: " + contentLength + "\r\n" +
                      "\r\n" +
                      responseBody;
            } else if (urlPath.equals("/user-agent")) {
              String userAgent = headers.get("user-agent");
              if (userAgent == null) {
                userAgent = "";
              }
              String responseBody = userAgent;
              String contentType = "text/plain";
              int contentLength = responseBody.length();

              httpResponse = "HTTP/1.1 200 OK\r\n" +
                      "Content-Type: " + contentType + "\r\n" +
                      "Content-Length: " + contentLength + "\r\n" +
                      "\r\n" +
                      responseBody;
            } else {
              httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
          } else {
            httpResponse = "HTTP/1.1 405 Method Not Allowed\r\n\r\n";
          }

          OutputStream clientOutput = clientSocket.getOutputStream();
          clientOutput.write(httpResponse.getBytes("UTF-8"));
          clientOutput.flush();
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
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
