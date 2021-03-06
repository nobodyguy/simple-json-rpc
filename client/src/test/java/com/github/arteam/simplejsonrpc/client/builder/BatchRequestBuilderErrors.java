package com.github.arteam.simplejsonrpc.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.github.arteam.simplejsonrpc.client.JsonRpcClient;
import com.github.arteam.simplejsonrpc.client.Transport;
import com.github.arteam.simplejsonrpc.client.builder.BatchRequestBuilder;
import com.github.arteam.simplejsonrpc.client.domain.Player;
import com.github.arteam.simplejsonrpc.client.exception.JsonRpcBatchException;
import com.github.arteam.simplejsonrpc.core.domain.ErrorMessage;
import org.hamcrest.core.StringStartsWith;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Date: 10/23/14
 * Time: 11:55 PM
 */
public class BatchRequestBuilderErrors {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    JsonRpcClient client = new JsonRpcClient(new Transport() {
        @NotNull
        @Override
        public String pass(@NotNull String request) throws IOException {
            System.out.println(request);
            return "[{\n" +
                    "    \"jsonrpc\": \"2.0\",\n" +
                    "    \"id\": 1,\n" +
                    "    \"result\": {\n" +
                    "        \"firstName\": \"Steven\",\n" +
                    "        \"lastName\": \"Stamkos\",\n" +
                    "        \"team\": {\n" +
                    "            \"name\": \"Tampa Bay Lightning\",\n" +
                    "            \"league\": \"NHL\"\n" +
                    "        },\n" +
                    "        \"number\": 91,\n" +
                    "        \"position\": \"C\",\n" +
                    "        \"birthDate\": \"1990-02-07T00:00:00.000+0000\",\n" +
                    "        \"capHit\": 7.5\n" +
                    "    }\n" +
                    "}]";
        }
    });

    @Test
    public void testRequestsAreEmpty() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Requests are not set");

