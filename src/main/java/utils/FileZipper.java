/*
 *Δημιουργεί zip αρχεία
 */

package utils;

import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.primefaces.event.FileUploadEvent;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class FileZipper {

    private List<String> fileList;
    private String outputZipFile;// "C:\\jboss-as-7.1.1.Final_8888\\standalone\\deployments\\resources.war\\localjaspers";
    private String sourceFolder;// = "C:\\jboss-as-7.1.1.Final_8888\\standalone\\deployments\\resources.war\\localjaspers"; // SourceFolder path
    private String foldername;

    //in the constructor is setted the path of the output zip file, the path of the folder to be zipped and the name of the folder
    public FileZipper(String outputZipFile, String outputSourceFolder, String foldername) {
        fileList = new ArrayList<String>();
        this.outputZipFile = outputZipFile;
        this.sourceFolder = outputSourceFolder;
        this.foldername = foldername;
    }
    //

    public FileZipper() {
        fileList = new ArrayList<String>();
    }

    //main method is just for checking
    public static void main(String[] args) {
        Date now = new Date();

        //το μονοπάτι τους αρχείου στον κοινόχρηστο
        FileZipper appZip = new FileZipper("1396595694331", "1396595694331", "1396595694331");
        appZip.generateFileList(new File("" + "1396595694331"));
//        FileZipper appZip = new FileZipper("\\\\adobe_srv\\shared_folders\\esep\\metafora\\localjaspers\\" + "1396595694331", "\\\\adobe_srv\\shared_folders\\esep\\metafora\\localjaspers", "1396595694331");
//            appZip.generateFileList(new File("\\\\adobe_srv\\shared_folders\\esep\\metafora\\localjaspers\\" + "1396595694331"));
        StringBuilder sbb = new StringBuilder();
        sbb.append("lalalalalla").append(" wxwxwx");


        try {
//            String zipFilePath = appZip.zipIt();
            File zipFilePath = appZip.zipItWithCode(sbb, "", "");
//            System.out.println(zipFilePath);
//            System.out.println("current Path :" + System.getProperty("user.dir"));
        } catch (IOException ex) {
            Logger.getLogger(FileZipper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //this method get a folder  and make a zip of it in it
    public String zipIt() throws IOException {
        byte[] buffer = new byte[1024];
        String source = "";
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        FileInputStream in = null;
        String zipFilePath = outputZipFile + "\\" + foldername + ".zip";
        source = "\\";//this is the root folder in the tree of zipfile.For example with / after unzip all the files will be in the root folder, with f1/f2/f3 as values all files will be in the subfolder f3

        fos = new FileOutputStream(zipFilePath);
        zos = new ZipOutputStream(fos);

        for (String file : this.fileList) {
            ZipEntry ze = new ZipEntry(file);
            try {
                zos.putNextEntry(ze);//χσδφσδφ zip entries into zip outputstream
            } catch (IOException ex) {
                Logger.getLogger(FileZipper.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                in = new FileInputStream(sourceFolder + file);//copy zipoutputstream to a fileInputstream ans create the zipfile

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileZipper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FileZipper.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                in.close();
            }
        }

        zos.closeEntry();
        zos.close();
        fos.close();

        return zipFilePath;
    }

    // fix the inner file name!
    public File zipItWithCode(StringBuilder sbb, String password, String innerName) throws IOException {
        byte[] buffer = new byte[1024];
        String source = "";

        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        FileInputStream in = null;
        String zipFilePath = outputZipFile + "\\" + foldername + ".zip";
        final int BUFFER = 1024;
//        final String destinationPath = "c:\\Users\\kepyes_4a_21\\Desktop\\myfigs.zip";
        final String destinationPath = "myfigs.zip";
        try {
            File oldfile = new File("myfigs.zip");
            oldfile.delete();

            net.lingala.zip4j.ZipFile zipFile = new net.lingala.zip4j.ZipFile(destinationPath);

//            BufferedInputStream origin = null;
//            FileOutputStream dest = new FileOutputStream(destinationPath);
//            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
//            out.setMethod(ZipOutputStream.DEFLATED);
            byte data[] = new byte[BUFFER];
            File f = new File(innerName + ".txt");
//            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
            bw.write(sbb.toString());
            bw.newLine();
            bw.close();
            String files[] = new String[1];
//          System.out.println("f.getAbsolutePath() = " + f.getAbsolutePath());
            files[0] = f.getName();
            //---------------------
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            if (!password.equals("")) {
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(EncryptionMethod.AES);
                parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

            }
//            System.out.println("Adding: " + files[0]);
            ArrayList<File> filesToAdd = new ArrayList<File>();
            filesToAdd.add(f);
            zipFile.addFiles(filesToAdd, parameters);


            //---------------------------
            f.delete();
        } catch (Exception e) {
//            System.out.println(e.getMessage() + "marios message");
        }
        return new File(destinationPath);
    }


    public static void zipFilesWithCode(Map<String, String> dataMap, String password, OutputStream outputStream) throws IOException {


        ZipParameters zipParams = new ZipParameters();
        zipParams.setCompressionMethod(CompressionMethod.DEFLATE);
        zipParams.setCompressionLevel(CompressionLevel.NORMAL);
        zipParams.setEncryptFiles(true);
        zipParams.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        net.lingala.zip4j.io.outputstream.ZipOutputStream zos = new net.lingala.zip4j.io.outputstream.ZipOutputStream(outputStream, password.toCharArray());

        dataMap.forEach((key, value) -> {

            try {
                zipParams.setFileNameInZip(key);
                zos.putNextEntry(zipParams);

                OutputStreamWriter osw = new OutputStreamWriter(zos, "UTF-8");
                osw.write(value);
                osw.flush();
                zos.closeEntry();

            } catch (IOException e) {
                e.printStackTrace();
            }


        });

        zos.close();

//        zos.close();
    }


    public void generateFileList(File node) {

        // add file only
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString()));
        }
        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }
    }

    //this method take the file name of every file to be included in the final zip file
    private String generateZipEntry(String file) {
        return file.substring(this.sourceFolder.length(), file.length());
    }

    //this method return the path of the zipfile
    public String getZipFile() {
        return this.outputZipFile;
    }


//    public static Map<String, String> unlockZipWithCode(org.primefaces.model.UploadedFile uploadedFile, String password) {
//        //KEY: Filename - VALUE: OutputStream
//        Map<String, OutputStream> inMemoryExtractedFiles = new HashMap<>();
//
//        //KEY: Filename - VALUE: OutputStream
//        Map<String, String> inMemoryFiles = new HashMap<>();
//
//        try {
//            LocalFileHeader localFileHeader;
//
//            net.lingala.zip4j.io.inputstream.ZipInputStream zis = new net.lingala.zip4j.io.inputstream.ZipInputStream(uploadedFile.getInputstream(), password.toCharArray());
//            localFileHeader = zis.getNextEntry();
//
//            while (localFileHeader != null) {
//                String fileName = localFileHeader.getFileName();
//                File extractedFile = new File(fileName);
//                StringBuilder sb = new StringBuilder();
//
//                InputStreamReader is = new InputStreamReader(zis, "UTF-8");
//
//                BufferedReader br = new BufferedReader(is);
//                String read = br.readLine();
//
//                while (read != null) {
//                    sb.append(read);
//                    read = br.readLine();
//                }
//                inMemoryFiles.put(fileName, sb.toString());
//
//                localFileHeader = zis.getNextEntry();
//
//            }
//
//            zis.close();
//
//        } catch (ZipException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return inMemoryFiles;
//
//
//    }


    public static byte[] copyStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = 0;
        while ((n = is.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }
        output.close();
        return output.toByteArray();

    }


//    public void handleFileUpload(FileUploadEvent e) throws Exception {
//        if (e.getFile() != null) {
//            //Get file
//            this.file = e.getFile();
//
//            try {
//
//
//                Map<String, String> tempMap = FileZipper.unlockZipWithCode(e.getFile(), "mike");
//
//                Iterator it = tempMap.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry pair = (Map.Entry) it.next();
//
//                    System.out.println(pair.getKey() + " = " + pair.getValue());
////                    it.remove();
//                }
//
//                System.out.println("worked");
//
//
////                ObjectMapper mapper = new ObjectMapper();
////
////                JsonNode KenNode = mapper.readTree(e.getFile().getInputstream());
////
////                okhttp3.RequestBody body = okhttp3.RequestBody.create(MEDIA_TYPE_JSON_TYPE, KenNode.toString());
////
////                Request request = new Request.Builder()
////                        .url(BaseURL + SERVICE_URL)
////                        .put(body)
////                        .build();
////
////
////                Response response = httpClient.newCall(request).execute();
////
////                if (response.isSuccessful()) {
////                    FacesMessage msg = new FacesMessage("Επιτυχές ανεβασμα αρχειου!", e.getFile().getFileName() + " is uploaded");
////                    FacesContext.getCurrentInstance().addMessage(null, msg);
////
////                    String json = response.body().string();
////
////                    Gson gson = new Gson();
////                    List<KenSyncError> testMeList = gson.fromJson(json , new TypeToken<List<KenSyncError>>() {}.getType());
////
////
////                    //TODO: Εδω θα πρέπει να βάλουμε να βγάζει λίστα με τα error  που θα έχει
////
////
////                }
////
////                response.close();
//            } catch (Exception ex) {
//                throw new Exception(ex.getMessage());
//            }
//
//        }
//    }
}
