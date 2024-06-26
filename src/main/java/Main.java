import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  public static void main(String[] args) {
    String directory = null;
    if ((args.length == 2) && (args[0].equalsIgnoreCase("--directory"))) {
      directory = args[1];
    }

    if (directory == null) {
      // Set default directory
      directory = "/default/directory/path";
    }

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        final String finalDirectory = directory;
        new Thread(() -> {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
               OutputStream outputStream = clientSocket.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
              return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
              return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            String userAgent = "";
            String acceptEncoding = "";
            int contentLength = 0;
            String line;

            while (!(line = reader.readLine()).equals("")) {
              if (line.startsWith("User-Agent: ")) {
                userAgent = line.substring(12);
              } else if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring(16).trim());
              } else if (line.startsWith("Accept-Encoding: ")) {
                acceptEncoding = line.substring(17).trim();
              }
            }

            if (method.equals("GET")) {
              handleGetRequest(path, finalDirectory, outputStream, userAgent, acceptEncoding);
            } else if (method.equals("POST")) {
              handlePostRequest(path, finalDirectory, reader, outputStream, contentLength);
            }
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          } finally {
            try {
              clientSocket.close();
            } catch (IOException e) {
              System.out.println("IOException: " + e.getMessage());
            }
          }
        }).start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static void handleGetRequest(String path, String finalDirectory, OutputStream outputStream, String userAgent, String acceptEncoding) throws IOException {
    if (path.startsWith("/files/")) {
      String fileName = path.substring(7);
      Path filePath = Paths.get(finalDirectory, fileName);
      if (Files.exists(filePath)) {
        byte[] fileBytes = Files.readAllBytes(filePath);
        String response =
                "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                        fileBytes.length + "\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.write(fileBytes);
      } else {
        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }
    } else if (path.startsWith("/user-agent")) {
      String response =
              "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                      userAgent.length() + "\r\n\r\n" + userAgent;
      outputStream.write(response.getBytes());
    } else if (path.startsWith("/echo/")) {
      String randomString = path.substring(6);
      String response;
      if (acceptEncoding.contains("gzip")) {
        response =
                "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Encoding: gzip\r\nContent-Length: " +
                        randomString.length() + "\r\n\r\n" + randomString; // Here, you would normally compress the content
      } else {
        response =
                "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                        randomString.length() + "\r\n\r\n" + randomString;
      }
      outputStream.write(response.getBytes());
    } else if (path.equals("/")) {
      outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
    } else {
      outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
    }
    outputStream.flush();
  }

  private static void handlePostRequest(String path, String finalDirectory, BufferedReader reader, OutputStream outputStream, int contentLength) throws IOException {
    if (path.startsWith("/files/")) {
      String fileName = path.substring(7);
      Path filePath = Paths.get(finalDirectory, fileName);

      char[] content = new char[contentLength];
      reader.read(content, 0, contentLength);

      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
        bos.write(new String(content).getBytes());
      }

      String response = "HTTP/1.1 201 Created\r\n\r\n";
      outputStream.write(response.getBytes());
      outputStream.flush();
    } else {
      outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      outputStream.flush();
    }
  }
}
