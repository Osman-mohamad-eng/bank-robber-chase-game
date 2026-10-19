# Bank Heist

A 2D grid-based chase game developed in **Java using Swing and Maven**.

The player controls a thief navigating through a bank while collecting money and diamonds, avoiding traps, and escaping from a pursuing guard. The project was developed collaboratively by a five-person software engineering team at Simon Fraser University.

## Live Demo

[🎮 Play Bank Heist](https://osman-mohamad-eng.github.io/bank-robber-chase-game/)

[▶ Watch Gameplay Demo](https://youtu.be/ZiidlGFFCcc)

![Bank Heist Gameplay](BankHeist/src/main/resources/group1/interface/BckTutorial.png)

## Gameplay

The player controls a thief navigating through a bank while attempting to complete a heist.

### Objectives

- Move using **WASD**
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
- Object-oriented Java architecture
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

### Testing and Debugging

The project uses automated testing alongside iterative gameplay testing to validate application behavior and identify integration issues during development.

## My Contributions

This project was developed collaboratively by a five-person team.

My contributions included:

- Gameplay development and feature integration
- Debugging gameplay and application behavior
- Testing and validating game functionality
- Integrating project components with the team's shared codebase
- Supporting iterative development and issue resolution throughout the project

> The complete game represents the combined work of the full team.

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
