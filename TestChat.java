import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestChat {
    public static void main(String[] args) throws Exception {
        // Test 1: Simple greeting
        testChat("test_greeting", "你好");

        Thread.sleep(2000);
        
        // Test 2: Query systems - should trigger tool call
        testChat("test_systems", "查一下现在有哪些绩效体系");
    }
    
    private static void testChat(String sessionId, String message) throws Exception {
        String requestBody = String.format("""
            {
                "sessionId": "%s",
                "message": "%s"
            }
            """, sessionId, message);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/agent/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        System.out.println("\n=== Testing: " + message + " ===");
        long start = System.currentTimeMillis();
        
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;
            
            System.out.println("Status: " + response.statusCode());
            System.out.println("Response time: " + duration + "ms");
            String body = response.body();
            // Truncate long responses
            if (body.length() > 500) {
                body = body.substring(0, 500) + "...";
            }
            System.out.println("Response: " + body);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
