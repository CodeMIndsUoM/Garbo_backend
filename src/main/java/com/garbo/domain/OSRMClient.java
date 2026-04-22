package com.garbo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class OSRMClient {

    private static final String BASE =
            "http://router.project-osrm.org/table/v1/driving/";

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    public static double[][] getDurationMatrix(double[][] coords) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < coords.length; i++) {

            double lat = coords[i][0];
            double lng = coords[i][1];

            sb.append(lng).append(",").append(lat);

            if (i < coords.length - 1) sb.append(";");
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl(BASE + sb)
                .build()
                .toUri();

        RestTemplate restTemplate = buildRestTemplate();

        OSRMResponse res = restTemplate.getForObject(uri, OSRMResponse.class);

        if (res == null || res.durations == null) {
            throw new RuntimeException("OSRM failed");
        }

        int n = coords.length;
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                matrix[i][j] =
                        res.durations[i][j] != null
                                ? res.durations[i][j]
                                : 999999;
            }
        }

        return matrix;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSRMResponse {
        public Double[][] durations;
        public String code;
    }
}




/* package com.garbo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

public class OSRMClient {

    // Use the public OSRM demo server — replace with your own instance in production
    private static final String OSRM_TABLE_URL = "http://router.project-osrm.org/table/v1/driving/";

    /**
     * Builds a duration matrix (in seconds) between all coordinates using OSRM Table API.
     * Per OSRM docs, durations[i][j] = travel time in seconds from waypoint i to waypoint j.
     *
     * @param coords double[n][2] where each row is [lat, lng]
     * @return duration matrix as double[n][n], or throws RuntimeException on failure
     */
    /* 
    public static double[][] getDurationMatrix(double[][] coords) {
        // OSRM expects coordinates as lng,lat (longitude first)
        StringBuilder coordinateStr = new StringBuilder();
        for (int i = 0; i < coords.length; i++) {
            double lat = coords[i][0];
            double lng = coords[i][1];
            coordinateStr.append(lng).append(",").append(lat);
            if (i < coords.length - 1) coordinateStr.append(";");
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl(OSRM_TABLE_URL + coordinateStr)
                // Default annotation is durations — explicitly request it per the docs
                .build()
                .toUri();

        System.out.println("[OSRM] Requesting table from: " + uri);

        RestTemplate restTemplate = new RestTemplate();
        OSRMTableResponse response = restTemplate.getForObject(uri, OSRMTableResponse.class);

        if (response == null) {
            throw new RuntimeException("OSRM returned a null response. Check the server URL and connectivity.");
        }

        if (!"Ok".equals(response.code)) {
            throw new RuntimeException("OSRM error: code=" + response.code + ", message=" + response.message);
        }

        if (response.durations == null || response.durations.length == 0) {
            throw new RuntimeException("OSRM returned empty durations matrix. Response code was: " + response.code);
        }

        int n = coords.length;
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // OSRM may return null for unreachable pairs — treat as very high cost
                matrix[i][j] = (response.durations[i][j] != null) ? response.durations[i][j] : 999999.0;
            }
        }

        return matrix;
    }

    // Maps the OSRM Table API JSON response
    // Per OSRM docs: { "code": "Ok", "durations": [[...], ...], "sources": [...], "destinations": [...] }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSRMTableResponse {
        public String code;
        public String message;
        public Double[][] durations;  // Double (boxed) to handle null values for unreachable pairs
    }
}
*/