package org.entryPoint;

import java.io.IOException;

public class App {
   public static void main(String[] args) {     
        try {
            LawsSource.access();
            // Scrapper.test();
            
        } catch (IOException e) {
            System.err.println(e);

        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }
}
