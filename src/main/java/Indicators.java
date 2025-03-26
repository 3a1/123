import org.json.JSONArray;

public class Indicators
{
    public static double calculateRSI(JSONArray prices)
    {
        int period = 14; // Typical RSI period is 14
        double gain = 0, loss = 0;

        // Start loop from index prices.length() - period to only consider last "period" prices
        for (int i = prices.length() - period; i < prices.length(); i++) {
            double currentPrice = prices.getJSONArray(i).getDouble(1);
            double previousPrice = prices.getJSONArray(i - 1).getDouble(1);
            double change = currentPrice - previousPrice;

            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        // Calculate average gain and loss
        gain /= period;
        loss /= period;

        // Avoid division by zero
        if (loss == 0) {
            return 100.0;
        }

        double rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }

    public static double calculateMA(JSONArray prices)
    {
        double sum = 0;
        for (int i = 0; i < prices.length(); i++) {
            sum += prices.getJSONArray(i).getDouble(1);
        }
        return sum / prices.length();
    }

    public static double calculateEMA(JSONArray prices, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = prices.getJSONArray(0).getDouble(1); // Initial EMA is the first price
        for (int i = 1; i < prices.length(); i++) {
            double price = prices.getJSONArray(i).getDouble(1);
            ema = (price - ema) * multiplier + ema;
        }
        return ema;
    }


    public static double[] calculateMACD(JSONArray prices) {
        double ema12 = calculateEMA(prices, 12); // 12-period EMA
        double ema26 = calculateEMA(prices, 26); // 26-period EMA
        double macd = ema12 - ema26; // MACD line
        double signal = calculateEMA(prices, 9); // 9-period EMA for signal line
        return new double[]{macd, signal};
    }

    public static double calculateATR(JSONArray prices) {
        double atr = 0;

        // Ensure there are enough entries
        if (prices.length() < 2) {
            return 0;  // Not enough data to calculate ATR
        }

        double previousClose = prices.getJSONArray(0).getDouble(1); // Initialize previous close as the first price's close

        for (int i = 1; i < prices.length(); i++) {
            // Each entry has timestamp and close price
            double close = prices.getJSONArray(i).getDouble(1); // Close price

            // If you only have the close prices, you can calculate the True Range (TR) using the absolute difference
            double tr = Math.abs(close - previousClose);

            atr += tr; // Add to the ATR sum

            previousClose = close; // Update the previous close
        }

        return atr / (prices.length() - 1); // Average True Range
    }

    public static double calculateStochasticOscillator(JSONArray prices)
    {
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        for (int i = 0; i < prices.length(); i++) {
            double price = prices.getJSONArray(i).getDouble(1);
            highestHigh = Math.max(highestHigh, price);
            lowestLow = Math.min(lowestLow, price);
        }
        double latestClose = prices.getJSONArray(prices.length() - 1).getDouble(1);
        return 100 * ((latestClose - lowestLow) / (highestHigh - lowestLow));
    }

    public static double calculateROC(JSONArray prices)
    {
        double startPrice = prices.getJSONArray(0).getDouble(1);
        double endPrice = prices.getJSONArray(prices.length() - 1).getDouble(1);
        return ((endPrice - startPrice) / startPrice) * 100;
    }

    public static double calculateOBV(JSONArray prices)
    {
        double obv = 0;
        for (int i = 1; i < prices.length(); i++) {
            double currentPrice = prices.getJSONArray(i).getDouble(1);
            double previousPrice = prices.getJSONArray(i - 1).getDouble(1);
            if (currentPrice > previousPrice) {
                obv += 1; // Placeholder, replace with volume if available
            } else if (currentPrice < previousPrice) {
                obv -= 1; // Placeholder
            }
        }
        return obv;
    }
}