import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    public Chart(String title)
    {
        super(title);
        setupUI();
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
                updateChart(prices);

                double rsi = Indicators.calculateRSI(prices);
                double ma = Indicators.calculateMA(prices);
                double ema = Indicators.calculateEMA(prices, days);
                double[] macd = Indicators.calculateMACD(prices);
                double atr = Indicators.calculateATR(prices);
                double stochastic = Indicators.calculateStochasticOscillator(prices);
                double roc = Indicators.calculateROC(prices);
                double obv = Indicators.calculateOBV(prices);

                SwingUtilities.invokeLater(() -> {
                    indicatorsLabel.setText(String.format(
                            "<html>" +
                                    "<div style='font-family: Arial, sans-serif; font-size: 14px;'>" +
                                    "<strong>RSI:</strong> %.2f<br>" +
                                    "MA: %.2f | EMA: %.2f<br>" +
                                    "<strong>MACD:</strong> %.2f (Signal: %.2f)<br>" +
                                    "<strong>ATR:</strong> %.2f<br>" +
                                    "<strong>Stochastic Oscillator:</strong> %.2f<br>" +
                                    "<strong>ROC:</strong> %.2f<br>" +
                                    "<strong>OBV:</strong> %.2f" +
                                    "</div>" +
                                    "</html>",
                            rsi, ma, ema, macd[0], macd[1], atr,
                            stochastic, roc, obv));
                });

                System.out.println("API Response");

                return null;
            }
        }.execute();
    }

    private void updateChart(JSONArray prices)
    {
        SwingUtilities.invokeLater(() -> {
            priceSeries.clear();
            for (int i = 0; i < prices.length(); i++)
            {
                JSONArray point = prices.getJSONArray(i);
                Second second = new Second(new java.util.Date(point.getLong(0)));
                priceSeries.addOrUpdate(second, point.getDouble(1));
            }
            chartPanel.repaint();
        });
    }

    private JSONArray fetchData(String currency, int days) throws Exception
    {
        if(Objects.equals(currency, "btc"))
        {
            currency = "bitcoin";
        }
        else if(Objects.equals(currency, "eth"))
        {
            currency = "ethereum";
        }
        else if(Objects.equals(currency, "xrp"))
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

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            Chart chart = new Chart("Cryptocurrency Price Tracker");
            chart.setVisible(true);
        });
    }
}