package beans;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;


public class ListBCServicesBean {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        Provider p = Security.getProvider("BC");
        System.out.println("Provider: " + p.getName() + " " + p.getVersionStr());

        p.getServices().stream()
                .filter(s ->
                        s.getType().equalsIgnoreCase("Signature")
                                || s.getType().equalsIgnoreCase("KeyPairGenerator"))
                .map(s -> s.getType() + " -> " + s.getAlgorithm())
                .sorted()
                .forEach(System.out::println);
    }
}
