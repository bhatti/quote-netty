package util;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class Metrics {
   private static final Map<String, Metrics>METRICS = new HashMap<String, Metrics>();
   private static final ThreadLocal<NumberFormat> FMT = new ThreadLocal<NumberFormat>() {
      @Override public NumberFormat initialValue() {
         return new DecimalFormat("#0.000");
      }
   };

   private static final String format(double d) {
      return FMT.get().format(d);
   }

   public class MetricResultSummary {
      public final long count;
      public final double averageResponseTime;
      public final double averageSize;

      public MetricResultSummary(final long count) {
         this.averageResponseTime = totalTime / messagesReceived;
         this.averageSize = totalPayloadSize / messagesReceived;
         this.count = count;
      }
      @Override
      public String toString() {
         return "COUNT" + this.count + ',' + format(this.averageResponseTime) + ',' + format(this.averageSize) + ',' + messagesReceived;
      }
   }
   private static final ObjectMapper jsonMapper = new ObjectMapper();
   private static final JsonFactory jsonFactory = jsonMapper.getJsonFactory();

   private long totalTime;
   private long totalPayloadSize;
   private long messagesReceived;


   public Metrics() {
      this.totalTime = 0;
      this.totalPayloadSize = 0;
      this.messagesReceived = 0;
   }

   public void addTo(Metrics target) {
      target.totalTime += this.totalTime;
      target.totalPayloadSize += this.totalPayloadSize;
      target.messagesReceived += this.messagesReceived;
   }

   public void update(long sendTime, long payloadSize) {
      long elapsed = new Date().getTime() - sendTime;
      this.messagesReceived++;
      this.totalTime += elapsed;
      this.totalPayloadSize += payloadSize;
   }
   //
   public double getAverageResponseTime() {
      return this.totalTime / this.messagesReceived;
   }


   public MetricResultSummary getSummary(long count) {
      return new MetricResultSummary(count);
   }


   @Override
   public String toString() {
      return new MetricResultSummary(1).toString();
   }
   
   public static Metrics getOrCreate(String key) {
      Metrics metric = METRICS.get(key);
      if (metric == null) {
         metric = new Metrics();
         METRICS.put(key, metric);
      }
      return metric;
   }

   public static long update(String jsonText) {
      try {
      JsonParser jp = jsonFactory.createJsonParser(jsonText);
      JsonNode actualObj = jsonMapper.readTree(jp);
      update(actualObj.get("request").getTextValue(), actualObj.get("timestamp").getLongValue(), jsonText.length());
      return actualObj.get("request").getLongValue();
   } catch (IOException e) {
      throw new RuntimeException(e);
   }
   }

   public static void update(String key, long sendTime, long payloadSize) {
      getOrCreate(key).update(sendTime, payloadSize);
   }

   public static MetricResultSummary summaryFor(String key, long count) {
      return getOrCreate(key).getSummary(count);
   }

   public static String getHeading() {
      return "clients, average response (ms), average size, messages";
   }

   public static MetricResultSummary getFinalSummary() {
      Metrics result = new Metrics();
      for (String key : METRICS.keySet()) {
         Metrics metric = METRICS.get(key);
         metric.addTo(result);
      }
      MetricResultSummary summary = result.getSummary(METRICS.size());
      return summary;
   }
}
