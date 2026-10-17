# Bank Heist

A 2D grid-based chase game built in **Java with Swing and Maven**, featuring player movement, guard pursuit, collectibles, scoring, traps, objective progression, and win/loss game states.

Developed collaboratively by a five-person software engineering team at Simon Fraser University, the project focused on object-oriented design, gameplay-system integration, automated testing, debugging, and iterative development.

## Demo

[▶ Watch Gameplay Demo](https://youtu.be/ZiidlGFFCcc)

![Bank Heist Gameplay](BankHeist/src/main/resources/group1/interface/BckTutorial.png)

## Gameplay

The player controls a thief navigating through a bank while attempting to complete a heist.

### Objectives

- Navigate the map using **WASD**
- Collect money and diamonds
- Avoid traps
- Evade a guard that actively pursues the player
- Complete the required objectives
- Reach the exit to successfully complete the heist

## Key Features

- Grid-based player movement
- Guard pursuit behavior
- Money and diamond collectible systems
- Score tracking and objective progression
- Trap mechanics
- Win and loss game states
- Exit and level-completion logic
- Object-oriented gameplay architecture
- Maven build automation
- Automated testing
- Team-based Git development workflow

## Technologies

- **Java 17**
- **Java Swing**
- **Maven**
- **Object-Oriented Programming**
- **Git / GitHub**
- **Automated Testing**

## Software Engineering Highlights

### Object-Oriented Design

Gameplay functionality is organized into modular Java classes representing game entities, interactions, and application behavior.

This structure separates gameplay responsibilities and makes individual systems easier to develop, debug, test, and integrate.

### Gameplay Systems

The game combines several interacting runtime systems, including:

- Player movement
- Guard pursuit
- Collectible handling
- Score management
- Trap interactions
- Objective tracking
- Win/loss state transitions

### Build Automation

Maven is used to provide a repeatable development workflow for:

- Compilation
- Testing
- Dependency management
- Packaging
- Javadoc generation

### Testing & Debugging

The project uses automated testing alongside iterative gameplay testing to validate application behavior and identify integration issues during development.

## My Contributions

This project was developed collaboratively by a five-person team.

My work included:

- Gameplay development and feature integration
- Debugging gameplay and application behavior
- Testing and validating game functionality
- Integrating project components with the shared team codebase
- Resolving issues during iterative development and integration

> This section describes my individual contributions to the team project. The complete game represents the combined work of all five team members.

## Project Structure

```text
BankHeist/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── pom.xml
└── target/
