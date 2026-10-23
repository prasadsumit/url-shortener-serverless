package com.prasadsumit.shortener;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.util.Map;

final class Responses {
    private Responses() {}

    static APIGatewayV2HTTPResponse text(int status, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(Map.of(
                        "Content-Type", "text/plain; charset=utf-8",
                        "Cache-Control", "no-store"))
                .withBody(body)
                .build();
    }

    static APIGatewayV2HTTPResponse redirect(String location) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(302)
                .withHeaders(Map.of(
                        "Location", location,
                        "Cache-Control", "no-store"))
                .withBody("")
                .build();
    }
}
