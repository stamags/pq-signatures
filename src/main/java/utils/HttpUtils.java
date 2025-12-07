package utils;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpUtils {

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException { }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException { }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
        public static void initTrustAllClient() {

            try {
                TrustManager[] trustAll = new TrustManager[]{new TrustAllManager()};

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAll, new SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HostnameVerifier allHostValid = new HostnameVerifier() {
                    @Override
                    public boolean verify(String string, SSLSession ssls) {
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(allHostValid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
