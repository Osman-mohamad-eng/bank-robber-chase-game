package group1;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;

/**
 * Launches the Bank Heist game.
 *
 * This class contains the main method that creates the game window, intializes board (playable area),
 * initializes user interface and sets up the layered pane to display game components
 * 
 * @author Daniil, Keith
 */
public class BankHeist {
    private static final Dimension WINDOW_SIZE = new Dimension(1600, 896);
  /**
 * Starts the Bank Heist application.
 * 
 * This method creates the main window, requests a board from BoardFactory, intializes user interface,
 * And adds all components to the layered pane.
 * 
 * @param args command line argument
 */
    public static void main(String[] args) {
      
      // Window properties
      JFrame frame = new JFrame("Bank Heist");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setResizable(false);

      /**
       * Allows components to be layered
       * Layer 0 = Board (playable area)
       * Layer 1 = User Interface
       */
      JLayeredPane layeredPane = new JLayeredPane();
      layeredPane.setLayout(null);
      layeredPane.setPreferredSize(WINDOW_SIZE);

      // Request a board from the factory
      BoardFactory factory = new BoardFactory();
      Board board = factory.makeBoard(3);
      board.setPaused(true);

      // Set board size and position
      fillBounds(board);

      // Initialize User Interface and link to the board+
      Interface ui = new Interface(board);
      fillBounds(ui);
      board.setInterface(ui);

      // Add components to layers (board = layer 0, UI = layer 1)
      layeredPane.add(board, Integer.valueOf(0));
      layeredPane.add(ui, Integer.valueOf(1));

      // Add layered pane to the frame
      frame.add(layeredPane);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    }

    // Sets component's bounds
    private static void fillBounds(java.awt.Component c) {
        c.setBounds(0, 0, WINDOW_SIZE.width, WINDOW_SIZE.height);
    }
}
