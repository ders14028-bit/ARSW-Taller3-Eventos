package eda;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;

import java.util.Scanner;

public class MainProductor {

    private static final String STREAM = "banco.transferencias";
    private static final String[] GRUPOS = {"fraude-group", "notif-group", "auditoria-group"};

    public static void main(String[] args) {
        inicializarGrupos();
        Scanner sc = new Scanner(System.in);

        System.out.println("==============================================");
        System.out.println("  PRODUCTOR - EDA Redis Streams ARSW 2026-i");
        System.out.println("==============================================");
        System.out.println("  Presiona ENTER para publicar un evento.");
        System.out.println("  Escribe 'salir' para terminar.");
        System.out.println("==============================================\n");

        while (true) {
            System.out.print(">> ");
            String linea = sc.nextLine().trim();
            if (linea.equalsIgnoreCase("salir")) {
                RedisConnection.cerrar();
                System.out.println("Productor detenido.");
                break;
            }
            Productor.publicar();
            System.out.println();
        }
    }

    private static void inicializarGrupos() {
        Jedis jedis = RedisConnection.get();
        for (String g : GRUPOS) {
            try {
                jedis.xgroupCreate(STREAM, g, new StreamEntryID("0-0"), true);
            } catch (Exception e) {
                // BUSYGROUP: ya existe, ignorar
            }
        }
        System.out.println("Grupos listos: fraude-group, notif-group, auditoria-group\n");
    }
}
