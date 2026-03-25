# 🌍 Geofence API

Un système complet de géofencing en temps réel pour le suivi et la gestion de véhicules avec zones géographiques personnalisables.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PostGIS-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 Table des matières

- [Aperçu](#aperçu)
- [Fonctionnalités](#fonctionnalités)
- [Architecture technique](#architecture-technique)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Utilisation](#utilisation)
- [Développement](#développement)
- [Déploiement](#déploiement)
- [Contribution](#contribution)

## 🎯 Aperçu

**Geofence API** est une solution backend complète pour la gestion de zones géographiques (géofences) et le suivi de véhicules en temps réel. Elle permet aux utilisateurs de créer des zones de surveillance, recevoir des alertes automatiques, et partager des géofences avec d'autres utilisateurs.

### Cas d'usage

- 🚚 **Logistique** : Suivi de flotte avec alertes d'entrée/sortie de zones
- 🏢 **Entreprises** : Surveillance de véhicules d'entreprise
- 👨‍👩‍👧‍👦 **Familial** : Suivi sécurisé de véhicules familiaux
- 🏗️ **Chantiers** : Surveillance d'équipements mobiles
- 🚌 **Transport public** : Gestion de zones de service

## ✨ Fonctionnalités

### 🔐 Authentification & Autorisation
- Authentification JWT avec refresh tokens
- Système de rôles (USER, MANAGER, ADMIN)
- Login via username, email ou téléphone
- Gestion sécurisée des sessions

### 🚗 Gestion des véhicules
- CRUD complet des véhicules
- Upload et gestion d'images
- Génération d'API keys pour devices mobiles
- Association avec zones de géofence

### 🗺️ Géofencing avancé
- **Zones circulaires** : Centre + rayon
- **Zones polygonales** : Formes complexes
- **Fork de géofences** : Copie et modification de zones existantes
- **Partage collaboratif** : Invitation et permissions

### 📍 Suivi de position
- API publique pour mise à jour depuis devices
- Historique complet des positions
- Calcul automatique des entrées/sorties de zones
- Support de métadonnées (vitesse, direction, altitude)

### 🚨 Système d'alertes
- Alertes temps réel (WebSocket)
- Types : ZONE_ENTER, ZONE_EXIT, SPEED_LIMIT, BATTERY_LOW
- Notifications push et email
- Historique des alertes

### 🤝 Collaboration
- **Partage de géofences** avec permissions
- **Liens d'invitation** publics avec expiration
- **Système d'approbation** pour invitations
- Gestion des droits d'édition

### 📊 Analytics & Reporting
- Statistiques utilisateur et système
- Métriques par véhicule
- Rapports d'activité
- Dashboard administrateur

## 🏗️ Architecture technique

### Stack technologique

**Backend**
- **Framework** : Spring Boot 3.4.3 (WebFlux - Réactif)
- **Sécurité** : Spring Security avec JWT
- **Base de données** : PostgreSQL + PostGIS (données géographiques)
- **ORM** : Hibernate Spatial
- **Validation** : Spring Validation
- **Mail** : Spring Mail
- **WebSocket** : Spring WebSocket (STOMP)

**Géospatial**
- **PostGIS** : Extensions géographiques PostgreSQL
- **JTS Topology Suite** : Calculs géométriques
- **Hibernate Spatial** : Mapping objet-spatial

**Infrastructure**
- **Serveur** : Netty (réactif)
- **CORS** : Configuration WebFlux
- **Documentation** : ApiDoc intégrée
- **Environment** : Dotenv pour variables

### Architecture réactive

```
Client Request → WebFlux Router → Service Layer → Repository → PostgreSQL
                      ↓
                 WebSocket → Real-time Alerts
```

## 🚀 Installation

### Prérequis

- **Java 21+**
- **Maven 3.6+**
- **PostgreSQL 12+** avec **PostGIS 3.0+**
- **Git**

### 1. Cloner le projet

```bash
git clone https://github.com/votre-username/geofence-api.git
cd geofence-api
```

### 2. Configuration de la base de données

```sql
-- Créer la base de données
CREATE DATABASE geofence_db;

-- Activer PostGIS
\c geofence_db;
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_topology;
```

### 3. Variables d'environnement

```bash
# Copier le fichier d'exemple
cp .env.example .env
```

Éditez `.env` avec vos paramètres :

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/geofence_db
DB_USERNAME=postgres
DB_PASSWORD=votre_mot_de_passe

# JWT
JWT_ENCRYPTION_KEY=votre_cle_jwt_securisee_64_caracteres

# Mail (Gmail exemple)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_app_password

# Application
FRONTEND_URL=http://localhost:3000
UPLOAD_DIR=./uploads
SERVER_PORT=8080
```

### 4. Installation et démarrage

```bash
# Installer les dépendances
mvn clean install

# Démarrer l'application
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8080`

## ⚙️ Configuration

### Variables d'environnement

| Variable | Description | Valeur par défaut |
|----------|-------------|-------------------|
| `DB_URL` | URL de la base PostgreSQL | `jdbc:postgresql://localhost:5432/geofence_db` |
| `DB_USERNAME` | Nom d'utilisateur BDD | `postgres` |
| `DB_PASSWORD` | Mot de passe BDD | - |
| `JWT_ENCRYPTION_KEY` | Clé de chiffrement JWT | - |
| `MAIL_HOST` | Serveur SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Port SMTP | `587` |
| `MAIL_USERNAME` | Email d'envoi | - |
| `MAIL_PASSWORD` | Mot de passe email | - |
| `FRONTEND_URL` | URL du frontend | `http://localhost:3000` |
| `UPLOAD_DIR` | Dossier d'upload | `./uploads` |
| `SERVER_PORT` | Port du serveur | `8080` |

### Configuration Gmail

Pour utiliser Gmail comme serveur SMTP :

1. Activez la **2FA** sur votre compte Gmail
2. Générez un **mot de passe d'application**
3. Utilisez ce mot de passe dans `MAIL_PASSWORD`

## 📚 API Documentation

### Accès à la documentation

- **URL** : `http://localhost:8080/api/v1/docs/index.html`
- **Format** : Documentation interactive avec exemples
- **Authentification** : Incluez le token JWT dans l'en-tête `Authorization: Bearer <token>`

### Endpoints principaux

#### Authentification
```http
POST /api/auth/register    # Inscription
POST /api/auth/login       # Connexion
GET  /api/auth/verify-user # Vérification token
```

#### Géofences
```http
GET    /api/geofence                    # Mes géofences
POST   /api/geofence                    # Créer géofence
GET    /api/geofence/{type}/{id}        # Détails géofence
PUT    /api/geofence/{type}/{id}        # Modifier géofence
DELETE /api/geofence/{type}/{id}        # Supprimer géofence
```

#### Véhicules
```http
GET    /api/vehicle           # Mes véhicules
POST   /api/vehicle           # Créer véhicule (avec image)
GET    /api/vehicle/{id}      # Détails véhicule
PUT    /api/vehicle/{id}      # Modifier véhicule
DELETE /api/vehicle/{id}      # Supprimer véhicule
```

#### Positions (API publique)
```http
POST /api/public/location/update  # Mise à jour position (API Key)
GET  /api/location/vehicle/{id}/history  # Historique
GET  /api/location/vehicle/{id}/latest   # Dernière position
```

## 🔧 Utilisation

### 1. Créer un compte

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "John",
    "lastname": "Doe",
    "username": "johndoe",
    "email": "john@example.com",
    "phoneNumber": "+1234567890",
    "password": "SecurePass123!",
    "password_confirmation": "SecurePass123!",
    "DOB": "1990-01-01"
  }'
```

### 2. Se connecter

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "type": "username",
    "username": "johndoe",
    "password": "SecurePass123!"
  }'
```

### 3. Créer une géofence circulaire

```bash
curl -X POST http://localhost:8080/api/geofence \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "circle",
    "title": "Zone Bureau",
    "description": "Périmètre autorisé",
    "center": {
      "coordinates": [3.8667, 11.5167]
    },
    "radius": 500.0
  }'
```

### 4. Créer un véhicule

```bash
curl -X POST http://localhost:8080/api/vehicle \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F 'vehicle={"brand":"Toyota","model":"Camry","licensePlate":"AB-123-CD"};type=application/json' \
  -F 'image=@vehicle_photo.jpg'
```

### 5. Mise à jour de position depuis un device

```bash
curl -X POST http://localhost:8080/api/public/location/update \
  -H "X-API-Key: YOUR_VEHICLE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 11.5167,
    "longitude": 3.8667,
    "speed": 45.5,
    "heading": 180.0
  }'
```

## 👨‍💻 Développement

### Structure du projet

```
src/
├── main/java/com/reseau/geofence/
│   ├── auth/                 # Authentification
│   ├── config/              # Configurations
│   ├── controller/          # Contrôleurs REST
│   ├── dto/                 # Objects de transfert
│   ├── exception/           # Gestion d'erreurs
│   ├── mapper/              # Mappers DTO/Entity
│   ├── model/               # Entités JPA
│   ├── repository/          # Repositories JPA
│   ├── security/            # Sécurité JWT
│   └── service/             # Services métier
├── main/resources/
│   ├── application.properties
│   └── static/docs/         # Documentation API
└── lib/                     # Dépendances locales
```

### Démarrage en mode développement

```bash
# Avec rechargement automatique
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"

# Avec debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Coverage
mvn jacoco:report
```

### Profils Spring

- **dev** : Développement local
- **test** : Tests automatisés
- **prod** : Production

## 🚀 Déploiement

### Docker

```dockerfile
FROM openjdk:21-jdk-slim

COPY target/geofence-0.0.1-SNAPSHOT.jar app.jar
COPY lib/ /app/lib/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build
docker build -t geofence-api .

# Run
docker run -p 8080:8080 --env-file .env geofence-api
```

### Docker Compose

```yaml
version: '3.8'
services:
  db:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: geofence_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"

  api:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
    env_file:
      - .env
```

### Production

1. **Construire le JAR**
   ```bash
   mvn clean package -Pprod
   ```

2. **Variables d'environnement de production**
   ```env
   DB_URL=jdbc:postgresql://prod-db:5432/geofence_db
   JWT_ENCRYPTION_KEY=votre_cle_production_ultra_securisee
   ```

3. **Lancement**
   ```bash
   java -jar -Dspring.profiles.active=prod target/geofence-0.0.1-SNAPSHOT.jar
   ```

## 🤝 Contribution

### Guidelines

1. **Fork** le projet
2. Créez une **branche feature** (`git checkout -b feature/AmazingFeature`)
3. **Committez** vos changements (`git commit -m 'Add some AmazingFeature'`)
4. **Push** vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une **Pull Request**

### Standards de code

- **Java** : Suivre les conventions Oracle
- **Commits** : Format conventionnel (`feat:`, `fix:`, `docs:`)
- **Tests** : Minimum 80% de couverture
- **Documentation** : Commenter les méthodes publiques

### Issues et bugs

Utilisez les **GitHub Issues** avec les labels appropriés :
- 🐛 `bug` : Bugs à corriger
- ✨ `enhancement` : Nouvelles fonctionnalités
- 📚 `documentation` : Améliorations de doc
- 🔧 `refactor` : Refactoring de code

## 📄 Licence

Ce projet est sous licence **MIT**. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 📞 Support

- **Email** : support@geofence-api.com
- **Documentation** : [API Docs](http://localhost:8080/api/v1/docs/index.html)
- **Issues** : [GitHub Issues](https://github.com/votre-username/geofence-api/issues)



---

**Fait avec ❤️ par [KAMGA CHEUKO FRANKLIN DAVY]**

> 🌟 Si ce projet vous aide, n'hésitez pas à lui donner une étoile !