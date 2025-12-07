package rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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