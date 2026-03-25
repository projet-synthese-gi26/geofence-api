# Guide de Déploiement - Geofence Reactive

## Prérequis

### 1. Installation de PostgreSQL

```bash
# Mise à jour des paquets
sudo apt update

# Installation de PostgreSQL
sudo apt install postgresql postgresql-contrib
```

### 2. Installation de PostGIS

```bash
# Installation de PostGIS pour PostgreSQL
sudo apt install postgis postgresql-postgis
```

## Configuration de la Base de Données

### 1. Connexion à PostgreSQL

```bash
# Se connecter à PostgreSQL en tant qu'utilisateur postgres
sudo -u postgres psql
```

### 2. Création de la Base de Données

```sql
-- Créer une nouvelle base de données
CREATE DATABASE geofence_db;

-- Créer un utilisateur pour l'application
CREATE USER geofence_user WITH PASSWORD 'votre_mot_de_passe';

-- Accorder les privilèges sur la base de données
GRANT ALL PRIVILEGES ON DATABASE geofence_db TO geofence_user;

-- Se connecter à la base de données
\c geofence_db;

-- Activer l'extension PostGIS
CREATE EXTENSION POSTGIS;

-- Vérifier l'installation de PostGIS
SELECT PostGIS_Version();
```

### 3. Quitter PostgreSQL

```sql
\q
```

## Configuration Spring Boot

### 1. Configuration application.properties

```properties
# Configuration de la base de données PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/geofence_db
spring.datasource.username=geofence_user
spring.datasource.password=votre_mot_de_passe
spring.datasource.driver-class-name=org.postgresql.Driver

# Configuration JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect

# Configuration des logs
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### 2. Dépendances nécessaires (pom.xml)

Assurez-vous d'avoir ces dépendances dans votre `pom.xml` :

```xml
<dependencies>
    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Hibernate Spatial pour PostGIS -->
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-spatial</artifactId>
    </dependency>
</dependencies>
```

## Lancement de l'Application

### 1. Compilation et exécution

```bash
# Compilation du projet
mvn clean compile

# Lancement de l'application
mvn spring-boot:run
```

### 2. Vérification

- L'application devrait démarrer sur `http://localhost:8080`
- Vérifiez les logs pour confirmer la connexion à la base de données
- Les tables devraient être créées automatiquement grâce à `spring.jpa.hibernate.ddl-auto=update`

## Commandes Utiles

### PostgreSQL

```bash
# Redémarrer PostgreSQL
sudo systemctl restart postgresql

# Vérifier le statut
sudo systemctl status postgresql

# Se connecter à une base spécifique
psql -h localhost -U geofence_user -d geofence_db
```

### PostGIS

```sql
-- Vérifier les extensions installées
SELECT * FROM pg_extension;

-- Lister les fonctions PostGIS disponibles
SELECT proname FROM pg_proc WHERE proname LIKE 'st_%';
```

## Troubleshooting

### Problèmes courants

1. **Erreur de connexion** : Vérifiez que PostgreSQL est démarré et que les credentials sont corrects
2. **Extension PostGIS manquante** : Assurez-vous que PostGIS est installé et activé dans la base
3. **Port occupé** : Changez le port dans `application.properties` si nécessaire

### Logs à vérifier

- Logs de PostgreSQL : `/var/log/postgresql/`
- Logs Spring Boot : Console de l'application