package org.insightcentre.nlp.saffron.documentindex;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentIndexApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting Document Index App");
        SpringApplication.run(DocumentIndexApplication.class, args);
    }
}
