package net.sadiq;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TransactionService {

    private final InfluxDBClient influxDBClient;
    private final String bucket;
    private final String org;

    public TransactionService(String url, String token, String bucket, String org) {
        this.influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray());
        this.bucket = bucket;
        this.org = org;
    }

    public void addTransaction(String userId, double amount, LocalDateTime timestamp) {
        // Créer un point pour la transaction
        Point point = Point.measurement("suspicious_transactions")
                .addTag("userId", userId)
                .addField("amount", amount)
                .time(timestamp.toInstant(ZoneOffset.UTC), WritePrecision.NS);

        // Écrire dans InfluxDB
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'enregistrement de la transaction : " + e.getMessage());
        }

        System.out.println("Transaction suspecte enregistrée : userId=" + userId + ", amount=" + amount);
    }
}
