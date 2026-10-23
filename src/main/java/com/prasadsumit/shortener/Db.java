package com.prasadsumit.shortener;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

final class Db {
    static final String TABLE = System.getenv("TABLE_NAME");

    // Region and credentials come from the Lambda environment automatically.
    static final DynamoDbClient CLIENT = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .build();

    private Db() {}
}
