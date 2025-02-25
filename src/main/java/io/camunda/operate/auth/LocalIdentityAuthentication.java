package io.camunda.operate.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.util.JsonUtils;

public class LocalIdentityAuthentication extends JwtAuthentication {

    private String clientId;
    private String clientSecret;
    private String baseUrl = "http://localhost:18080";
    private String keycloakRealm = "camunda-platform";

    public LocalIdentityAuthentication() {
    }
    
    public LocalIdentityAuthentication(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    public LocalIdentityAuthentication clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }
    public LocalIdentityAuthentication clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }
    public LocalIdentityAuthentication baseUrl(String url) {
        this.baseUrl = url;
        return this;
    }
    public LocalIdentityAuthentication keycloakRealm(String keycloakRealm) {
        this.keycloakRealm = keycloakRealm;
        return this;
    }
    
    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
    
    private String getConnectionString() throws UnsupportedEncodingException{
        return "grant_type=client_credentials&client_id="+encode(clientId)+"&client_secret="+encode(clientSecret);
    }
    
    @Override
    public void authenticate(CamundaOperateClient client) throws OperateException {
        try {
            URL url = new URL(this.baseUrl+"/auth/realms/"+keycloakRealm+"/protocol/openid-connect/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setConnectTimeout(1000 * 5);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String data = getConnectionString();
            
            conn.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JsonNode responseBody = JsonUtils.toJsonNode(response.toString());
                    String token = responseBody.get("access_token").asText();
                    setToken(client, token);
                }
            } else {
                throw new OperateException("Error "+conn.getResponseCode()+" obtaining access token : "+conn.getResponseMessage());
            }
        } catch (IOException e) {
            throw new OperateException(e);
        }
    }
}
