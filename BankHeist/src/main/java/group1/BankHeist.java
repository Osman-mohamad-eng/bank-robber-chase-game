package group1;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import java.net.URI;
import java.nio.file.Path;
import group1.results.*;

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
      javax.swing.SwingUtilities.invokeLater(BankHeist::launch);
    }

    private static void launch() {
      
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
      int level = Integer.parseInt(System.getProperty("bankheist.level", "3"));
      if (level < 1 || level > 5) throw new IllegalArgumentException("Level must be 1–5.");
      Board board = factory.makeBoard(level);
      board.setPaused(true);

      // Set board size and position
      fillBounds(board);

      // Initialize User Interface and link to the board+
      Interface ui;
      try {
        Path dataDirectory = Path.of(System.getProperty("bankheist.dataDir",
                Path.of(System.getProperty("user.home"), ".bankheist").toString()));
        PlayerProfile profile = PlayerProfile.loadOrCreate(dataDirectory,
                System.getProperty("bankheist.username", System.getenv("BANK_HEIST_USERNAME")));
        HttpGameServicesClient api = new HttpGameServicesClient(URI.create(
                System.getProperty("bankheist.apiUrl", System.getenv().getOrDefault("BANK_HEIST_API_URL", "http://localhost:8080"))));
        AsyncResultReporter reporter = new AsyncResultReporter(api, profile,
                new PendingResultStore(dataDirectory.resolve("pending")));
        Runtime.getRuntime().addShutdownHook(new Thread(reporter::close));
        ui = new Interface(board, reporter, profile.playerId(), level);
        frame.setTitle("Bank Heist — " + profile.username());
        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu services = new javax.swing.JMenu("Game services");
        javax.swing.JMenuItem openServices = new javax.swing.JMenuItem("Statistics, quests and global leaderboards");
        Interface activeUi = ui;
        openServices.addActionListener(event -> {
          if (activeUi.getState() == Interface.GameState.PLAYING) {
            activeUi.setState(Interface.GameState.PAUSED);
          }
          GameServicesWindow.open(api, profile);
        });
        services.add(openServices);
        menuBar.add(services);
        frame.setJMenuBar(menuBar);
      } catch (Exception failure) {
        java.util.logging.Logger.getLogger(BankHeist.class.getName())
                .log(java.util.logging.Level.WARNING, "Game services disabled: check profile and API configuration", failure);
        ui = new Interface(board);
      }
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
