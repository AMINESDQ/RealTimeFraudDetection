package net.sadiq;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        // Configuration de InfluxDB
        String influxUrl = "http://influxdb:8086";
        String influxToken = "0XL4j1rfDdpCr0q5hjJJxOaIjoWtJGLuw9-J83hGezfAKH0jmAdsT-fLpLgIZY18Az2wPQTToNbZ7yh8LDlsNQ==";
        String influxBucket = "aminesdq";
        String influxOrg = "aminesdq";

        TransactionService transactionService = new TransactionService(influxUrl, influxToken, influxBucket, influxOrg);
        FraudDetectionStream fraudDetectionStream = new FraudDetectionStream(transactionService);

        // Configuration Kafka Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fraud-detection-app"); // ID de l'application
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "http://host.docker.internal:9092");   // Adresse du serveur Kafka
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        fraudDetectionStream.processTransactions(streamsBuilder);

        KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), props);
        kafkaStreams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
}
