# **CMPT 276 Group 1 Project `Bank Heist`**

**Course:** CMPT 276 D300 \- Introduction to Software Engineering  
**Instructor:** Jocelyn Minns  
**Semester:** Spring 2026  

## **Group Members**

| Name | Student ID | Email |
| :---- | :---- | :---- |
| Brian Chan | 301439177 | bca93@sfu.ca |
| Daniil Bugakov | 301540700 | dba73@sfu.ca |
| Dev Tomar | 301548576 | dst9@sfu.ca |
| Keith Ng | 301625387 | kwn6@sfu.ca |
| Mohamad Osman | 301590514 | moa45@sfu.ca |


## **1\. Project Overview & Description**

Bank Heist is a 2D grid-based game developed in Java using Swing.

The player controls a Thief navigating through a bank while:

* Collecting money and diamonds.
* Avoiding traps and a Guard that actively chases the player.


## **2\. Gameplay**

1. Use WASD to move the Thief.
2. Collect all money and diamonds to earn points.
3. Avoid the Guard (game ends if caught) and traps (game ends if score gets negative).
4. Reach the exit after collecting all the money on the map. 

![Tutorial Gameplay Guide](BankHeist/src/main/resources/group1/interface/BckTutorial.png)


## **3\. Video Demo**

[**▶️ Watch Project Demo on YouTube**](https://youtu.be/ZiidlGFFCcc)


## **4\. Prerequisites (Fresh Environment)**

To run this project, you need:

* **Java 17** or higher. 
* **Maven 3.8** or higher.
* (Optional) VS Code.


## **5\. Build Automation**

### **Compile**

Clone the repository and run: 
```bash
mvn clean compile
```

### **Run**

To run the game:
```bash
mvn exec:java
```

### **Test**

To test run:
```bash
mvn test
```

### **Artifacts**

To generate artifacts run:
```bash
mvn clean package javadoc:javadoc
```
Artifacts are generated inside the target folder.
