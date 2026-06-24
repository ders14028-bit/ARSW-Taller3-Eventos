package eda;

import java.util.Scanner;

public class MainConsumidor {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("==============================================");
        System.out.println("  CONSUMIDOR - EDA Redis Streams ARSW 2026-i");
        System.out.println("==============================================");
        System.out.println("  Grupos disponibles:");
        System.out.println("    1  fraude-group");
        System.out.println("    2  notif-group");
        System.out.println("    3  auditoria-group");
        System.out.println("==============================================");
        System.out.print("Elige grupo (1/2/3): ");

        String grupo = switch (sc.nextLine().trim()) {
            case "1" -> "fraude-group";
            case "2" -> "notif-group";
            case "3" -> "auditoria-group";
            default  -> "fraude-group";
        };

        System.out.println("\nModo:");
        System.out.println("  1  Normal (procesa y hace XACK)");
        System.out.println("  2  Simular CAIDA (sin XACK -> queda pendiente)");
        System.out.println("  3  Ver pendientes (XPENDING)");
        System.out.println("  4  Reclamar pendiente (XCLAIM + XACK)");
        System.out.print("Elige modo (1/2/3/4): ");

        String modo = sc.nextLine().trim();

        switch (modo) {
            case "3" -> {
                Consumidor.verPendientes();
                RedisConnection.cerrar();
            }
            case "4" -> {
                Consumidor.reclamarPendiente(grupo);
                RedisConnection.cerrar();
            }
            default -> {
                boolean simularCaida = modo.equals("2");
                System.out.println("\n[" + grupo + "] Esperando eventos... (BLOCK 0)\n");
                // Loop: escucha continuamente
                while (true) {
                    Consumidor.escuchar(grupo, simularCaida);
                }
            }
        }
    }
}
