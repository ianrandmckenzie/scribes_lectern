# Scribes’ Lectern: Adventure Untethered

Stop acting like a console operator and start playing like a hero.

In the world of Hytale, immersion is everything. Yet, the moment you have to type /home or /spawn, that magic flickers. Scribes’ Lectern is a comprehensive utility framework designed to bridge the gap between powerful server commands and tactile, in-game gameplay.

By shifting essential navigation into a physical, item-based system, we return the focus to your inventory and your journey—not your keyboard.

## Usage

To craft your scrolls, head over to the vanilla crafting bench under the Survival tab.

To use the scrolls, put them on your hotbar and hold left click.

Server admins, to adjust random teleport distance or how long it takes to teleport, edit the auto-generated config JSON added to your mods folder.

## Key Features

*   **Tactile Navigation:** Replace immersion-breaking text commands with beautifully crafted magical scrolls.
*   **The Scroll of Home Binding:** Eschew the chat box; use this scroll to tether your soul to your sanctuary, base, bed, or whatever you call home, home.
*   **The Scroll of Home Teleport:** A lifeline back to safety. If no home is set, the winds of magic will carry you back to the world's dawn (Spawn).
*   **The Scroll of Random Teleport (RTP):** For the true pioneer. Shatter this scroll to be whisked away to a safe, uncharted corner of the world.

## Why Scribes’ Lectern?

Most servers turn players into "grammarians" just to move around. Scribes’ Lectern is built with global accessibility and gameplay balance in mind:

*   **Language Agnostic:** No more struggling with English syntax or typos; if you can click an item, you can travel.
*   **Balanced Economy:** Server owners can gate these scrolls behind crafting recipes, rare loot tables, or cooldowns, ensuring that teleportation feels like a earned reward rather than a "cheat."
*   **Physicality:** Every movement has a cost and a visual presence, keeping the stakes high and the world feeling "real."

## On the Horizon

The library of the Scribe is ever-expanding. Future updates include Graphical Friend Menus for party-based teleportation and Imbued Scrolls that use weighted logic to find your allies in the vast wilderness of Orbis.


## Feedback

1. Issues with translation or want to contribute your native tongue? Please contact Ian, `hey@relentlesscurious.com`
2. Love this mod and want more like it? Why not buy from the store or request a commission? Visit [relentlessCurious.com](https://www.relentlesscurious.com)
3. Issues with the mod itself? Please visit [the support page](https://www.relentlesscurious.com/faq)

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

The compiled JAR will be in `target/scribes_lectern-0.1.1-SNAPSHOT.jar`.
