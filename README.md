# Scribes' Lectern Plugin

A baseline Hytale plugin scaffold for the Scrolls mod (Phase Two). This repository has been cleaned of WorldBorder logic and is ready for Scrolls implementation.

## Status
- Plugin scaffold only (no gameplay logic yet)
- Scrolls features to be implemented in Phase Two

## Build and Deploy

### Prerequisites
- Java 25 or higher
- Maven 3.x
- `HytaleServer.jar` in the parent directory
- SSH access to your deployment server

### Setup

1. **Configure Environment Variables**

   Copy the example environment file:
   ```sh
   cp .env.example .env
   ```

   Edit `.env` with your server details:
   ```env
   SERVER_IP=your.server.ip.here
   SERVER_USER=your_ssh_user
   REMOTE_PATH=/path/to/server/mods/
   ```

2. **Deploy to Server**

   Run the deployment script:
   ```sh
   ./deploy.sh
   ```

The script will:
- Load configuration from `.env`
- Install `HytaleServer.jar` to your local Maven repository (if needed)
- Compile and package the plugin using Maven
- Upload the generated JAR to the configured server using SCP
- Restart the server (requires `../restart_server.sh`)

### Manual Build

To build without deploying:
```sh
mvn clean package
```

The compiled JAR will be in `target/scribes_lectern-0.1.0-SNAPSHOT.jar`.
