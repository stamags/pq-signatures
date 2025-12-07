package rest;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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

