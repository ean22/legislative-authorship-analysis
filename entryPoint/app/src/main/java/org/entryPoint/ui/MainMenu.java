package org.entryPoint.ui;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.entryPoint.service.LawsSource;

public class MainMenu {
  private static final Scanner scanner = new Scanner(System.in);

  public static void greeting() {
    System.out.println("Bem-vindo ao sistema de análise de autoria legislativa!");
    newLine();
  }

  public static void init() {
    menuController();
  }
  
  public static void menu() {
    System.out.println("Escolha uma opção:");
    System.out.println("1. Baixar normas");
    System.out.println("2. Listar normas");
    System.out.println("3. Sair");
  }

  public static void exit() {
    System.out.println("Saindo do sistema...");
    newLine();
  }

  public static void invalidOption() {
    System.out.println("Opção inválida. Tente novamente.");
    newLine();
  }

  public static void fetching() {
    System.out.println("Baixando normas...");
    newLine();
  }

  public static void listing() {
    System.out.println("Listando normas...");
    newLine();
  }

  public static void pickInitialDate() {
    System.out.print("Digite a data inicial (formato: YYYY-MM-DD): ");
  }

  public static void pickFinalDate() {
    System.out.print("Digite a data final (formato: YYYY-MM-DD): ");
  }

  private static void newLine() {
    System.out.println();
  }

  private static void menuController() {
    int option = 0;

    do {
      menu();

    try { 
      String input = scanner.nextLine(); 
      option = Integer.parseInt(input); 
    } catch (NumberFormatException e) { 
      System.out.println("Entrada inválida. Por favor, insira um número."); 
      newLine();
      continue; 
    }

      switch (option) {
        case 1:
          fetching();

          pickInitialDate();
          String initialDate = scanner.next();
          newLine();

          pickFinalDate();
          String finalDate = scanner.next();
          newLine();

          LawsSource.setDateRange(initialDate, finalDate);
          LawsSource.access();

          break;

        case 2:
          listing();
          LawsSource.listLaws();
          newLine();

          break;

        case 3:
          exit();
          
          break;

        default:
          invalidOption();

          break;
      }

    } while (option != 3);

    scanner.close();
  }
}
