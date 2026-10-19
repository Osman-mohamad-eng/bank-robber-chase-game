package group1.results;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Read-only views of server statistics, quests and global leaderboards. */
public final class GameServicesWindow {
    private GameServicesWindow() { }

    public static void open(HttpGameServicesClient api, PlayerProfile profile) {
        JFrame frame = new JFrame("Game services — " + profile.username());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Statistics", view(api, profile, "/players/" + profile.playerId() + "/stats", "stats"));
        tabs.addTab("Quests", view(api, profile, "/players/" + profile.playerId() + "/quests", "quests"));
        for (String metric : new String[]{"score", "money", "time"}) {
            tabs.addTab(metric.equals("money") ? "Most cash" : metric.equals("time") ? "Fastest wins" : "Highest score",
                    view(api, profile, "/leaderboards/" + metric + "?limit=20", "leaderboard"));
        }
        JTextField identity = new JTextField("Player ID: " + profile.playerId());
        identity.setEditable(false);
        identity.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        frame.add(identity, BorderLayout.NORTH);
        frame.add(tabs);
        frame.setMinimumSize(new Dimension(760, 480));
        frame.setSize(900, 540);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JPanel view(HttpGameServicesClient api, PlayerProfile profile, String path, String kind) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel status = new JLabel("Loading…");
        JTable table = new JTable();
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(false);
        JButton refresh = new JButton("Refresh from server");
        Runnable load = () -> {
            refresh.setEnabled(false);
            status.setText("Loading…");
            CompletableFuture.runAsync(() -> {
                try { api.register(profile); }
                catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
            }).thenCompose(ignored -> api.get(path)).whenComplete((data, failure) -> SwingUtilities.invokeLater(() -> {
                refresh.setEnabled(true);
                if (failure != null) {
                    status.setText("Services unavailable. Gameplay remains available; use Refresh to try again.");
                } else {
                    table.setModel(model(data, kind));
                    status.setText(table.getRowCount() == 0 ? "No records yet." : "Retrieved from game services");
                }
            }));
        };
        refresh.addActionListener(event -> load.run());
        panel.add(status, BorderLayout.NORTH);
        panel.add(new JScrollPane(table));
        panel.add(refresh, BorderLayout.SOUTH);
        load.run();
        return panel;
    }

    private static DefaultTableModel model(JsonObject data, String kind) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        if (kind.equals("stats")) {
            model.setColumnIdentifiers(new String[]{"Statistic", "Value"});
            data.entrySet().forEach(entry -> model.addRow(new Object[]{entry.getKey(), value(entry.getValue())}));
        } else {
            String[] keys = kind.equals("quests")
                    ? new String[]{"title", "progress", "target", "completed", "rewardPoints"}
                    : new String[]{"rank", "username", "finalScore", "cashCollected", "completionTimeMs", "level", "outcome"};
            model.setColumnIdentifiers(kind.equals("quests")
                    ? new String[]{"Quest", "Progress", "Target", "Completed", "Reward points"}
                    : new String[]{"Rank", "Player", "Score", "Gross cash", "Time (ms)", "Level", "Outcome"});
            JsonArray items = data.getAsJsonArray("items");
            for (JsonElement item : items) {
                JsonObject object = item.getAsJsonObject();
                Object[] row = new Object[keys.length];
                for (int i = 0; i < keys.length; i++) row[i] = value(object.get(keys[i]));
                model.addRow(row);
            }
        }
        return model;
    }

    private static String value(JsonElement value) {
        return value == null || value.isJsonNull() ? "—" : value.getAsString();
    }
}
