All scrolls must be supported by the general requirements for the mod:

  * **General Technical Requirement:** Implement a robust system to bind custom functionality (teleportation, menu logic) to an in-game item base ("Scrolls").
  * **General Success Criteria:** All scrolls must have a unique, non-vanilla, and balanced crafting recipe implemented and functional.

-----

### ✅ 1. Scroll of Home Teleport

**Requirements**

  * Implement logic to locate the player's set bed location.
  * Implement a fallback mechanism to teleport the player to the default spawn point.
  * Scroll of Home Binding (/sethome)

**Success Criteria**

  * The scroll reliably teleports the player directly to their set bed.
  * If a bed is not set or accessible, the player is successfully teleported to the default spawn.

### ⏳ 2. Scroll of Friend Teleport

**Requirements**

  * Implement a system to retrieve a list of a player's online friends/allies, complete with their avatar faces.
  * Implement logic to initiate a teleport request to a selected online friend/ally.

**Success Criteria**

  * When the scroll is used, a searchable, graphical menu of online friends/allies pops up instantly.
  * Selecting a friend from the menu triggers the intended teleport sequence.

### 🗓️ 3. Scroll of Elysium Teleport

**Requirements**

  * Implement a direct-teleport mechanism to the player's personal, dedicated Elysium instance.

**Success Criteria**

  * The scroll reliably and instantly teleports the player to their Elysium instance upon use.

### ✅ 4. Scroll of Spawn Teleport

**Requirements**

  * Implement a direct-teleport mechanism to the designated Orbis spawn point.

**Success Criteria**

  * The scroll reliably teleports the player to the specific Orbis spawn location.

### ⏳ 5. Scroll of Random Teleport

**Requirements**

  * Implement an algorithm to select a completely random, safe, and valid location within the Orbis world border.

**Success Criteria**

  * The scroll teleports the player to a random spot within Orbis, avoiding invalid or dangerous locations (e.g., in the air, inside a wall).

### 🗓️ 6. Imbued Scroll of Random Teleport

**Requirements**

  * Implement all technical requirements of the standard Scroll of Random Teleport.
  * Implement a weighted probability system based on the special, expensive crafting ingredients.

**Success Criteria**

  * The core random teleport function works as intended.
  * The system successfully increases the chance of the player being teleported to a location near other players when the scroll is used.

### 🗓️ 7. Scroll of Registry Teleport

**Requirements**

  * Implement a direct-teleport mechanism to the specific **Elysium Instance of a Bureaucratic Building**.
  * Ensure seamless integration with the **Elysium claim charter registry NPC + Menu** system.
  * Ensure the claim management menu includes a working shop interface for subscription-based claim expansions.

**Success Criteria**

  * The scroll takes the player directly to the central claim management hub.
  * Talking to the clerk NPC after using the scroll successfully opens the menu for claim management.
  * Players can access the shop and the logic correctly interacts with the **Web integration for subscription-based/paid claim management**.
