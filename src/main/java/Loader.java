import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.TimeZone;

public class Loader {
    public static void main(String[] args) {
        String query = "https://api.openweathermap.org/data/2.5/onecall?appid=738255fea3b89382dd1f35a06b50a4ee&lang=ru&lat=52.033329&lon=113.55000&exclude=minutely,hourly&units=metric";
        RestTemplate restTemplate = new RestTemplate();
        String jsonResult = restTemplate.getForObject(query, String.class);
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonResult);
            JSONArray daily = (JSONArray) jsonObject.get("daily");
            Iterator i = daily.iterator();
            double min = Double.MAX_VALUE;
            long timeMin = 0;
            while (i.hasNext()) {
                JSONObject innerObj = (JSONObject) i.next();
                long sunrise = (long) innerObj.get("sunrise") * 1000;
                LocalDate triggerTime = getDateFromLong(sunrise);
                System.out.println("Sunrise in " + triggerTime + " " + sunrise);
                JSONObject temp = (JSONObject)innerObj.get("temp");
                double nightTemp = Double.parseDouble(temp.get("night").toString());
                System.out.println(nightTemp);
                JSONObject feelsTemp = (JSONObject)innerObj.get("feels_like");
                double feelTemp = Double.parseDouble(feelsTemp.get("night").toString());
                double diff = Math.abs(feelTemp - nightTemp);
                if (min > diff) {
                    min = diff;
                    timeMin = (long) innerObj.get("dt");
                }
            }
            System.out.println(getDateFromLong(timeMin) + " " + min);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }

    private static LocalDate getDateFromLong(long dt) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dt), TimeZone.getDefault().toZoneId()).toLocalDate();
    }
}
