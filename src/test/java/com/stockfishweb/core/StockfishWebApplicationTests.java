package com.stockfishweb.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockfishweb.core.engine.enums.Query;
import com.stockfishweb.core.engine.enums.QueryType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.stockfishweb.common.Util.START_FEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ExtendWith(SpringExtension.class)
//@ContextConfiguration
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StockfishWebApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(StockfishWebApplicationTests.class);
    @LocalServerPort
    private int port;

    @Autowired
    private Environment env;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SfController controller;


//	@Configuration
//	static class Config {
//		@Bean
//		FinalizerInTests myComponent() {
//			return new FinalizerInTests();
//		}
//	}


    @Test
    void contextLoads() {
        assertThat(controller).isNotNull();
    }

    @Test
    void greetingShouldReturnDefaultMessage() throws Exception {
        String ret = this.restTemplate.getForObject("http://localhost:" + port + "/", String.class);
        log.info(ret);
        assertThat(ret).isEqualTo("{\"error\":\"POST must be used instead of GET\"}");
    }


    @Test
    void hittingLandingPage() throws Exception {
        URL obj = new URL("http://localhost:" + port);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
//		con.setRequestProperty("Content-Type", "text/html");
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
                response.append(System.lineSeparator());
            }
            in.close();

            String responseAsString = response.toString().trim();
            assertThat(responseAsString).startsWith("<!DOCTYPE html>");
            assertThat(responseAsString).endsWith("</html>");
            assertThat(responseAsString).contains(env.getProperty("spring.profiles.include"));
        } else {
            fail("GET request failed with " + responseCode);
        }
    }

	@RepeatedTest(10)
    void postBestMoveEval() throws Exception {

        Query query = new Query(QueryType.Best_Move, START_FEN);
        query.setDepth(15);
        ResponseEntity<String> entity =  postQueryForEntity(query);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(entity.getBody());
            System.out.printf(jsonObject.toString());
        }catch (JSONException err){
            log.info("Error", err.toString());
            fail("failed to convert to object: " + jsonObject);
        }

        assertThat(jsonObject).isNotNull();
        if (entity.getStatusCode() == HttpStatusCode.valueOf(200)){
            assertTrue(jsonObject.has("eval"));
            assertTrue(jsonObject.has("bestMove"));
        } else if (entity.getStatusCode() == HttpStatusCode.valueOf(429)) {
            assertTrue(jsonObject.has("error"));
        } else {
            fail("Unexpected HTTP status: " + entity.getStatusCode());
        }
    }


    @Test
    void postBestMoveEvalEmptyFen() throws Exception {
        Thread.sleep(1000);
        Query query = new Query(QueryType.Best_Move, "");
        query.setDepth(15);

        ResponseEntity<String> b = postQueryForEntity(query);

        assertThat(b).isNotNull();
        assertThat(b.getStatusCode().value()).isEqualTo(409);
        assertThat(b.getBody()).contains("FEN is empty");
    }

    @Test
    void postBestMoveEvalKingsCloseToEachOther() throws Exception {
        Thread.sleep(1000);
        Query query = new Query(QueryType.Best_Move, "Kk6/8/8/8/8/8/8/8 w - - 0 1");
        query.setDepth(15);

        ResponseEntity<String> b = postQueryForEntity(query);

        assertThat(b).isNotNull();
        assertThat(b.getStatusCode().value()).isEqualTo(409);
        assertThat(b.getBody()).contains("Kings are too close to each other");
    }


    @Test
    void requestLimitExeeded() throws Exception {
        Query query = new Query(QueryType.Best_Move, "Kk6/8/8/8/8/8/8/8 w - - 0 1");
        query.setDepth(1);

        int maxRequestsPerSecond = Integer.valueOf(env.getProperty("max.requests.per.second"));
        ResponseEntity<String> b;
        for (int i = 0; i < maxRequestsPerSecond; i++) {
            b = postQueryForEntity(query);
            assertThat(b.getStatusCode().value() == 200);
        }
        b = postQueryForEntity(query);
        assertThat(b.getStatusCode().value()).isEqualTo(429);
        assertThat(b.getBody()).contains("Too many requests");
    }


    private ResponseEntity<String> postQueryForEntity(Query query) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        String postBodyJson = null;

        postBodyJson = mapper.writeValueAsString(query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(postBodyJson, headers);

        ResponseEntity<String> b = restTemplate.postForEntity("http://localhost:" + port,
                entity, String.class);//"[a-h][1-8][a-h][1-8]");

        return b;
    }
}
