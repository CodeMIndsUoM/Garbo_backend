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



