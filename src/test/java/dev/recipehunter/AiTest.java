package dev.recipehunter;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import com.google.gson.*;
final class AiTest {
 static void run(){
  for(AiClient.Provider provider:AiClient.Provider.values()){
   List<HttpRequest> requests=new ArrayList<>();AiClient ai=new AiClient(request->{requests.add(request);return CompletableFuture.completedFuture(response(request,200,"{\"choices\":[{\"message\":{\"content\":\"Try mining. dummy-test-key\"}}]}"));});
   ai.provider=provider;ai.model="test-model";ai.key("dummy-test-key");ai.request("1 Refined Mithril","What next?");
   PlannerTest.check(!ai.busy&&ai.answer.contains("Try mining.")&&!ai.answer.contains("dummy-test-key"),"successful reply redacts credentials "+provider);
   PlannerTest.check(requests.getFirst().uri().toString().equals(provider.url),"correct provider endpoint "+provider);
   PlannerTest.check(requests.getFirst().headers().firstValue("Authorization").orElse("").equals("Bearer dummy-test-key"),"authentication header "+provider);
   String body=body(requests.getFirst());PlannerTest.check(!body.contains("dummy-test-key"),"key excluded from request body "+provider);
   JsonObject payload=JsonParser.parseString(body).getAsJsonObject();PlannerTest.check(payload.has(provider==AiClient.Provider.OPENAI?"max_completion_tokens":"max_tokens"),"provider token parameter "+provider);
   ai.request("Updated goal context","And after that?");PlannerTest.eq(JsonParser.parseString(body(requests.getLast())).getAsJsonObject().getAsJsonArray("messages").size(),5,"chat retains previous turn "+provider);
   ai.clearChat();PlannerTest.check(ai.answer.equals("Chat cleared."),"clear conversation "+provider);
   ai.testConnection();PlannerTest.check(requests.getLast().method().equals("GET")&&requests.getLast().uri().getPath().endsWith("/models"),"non-generation connection test "+provider);
  }
  for(int status:new int[]{401,403,429,500}){AiClient ai=new AiClient(r->CompletableFuture.completedFuture(response(r,status,"secret response")));ai.model="test";ai.key("dummy");ai.request("context");PlannerTest.check(!ai.busy&&ai.answer.contains("HTTP "+status)&&!ai.answer.contains("secret response"),"provider error sanitized "+status);}
  AiClient malformed=new AiClient(r->CompletableFuture.completedFuture(response(r,200,"{}")));malformed.model="test";malformed.key("dummy");malformed.request("context");PlannerTest.check(!malformed.busy&&malformed.answer.contains("Unsupported"),"malformed response handled");
  AiClient failure=new AiClient(r->CompletableFuture.failedFuture(new RuntimeException("secret")));failure.model="test";failure.key("dummy");failure.request("context");PlannerTest.check(!failure.busy&&failure.answer.contains("Connection failed")&&!failure.answer.contains("secret"),"transport error sanitized");
 }
 static String body(HttpRequest request){StringBuilder data=new StringBuilder();request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){data.append(java.nio.charset.StandardCharsets.UTF_8.decode(b));}public void onError(Throwable t){throw new AssertionError(t);}public void onComplete(){}});return data.toString();}
 static HttpResponse<String> response(HttpRequest request,int code,String text){return new HttpResponse<>(){
  public int statusCode(){return code;}public HttpRequest request(){return request;}public Optional<HttpResponse<String>> previousResponse(){return Optional.empty();}public HttpHeaders headers(){return HttpHeaders.of(Map.of(),(a,b)->true);}public String body(){return text;}public Optional<javax.net.ssl.SSLSession> sslSession(){return Optional.empty();}public URI uri(){return request.uri();}public HttpClient.Version version(){return HttpClient.Version.HTTP_1_1;}
 };}
}
