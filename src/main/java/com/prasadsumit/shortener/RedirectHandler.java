package com.prasadsumit.shortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.Map;

public class RedirectHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        Map<String, String> params = event.getPathParameters();
        String code = params == null ? null : params.get("shortCode");
        if (code == null || !code.matches("[0-9A-Za-z]{1,32}")) {
            return Responses.text(404, "Not found");
        }

        long now = Instant.now().getEpochSecond();
        try {
            UpdateItemResponse res = Db.CLIENT.updateItem(UpdateItemRequest.builder()
                    .tableName(Db.TABLE)
                    .key(Map.of("shortCode", AttributeValue.fromS(code)))
                    .updateExpression("ADD clickCount :one")
                    // Only succeed if the item exists and has not expired.
                    // (TTL deletion is lazy, so we must check expiry ourselves.)
                    .conditionExpression(
                            "attribute_exists(shortCode) AND (attribute_not_exists(expiresAt) OR expiresAt > :now)")
                    .expressionAttributeValues(Map.of(
                            ":one", AttributeValue.fromN("1"),
                            ":now", AttributeValue.fromN(Long.toString(now))))
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());

            return Responses.redirect(res.attributes().get("originalUrl").s());
        } catch (ConditionalCheckFailedException missingOrExpired) {
            return Responses.text(404, "Not found");
        }
    }
}
