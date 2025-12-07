package rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@WebServlet("/reports/*")
public class ImageServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filename = request.getPathInfo().substring(1);
        File file = new File(System.getProperty("jboss.server.temp.dir"), filename);

//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//        response.setHeader("Content-Disposition", "attachment; filename=" + YOUR_FILE_NAME);
        response.setHeader("Content-Type", getServletContext().getMimeType(filename));
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Disposition", "inline; filename="+File.separator + filename + File.separator );
        Files.copy(file.toPath(), response.getOutputStream());
    }


//
//    String filename = request.getPathInfo().substring(1);
//    File file = new File(System.getProperty("jboss.server.temp.dir"), filename);
//
////        HttpHeaders headers = new HttpHeaders();
////        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
////        response.setHeader("Content-Disposition", "attachment; filename=" + YOUR_FILE_NAME);
//        response.setHeader("Content-Type", getServletContext().getMimeType(filename));
////        response.setHeader("Content-Type", "application/octet-stream");
////        response.setHeader("Content-Length", String.valueOf(file.length()));
//        response.setHeader("Content-Disposition", "inline;filename="+"test.png");
////        response.setHeader("Content-Length", String.valueOf(file.length()));
////        response.setContentType(MediaType.APPLICATION_OCTET_STREAM);
////        response.setHeader("Content-Type", "application/octet-stream");
//
//        response.setHeader("Cache-Control", "no-cache");
//        response.setHeader("Pragma", "no-cache");








}

