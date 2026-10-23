package com.prasadsumit.shortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CreateHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_URL_LENGTH = 2048;
    private static final long MAX_EXPIRY_HOURS = 8760; // one year

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            return Responses.text(400, "Request body is required");
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            return Responses.text(400, "Body must be valid JSON");
        }

        String originalUrl = json.path("originalUrl").asText(null);
        if (!isValidUrl(originalUrl)) {
            return Responses.text(400, "originalUrl must be a valid http(s) URL of at most 2048 characters");
        }

        long now = Instant.now().getEpochSecond();
        Long expiresAt = null;
        if (json.hasNonNull("expiryHours")) {
            long hours = json.get("expiryHours").asLong(-1);
            if (hours < 1 || hours > MAX_EXPIRY_HOURS) {
                return Responses.text(400, "expiryHours must be between 1 and " + MAX_EXPIRY_HOURS);
            }
            expiresAt = now + hours * 3600;
        }

        // The conditional write guarantees we never overwrite an existing code.
        // On the (very unlikely) collision, just try another code.
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = CodeGenerator.next();

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("shortCode", AttributeValue.fromS(code));
            item.put("originalUrl", AttributeValue.fromS(originalUrl));
            item.put("createdAt", AttributeValue.fromN(Long.toString(now)));
            item.put("clickCount", AttributeValue.fromN("0"));
            if (expiresAt != null) {
                item.put("expiresAt", AttributeValue.fromN(Long.toString(expiresAt)));
            }

            try {
                Db.CLIENT.putItem(PutItemRequest.builder()
                        .tableName(Db.TABLE)
                        .item(item)
                        .conditionExpression("attribute_not_exists(shortCode)")
                        .build());
                return Responses.text(200, code);
            } catch (ConditionalCheckFailedException collision) {
                // loop and retry with a new code
            }
        }
        return Responses.text(500, "Could not generate a unique short code");
    }

    private static boolean isValidUrl(String value) {
        if (value == null || value.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            return httpScheme && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
