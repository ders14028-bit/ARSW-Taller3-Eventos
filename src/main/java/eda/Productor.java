package eda;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Productor {

    private static final String STREAM = "banco.transferencias";
    private static final AtomicInteger contador = new AtomicInteger(1000);

    public static void publicar() {
        Jedis jedis = RedisConnection.get();

        String eventId    = "evt-" + contador.incrementAndGet();
        String transferId = "tr-"  + (int)(Math.random() * 9000 + 1000);
        String from       = "cta-" + (int)(Math.random() * 900  + 100);
        String to         = "cta-" + (int)(Math.random() * 900  + 100);
        String amount     = String.valueOf((int)(Math.random() * 500000 + 10000));

        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("eventType",  "TransferenciaCreada");
        campos.put("eventId",    eventId);
        campos.put("transferId", transferId);
        campos.put("from",       from);
        campos.put("to",         to);
        campos.put("amount",     amount);
        campos.put("currency",   "COP");
        campos.put("createdAt",  Instant.now().toString());

        StreamEntryID redisId = jedis.xadd(STREAM, XAddParams.xAddParams(), campos);

        System.out.println("[PRODUCTOR] Evento publicado");
        System.out.println("  Redis ID   : " + redisId);
        System.out.println("  eventId    : " + eventId);
        System.out.println("  transferId : " + transferId);
        System.out.println("  amount     : $" + amount + " COP");
        System.out.println("  from -> to : " + from + " -> " + to);
    }
}
