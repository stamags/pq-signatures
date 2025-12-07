package beans;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/image/*")  // Servlet mapping
public class ImageServlet extends HttpServlet {
    private static final String IMAGE_DIRECTORY = "C:/Users/PX/IdeaProjects/parking/src/main/webapp/resources/assets_aerial/css/qr";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get the image filename from the request URL
        String filename = request.getPathInfo();
        if (filename == null || filename.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing image filename.");
            return;
        }

        // Remove leading slash
        filename = filename.substring(1);

        // Construct the full path
        File file = new File(IMAGE_DIRECTORY, filename);

        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            return;
        }

        // Set content type based on file extension
        response.setContentType(getServletContext().getMimeType(file.getName()));

        // Send the file to the response
        Files.copy(file.toPath(), response.getOutputStream());
    }
}
