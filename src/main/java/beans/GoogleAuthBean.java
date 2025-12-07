package beans;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.inject.Named;

@Named("GoogleAuthBean")
@SessionScoped
public class GoogleAuthBean implements Serializable {
    private String clientId;
    private String clientSecret;
    private static final String REDIRECT_URI = "http://localhost:8080/ken/callback.xhtml";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private String authCode;
    private String userName;
    private String userEmail;
    private String userPicture;
    private String accessToken;
    private Image image;

    public GoogleAuthBean() {
        loadClientCredentials();
    }

    private void loadClientCredentials() {
        try {
            String json = new String(Files.readAllBytes(Paths.get("C:\\Users\\PX\\Desktop\\credentials.json")));
            Map<String, Object> credentials = new Gson().fromJson(json, Map.class);
            Map<String, String> web = (Map<String, String>) credentials.get("web");
            this.clientId = web.get("client_id");
            this.clientSecret = web.get("client_secret");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authenticate() throws IOException {
        String state = UUID.randomUUID().toString();
        FacesContext.getCurrentInstance().getExternalContext().redirect(
                AUTH_URL + "?client_id=" + clientId + "&redirect_uri=" + REDIRECT_URI +
                        "&response_type=code&scope=openid%20email%20profile&state=" + state);
    }

    public void exchangeCodeForToken() {
        if (authCode != null) {
            try {
                TokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                        new NetHttpTransport(),
                        JacksonFactory.getDefaultInstance(),
                        "https://oauth2.googleapis.com/token",
                        clientId,
                        clientSecret,
                        authCode,
                        REDIRECT_URI)
                        .execute();

                accessToken = response.getAccessToken();
                fetchUserInfo();


                FacesContext.getCurrentInstance().getExternalContext().redirect("home.xhtml");

                // Add a success message
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Authentication successful!", null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchUserInfo() {
        try {
            // Call Google's UserInfo API to get user details
            URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String jsonResponse = scanner.useDelimiter("\\A").next();
                scanner.close();

//                 Parse JSON response
                JsonObject jsonObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
                userName = jsonObject.get("name").getAsString();
                userEmail = jsonObject.get("email").getAsString();
                userPicture = jsonObject.get("picture").getAsString();


            } else {
                System.err.println("Failed to fetch user info. Response Code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void userPicturelogout() throws IOException {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        FacesContext.getCurrentInstance().getExternalContext().redirect("home.xhtml");
    }

    // Getters and Setters
    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserPicture() {
        return userPicture;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserPicture(String userPicture) {
        this.userPicture = userPicture;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