        client.createBatchRequest().execute();
    }

    @Test
    public void testRequestWithoutReturnType() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Return type isn't specified for request with id='1'");

        client.createBatchRequest().add(1L, "findPlayer", "Steven", "Stamkos")
                .execute();
    }

    @Test
    public void testBothSingleAndGlobalResponseTypeAreSet() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Common and detailed configurations of return types shouldn't be mixed");

        client.createBatchRequest().add(1L, "findPlayer", new Object[]{"Steven", "Stamkos"}, Player.class)
                .returnType(Player.class)
                .execute();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBadId() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Wrong id=true");

        BatchRequestBuilder<?, ?> batchRequest = client.createBatchRequest();
        batchRequest.getRequests()
                .add(batchRequest.request(BooleanNode.TRUE, "findPlayer",
                        new ObjectMapper().createArrayNode().add("Steven").add("Stamkos")));
        batchRequest.returnType(Player.class).execute();
    }

    @Test
    public void tesKeyIdIsNotExpectedType() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Id: '1' has wrong type: 'Long'. Should be: 'String'");

        client.createBatchRequest().add(1L, "findPlayer", "Steven", "Stamkos")
                .returnType(Player.class)
                .keysType(String.class)
                .execute();
    }

    @Test
    public void testIOError() {
        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                throw new IOException("Network is down");
            }
        });

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("I/O error during a request processing");

        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .add(2L, "findPlayer", "Vladimir", "Sobotka")
                .keysType(Long.class)
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testFailFastOnNotJsonData() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(new StringStartsWith("No serializer found"));

        client.createBatchRequest()
                .add(1L, "findPlayer", new Name("Steven"), new Name("Stamkos"))
                .add(2L, "findPlayer", new Name("Vladimir"), new Name("Sobotka"))
                .keysType(Long.class)
                .returnType(Player.class)
                .execute();
    }

    private static class Name {
        private String value;

        private Name(String value) {
            this.value = value;
        }
    }

    @Test
    public void testNotArrayResponse() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Expected array but was OBJECT");

        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "{\"test\":\"data\"}";
            }
        });
        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .add(2L, "findPlayer", "Vladimir", "Sobotka")
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testNotJsonResponse() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(new StringStartsWith("Unable parse a JSON response"));

        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "test data";
            }
        });
        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .add(2L, "findPlayer", "Vladimir", "Sobotka")
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testNoVersion() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(new StringStartsWith("Not a JSON-RPC response"));

        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "[{\"test\":\"data\"}]";
            }
        });
        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .add(2L, "findPlayer", "Vladimir", "Sobotka")
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testBadVersion() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(new StringStartsWith("Bad protocol version"));

        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "[{\n" +
                        "    \"jsonrpc\": \"1.0\",\n" +
                        "    \"id\": 1,\n" +
                        "    \"result\": {\n" +
                        "        \"firstName\": \"Steven\",\n" +
                        "        \"lastName\": \"Stamkos\",\n" +
                        "        \"team\": {\n" +
                        "            \"name\": \"Tampa Bay Lightning\",\n" +
                        "            \"league\": \"NHL\"\n" +
                        "        },\n" +
                        "        \"number\": 91,\n" +
                        "        \"position\": \"C\",\n" +
                        "        \"birthDate\": \"1990-02-07T00:00:00.000+0000\",\n" +
                        "        \"capHit\": 7.5\n" +
                        "    }\n" +
                        "}]";
            }
        });
        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .add(2L, "findPlayer", "Vladimir", "Sobotka")
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testUnexpectedResult() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(new StringStartsWith("Neither result or error is set in response"));

        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "[{\n" +
                        "    \"jsonrpc\": \"2.0\",\n" +
                        "    \"id\": 1\n" +
                        "}]";
            }
        });
        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .add(2L, "findPlayer", "Vladimir", "Sobotka")
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testUnspecifiedId() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Unspecified id: '10' in response");

        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "[{\n" +
                        "    \"jsonrpc\": \"2.0\",\n" +
                        "    \"id\": 10,\n" +
                        "    \"result\": {\n" +
                        "        \"firstName\": \"Steven\",\n" +
                        "        \"lastName\": \"Stamkos\",\n" +
                        "        \"team\": {\n" +
                        "            \"name\": \"Tampa Bay Lightning\",\n" +
                        "            \"league\": \"NHL\"\n" +
                        "        },\n" +
                        "        \"number\": 91,\n" +
                        "        \"position\": \"C\",\n" +
                        "        \"birthDate\": \"1990-02-07T00:00:00.000+0000\",\n" +
                        "        \"capHit\": 7.5\n" +
                        "    }\n" +
                        "}]";
            }
        });
        client.createBatchRequest()
                .add(1L, "findPlayer", "Steven", "Stamkos")
                .returnType(Player.class)
                .execute();
    }

    @Test
    public void testJsonRpcError() {
        JsonRpcClient client = new JsonRpcClient(new Transport() {
            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                return "[{\n" +
                        "    \"jsonrpc\": \"2.0\",\n" +
                        "    \"id\": 1,\n" +
                        "    \"result\": {\n" +
                        "        \"firstName\": \"Steven\",\n" +
                        "        \"lastName\": \"Stamkos\",\n" +
                        "        \"team\": {\n" +
                        "            \"name\": \"Tampa Bay Lightning\",\n" +
                        "            \"league\": \"NHL\"\n" +
                        "        },\n" +
                        "        \"number\": 91,\n" +
                        "        \"position\": \"C\",\n" +
                        "        \"birthDate\": \"1990-02-07T00:00:00.000+0000\",\n" +
                        "        \"capHit\": 7.5\n" +
                        "    }\n" +
                        "}, " +
                        "{\"jsonrpc\":\"2.0\",\"id\":2, \"error\":{\"code\":-32603,\"message\":\"Internal error\"}}" +
                        "]";
            }
        });
        try {
            client.createBatchRequest()
                    .add(1L, "findPlayer", "Steven", "Stamkos")
                    .add(2L, "findPlayer", "Vladimir", "Sobotka")
                    .returnType(Player.class)
                    .keysType(Long.class)
                    .execute();
            Assert.fail();
        } catch (JsonRpcBatchException e) {
            Map<?, ErrorMessage> errors = e.getErrors();
            Map<?, ?> successes = e.getSuccesses();
            System.out.println(successes);
            System.out.println(errors);

            Object result = successes.get(1L);
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(Player.class);
            assertThat(((Player) result).getFirstName()).isEqualTo("Steven");
            assertThat(((Player) result).getLastName()).isEqualTo("Stamkos");

            assertThat(errors).isNotEmpty();
            ErrorMessage errorMessage = errors.get(2L);
            assertThat(errorMessage.getCode()).isEqualTo(-32603);
            assertThat(errorMessage.getMessage()).isEqualTo("Internal error");
        }
    }
}
