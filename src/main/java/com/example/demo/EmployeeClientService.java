package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
 
@Service
public class EmployeeClientService {
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String apiUrl;
 
    public EmployeeClientService(
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${employee.api.url}") String apiUrl) {
        this.authorizedClientManager = authorizedClientManager;
        this.apiUrl = apiUrl;
    }
 
    // Existing method to get all employees
    public String getEmployees() {
        String accessToken = getAccessToken();
        HttpHeaders headers = createHeaders(accessToken);
        return new RestTemplate()
                .exchange(
                    apiUrl + "/employees",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
                )
                .getBody();
    }
 
    // New method to get employee by ID
    public String getEmployeeById(String id) {
        String accessToken = getAccessToken();
        HttpHeaders headers = createHeaders(accessToken);
        return new RestTemplate()
                .exchange(
                    apiUrl + "/employees/" + id,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
                )
                .getBody();
    }
 
    // Extracted common token acquisition logic
    private String getAccessToken() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("okta")
                .principal("EmployeeClient")
                .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
        return authorizedClient.getAccessToken().getTokenValue();
    }
 
    // Extracted common headers creation
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
