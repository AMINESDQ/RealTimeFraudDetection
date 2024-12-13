# Étape 1 : Utiliser l'image de base Amazon Corretto 17
FROM amazoncorretto:17.0.12

# Étape 2 : Définir le répertoire de travail
WORKDIR /app

# Étape 3 : Copier le fichier JAR dans l'image Docker
COPY ./app.jar app.jar

# Étape 4 : Spécifier la commande pour exécuter l'application
CMD ["java", "-jar", "app.jar"]
