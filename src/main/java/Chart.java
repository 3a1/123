import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

public class Chart extends JFrame
{
    private ChartPanel chartPanel;
    private final TimeSeries priceSeries = new TimeSeries("Price (USD)");
    private final JComboBox<String> currencyCombo = new JComboBox<>(new String[]{"BTC", "ETH", "XRP"});
    private final JComboBox<Integer> periodCombo = new JComboBox<>(new Integer[]{1, 7, 30});
    private final JLabel indicatorsLabel = new JLabel("Loading indicators...");
    private Connection databaseConnection;

    public Chart(String title)
    {
        super(title);
        setupUI();
        initializeDatabase();
    }

    private void setupUI()
    {
        chartPanel = new ChartPanel(createChart());
        chartPanel.setPreferredSize(new Dimension(800, 550));

        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Currency:"));
        controlPanel.add(currencyCombo);
        controlPanel.add(new JLabel("Period (days):"));
        controlPanel.add(periodCombo);

        currencyCombo.addActionListener(e -> updateData());
        periodCombo.addActionListener(e -> updateData());

        add(indicatorsLabel, BorderLayout.SOUTH);
        add(chartPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private JFreeChart createChart()
    {
        return ChartFactory.createTimeSeriesChart(
                "",
                "Time",
                "Price (USD)",
                new TimeSeriesCollection(priceSeries),
                false, true, false
        );
    }

    private void updateData()
    {
        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                String currency = currencyCombo.getSelectedItem().toString().toLowerCase();
                int days = (Integer) periodCombo.getSelectedItem();

                JSONArray prices = fetchData(currency, days);
                saveDataToDatabase(currency, prices); // Save data to the database
                updateChart(prices);

                System.out.println("Data saved to the database.");
                return null;
            }
        }.execute();
    }

    private void updateChart(JSONArray prices)
    {
        SwingUtilities.invokeLater(() -> {
            priceSeries.clear();
            for (int i = 0; i < prices.length(); i++) {
                JSONArray point = prices.getJSONArray(i);
                Second second = new Second(new java.util.Date(point.getLong(0)));
                priceSeries.addOrUpdate(second, point.getDouble(1));
            }
            chartPanel.repaint();
        });
    }

    private JSONArray fetchData(String currency, int days) throws Exception 
    {
        if (Objects.equals(currency, "btc"))
        {
            currency = "bitcoin";
        } else if (Objects.equals(currency, "eth")) 
        {
            currency = "ethereum";
        } else if (Objects.equals(currency, "xrp"))
        {
            currency = "ripple";
        }

        URL url = new URL("https://api.coingecko.com/api/v3/coins/" + currency +
                "/market_chart?vs_currency=usd&days=" + days);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) 
        {
            StringBuilder response = new StringBuilder();
            int data;
            while ((data = reader.read()) != -1) 
            {
                response.append((char) data);
            }
            return new JSONObject(response.toString()).getJSONArray("prices");
        }
    }

    private void initializeDatabase() {
        try 
        {
            databaseConnection = DriverManager.getConnection("jdbc:sqlite:prices.db");

            try (Statement stmt = databaseConnection.createStatement()) 
            {
                String createTableQuery = "CREATE TABLE IF NOT EXISTS prices (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "currency TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "price REAL NOT NULL)";
                stmt.execute(createTableQuery);
            }
        } catch (Exception e) 
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database initialization failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveDataToDatabase(String currency, JSONArray prices) 
    {
        String insertQuery = "INSERT INTO prices (currency, timestamp, price) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = databaseConnection.prepareStatement(insertQuery)) 
        {
            for (int i = 0; i < prices.length(); i++) {
                JSONArray point = prices.getJSONArray(i);
                long timestamp = point.getLong(0);
                double price = point.getDouble(1);

                pstmt.setString(1, currency);
                pstmt.setLong(2, timestamp);
                pstmt.setDouble(3, price);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save data: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> 
        {
            Chart chart = new Chart("Cryptocurrency Price Tracker");
            chart.setVisible(true);
        });
    }
}
