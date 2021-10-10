import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Loader {
    public static void main(String[] args) {

        String jsonResult;
        try {
            jsonResult = getJSONString();
            printResult(parseResultForMinimalDiff(jsonResult), "Minimum difference (degrees):");
            printResult(parseResultForMaxDurationOfDay(jsonResult), "Maximum duration (ms):");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }


    }

    private static LocalDate getDateFromLong(long date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date), TimeZone.getDefault().toZoneId()).toLocalDate();
    }

    private static double getTempForParam(JSONObject jsonObject, String param) {
        return Double.parseDouble(jsonObject.get(param).toString());
    }

    private static void printResult(HashMap<Long, Double> resultMap, String annotation) {
        resultMap.forEach((key, value) ->
                System.out.println(String.format("%s %.2f ", annotation, value) +
                        String.format("%1$td.%1$tm.%1$ty ", getDateFromLong(key))));
    }


    private static String getJSONString() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        String url = "https://api.openweathermap.org/data/2.5/onecall";
        String apiKey = "{API_KEY}";
        String lat = "52.033329";
        String lon = "113.55000";
        String exclude = "minutely,hourly";
        String units = "metric";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("appid", apiKey)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("exclude", exclude)
                .queryParam("units", units);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);
        return response.getBody();
    }

    private static HashMap<Long, Double> parseResultForMinimalDiff(String jsonResult) {
        HashMap<Long, Double> resultMap = new HashMap<>();
        try {
            JSONArray daily = getDailyResult(jsonResult);
            Iterator i = daily.iterator();
            double min = Double.MAX_VALUE;
            long dateMin = 0;
            while (i.hasNext()) {
                JSONObject innerObj = (JSONObject) i.next();
                JSONObject temp = (JSONObject) innerObj.get("temp");
                double nightTemp = getTempForParam(temp, "night");
                JSONObject feelsTemp = (JSONObject) innerObj.get("feels_like");
                double feelTemp = getTempForParam(feelsTemp, "night");
                double diff = Math.abs(feelTemp - nightTemp);

                if (!(min < diff)) {
                    dateMin = (long) innerObj.get("dt") * 1000;
                    if (min > diff) {
                        resultMap.clear();
                    }
                    resultMap.put(dateMin, diff);
                    min = diff;
                }
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return resultMap;
    }

    private static HashMap<Long, Double> parseResultForMaxDurationOfDay(String jsonResult) {
        HashMap<Long, Double> resultMap = new HashMap<>();
        try {
            JSONArray daily = getDailyResult(jsonResult);
            Iterator i = daily.iterator();
            double max = 0;
            long dateMin = 0;
            while (i.hasNext()) {
                JSONObject innerObj = (JSONObject) i.next();
                long sunrise = (long) innerObj.get("sunrise") * 1000;
                long sunset = (long) innerObj.get("sunset") * 1000;
                double duration = sunset - sunrise;
                if (!(duration < max)) {
                    if (duration > max) {
                        resultMap.clear();
                    }
                    dateMin = sunrise;
                    max = duration;
                    resultMap.put(dateMin, duration);
                }
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return resultMap;
    }

    private static JSONArray getDailyResult (String jsonResult) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonResult);
        return (JSONArray) jsonObject.get("daily");
    }
}
