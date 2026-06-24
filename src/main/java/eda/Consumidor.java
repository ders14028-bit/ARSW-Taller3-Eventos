package eda;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XClaimParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamPendingEntry;

import java.util.List;
import java.util.Map;

public class Consumidor {

    private static final String STREAM = "banco.transferencias";

    /** Escucha indefinidamente nuevos eventos del grupo (BLOCK 0). */
    public static void escuchar(String grupo, boolean simularCaida) {
        Jedis jedis = RedisConnection.get();

        // BLOCK 0 = espera infinita hasta que llegue un evento
        List<Map.Entry<String, List<StreamEntry>>> resultado = jedis.xreadGroup(
            grupo, "consumidor-1",
            XReadGroupParams.xReadGroupParams().count(1).block(0),
            Map.of(STREAM, StreamEntryID.UNRECEIVED_ENTRY)
        );

        if (resultado == null || resultado.isEmpty()) return;

        StreamEntry entry = resultado.get(0).getValue().get(0);
        Map<String, String> campos = entry.getFields();

        System.out.println("\n[" + grupo + "] Evento recibido");
        System.out.println("  Redis ID   : " + entry.getID());
        System.out.println("  eventId    : " + campos.get("eventId"));
        System.out.println("  amount     : $" + campos.get("amount") + " COP");
        System.out.println("  from -> to : " + campos.get("from") + " -> " + campos.get("to"));

        if (simularCaida) {
            System.out.println("  !! CONSUMIDOR CAIDO - sin XACK. Evento queda PENDIENTE.");
            return;
        }

        switch (grupo) {
            case "fraude-group"    -> System.out.println("  Antifraude: verificando patrones... OK");
            case "notif-group"     -> System.out.println("  Notificaciones: alerta enviada a " + campos.get("from") + "... OK");
            case "auditoria-group" -> System.out.println("  Auditoria: registro guardado eventId=" + campos.get("eventId") + "... OK");
        }

        jedis.xack(STREAM, grupo, entry.getID());
        System.out.println("  XACK enviado -> cursor avanzado.");
    }

    public static void verPendientes() {
        Jedis jedis = RedisConnection.get();
        String[] grupos = {"fraude-group", "notif-group", "auditoria-group"};

        System.out.println("\n[XPENDING] Pendientes por grupo:");
        for (String g : grupos) {
            List<StreamPendingEntry> pendientes = jedis.xpending(
                STREAM, g, new XPendingParams("-", "+", 10)
            );
            if (pendientes.isEmpty()) {
                System.out.println("  " + g + " : sin pendientes");
            } else {
                System.out.println("  " + g + " : " + pendientes.size() + " pendiente(s)");
                for (StreamPendingEntry e : pendientes) {
                    System.out.println("    -> ID: " + e.getID()
                        + " | consumer: " + e.getConsumerName()
                        + " | entregas: "  + e.getDeliveredTimes());
                }
            }
        }
    }

    public static void reclamarPendiente(String grupo) {
        Jedis jedis = RedisConnection.get();

        List<StreamPendingEntry> pendientes = jedis.xpending(
            STREAM, grupo, new XPendingParams("-", "+", 10)
        );

        if (pendientes.isEmpty()) {
            System.out.println("[XCLAIM] No hay pendientes en " + grupo);
            return;
        }

        StreamEntryID[] ids = pendientes.stream()
            .map(StreamPendingEntry::getID)
            .toArray(StreamEntryID[]::new);

        List<StreamEntry> reclamados = jedis.xclaim(
            STREAM, grupo, "consumidor-2", 0, new XClaimParams(), ids
        );

        System.out.println("[XCLAIM] Reclamando " + reclamados.size() + " mensaje(s) con consumidor-2:");
        for (StreamEntry e : reclamados) {
            Map<String, String> c = e.getFields();
            System.out.println("  Reprocesando eventId=" + c.get("eventId")
                + " amount=$" + c.get("amount") + " COP");
            jedis.xack(STREAM, grupo, e.getID());
            System.out.println("  XACK enviado -> procesado tras reintento.");
        }
    }
}
