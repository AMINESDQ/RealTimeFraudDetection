package net.sadiq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class FraudDetectionStream {

    private final TransactionService transactionService;

    public FraudDetectionStream(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public KStream<String, String> processTransactions(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("transactions-input", Consumed.with(Serdes.String(), Serdes.String()));

        // Log des transactions
        stream.peek((key, value) -> System.out.println("Transaction consommée: key=" + key + ", value=" + value));

        // Filtrer les transactions suspectes
        KStream<String, String> suspiciousTransactions = stream.filter((key, value) -> {
            double amount = extractAmountFromJson(value);
            return amount > 10000;
        });

        // Enregistrer les fraudes dans InfluxDB via TransactionService
        suspiciousTransactions.peek((key, value) -> {
            double amount = extractAmountFromJson(value);
            String userId = extractUserIdFromJson(value);
            LocalDateTime timestamp = extractTimestampAsDate(value);
            System.out.println("Transaction frauduleuse détectée : key=" + key + ", amount=" + amount);
            transactionService.addTransaction(userId, amount, timestamp);
        });

        // Publier les transactions suspectes
        suspiciousTransactions.to("fraud-alerts", Produced.with(Serdes.String(), Serdes.String()));

        return stream;
    }

    private double extractAmountFromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            return rootNode.path("amount").asDouble();
        } catch (JsonProcessingException e) {
            System.err.println("Erreur lors de l'extraction du montant: " + e.getMessage());
            return 0;
        }
    }

    private String extractUserIdFromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            return rootNode.path("userId").asText();
        } catch (JsonProcessingException e) {
            System.err.println("Erreur lors de l'extraction de userId: " + e.getMessage());
            return "unknown";
        }
    }

    public LocalDateTime extractTimestampAsDate(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            String timestamp = rootNode.path("timestamp").asText();

            if (timestamp.isEmpty()) {
                System.err.println("Le champ 'timestamp' est manquant ou vide.");
                return null;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return LocalDateTime.parse(timestamp, formatter);

        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction ou de la conversion du timestamp : " + e.getMessage());
            return null;
        }
    }
}
