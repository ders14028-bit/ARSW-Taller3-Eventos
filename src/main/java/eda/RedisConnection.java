package eda;

import redis.clients.jedis.Jedis;

public class RedisConnection {

    private static Jedis instancia;

    public static Jedis get() {
        if (instancia == null || !instancia.isConnected()) {
            instancia = new Jedis("localhost", 6379);
        }
        return instancia;
    }

    public static void cerrar() {
        if (instancia != null) instancia.close();
    }
}
