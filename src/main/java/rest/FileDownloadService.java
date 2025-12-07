package rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.io.File;

@Path("/files")
public class FileDownloadService {
 	private static final String FILE_PATH = "/home/tsotzolas/Documents/scan0008.pdf";
 
	@GET
	@Path("/pdf")
	@Produces("application/pdf")
	public Response getFile() {
 
		File file = new File(FILE_PATH);
 
		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition","attachment; filename=\"javatpoint_pdf.pdf\"");
		return response.build();
 
	}
 }