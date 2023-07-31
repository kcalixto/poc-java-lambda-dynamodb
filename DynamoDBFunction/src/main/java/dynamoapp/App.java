package dynamoapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        String httpMethod = input.getHttpMethod();
        String output;
        int statusCode = 200;

        switch (httpMethod) {
            case "GET":
                output = getUsers();
                break;
            case "POST":
                output = createUser(input.getBody());
                break;
            case "PUT":
                output = updateUser(input.getBody());
                break;
            case "DELETE":
                output = deleteUser(input.getBody());
                break;
            default:
                output = "Unsupported HTTP method " + httpMethod;
                statusCode = 400;
                break;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(headers);

        response.setStatusCode(statusCode);
        response.setBody(output);

        return response;
    }

    private String getUsers() {
        List<User> users = dynamoDBMapper.scan(User.class, new DynamoDBScanExpression());
        return new Gson().toJson(users);
    }

    private String createUser(String body) {
        User user = new Gson().fromJson(body, User.class);
        dynamoDBMapper.save(user);

        return new Gson().toJson(user);
    }

    private String updateUser(String body) {
        User updatedUser = new Gson().fromJson(body, User.class);
        User existingUser = dynamoDBMapper.load(User.class, updatedUser.getId());
        if (existingUser != null) {
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());

            dynamoDBMapper.save(existingUser);
            return "User updated: " + existingUser.getId();
        } else {
            return "User not found";
        }
    }

    private String deleteUser(String body) {
        User user = new Gson().fromJson(body, User.class);
        dynamoDBMapper.delete(user);

        return "User deleted";
    }

}
