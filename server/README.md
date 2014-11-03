## JSON-RPC 2.0 service

Annotate you service class with `@JsonRpcService`, `@JsonRpcMethod` and `@JsonRpcParam` annotations.

* `@JsonRpcService` marks a class a JSON-RPC service.

* `@JsonRpcMethod` marks a method as eligible for calling from the web.

* `@JsonRpcParam` is a mandatory annotation for the method parameter and should contain parameter name (this is forced requirement because Java compiler doesn't retain information about parameter names in a class file and therefore this information is not available in runtime).

Additional annotations:

* `@JsonRpcOptional` is used for marking method parameter as an optional, so the caller is able to ignore it when invokes the method.
* `@JsonRpcError` is used for marking an exception as a JSON-RPC error.

```java
@JsonRpcService
public class TeamService {

    private List<Player> players = Lists.newArrayList();

    @JsonRpcMethod
    public boolean add(@JsonRpcParam("player") Player s) throws TeamServiceException {
        if (players.size() > 5) throw new TeamServiceException();
        return players.add(s);
    }

    @JsonRpcMethod("find_by_birth_year")
    public List<Player> findByBirthYear(@JsonRpcParam("birth_year") 
                                        final int birthYear) {
        return ImmutableList.copyOf(Iterables.filter(players, new Predicate<Player>() {
            @Override
            public boolean apply(Player player) {
                int year = new DateTime(player.getBirthDate()).getYear();
                return year == birthYear;
            }
        }));
    }

    @JsonRpcMethod
    public Player findByInitials(@JsonRpcParam("firstName") final String firstName,
                                 @JsonRpcParam("lastName") final String lastName) {
        return Iterables.tryFind(players, new Predicate<Player>() {
            @Override
            public boolean apply(Player player) {
                return player.getFirstName().equals(firstName) &&
                        player.getLastName().equals(lastName);
            }
        }).orNull();
    }

    @JsonRpcMethod
    public List<Player> find(@JsonRpcOptional @JsonRpcParam("position") final Position position,
                             @JsonRpcOptional @JsonRpcParam("number") final int number) {
        return Lists.newArrayList(Iterables.filter(players, new Predicate<Player>() {
            @Override
            public boolean apply(Player player) {
                if (position != null && !player.getPosition().equals(position)) 
                    return false;
                if (number != 0 && player.getNumber() != number) 
                    return false;
                return true;
            }
        }));
    }

    @JsonRpcMethod
    public List<Player> getPlayers() {
        return players;
    }
}    

@JsonRpcError(code = -32032, message = "It's not permitted to add new players")
public class TeamServiceException extends Exception {
}
```

Invoke the service through *JsonRpcServer*

```java
TeamService teamService = new TeamService();
JsonRpcServer rpcServer = new JsonRpcServer();
String textRequest = "{\n" +
                    "    \"jsonrpc\": \"2.0\",\n" +
                    "    \"method\": \"findByInitials\",\n" +
                    "    \"params\": {\n" +
                    "        \"firstName\": \"Kevin\",\n" +
                    "        \"lastName\": \"Shattenkirk\"\n" +
                    "    },\n" +
                    "    \"id\": \"92739\"\n" +
                    "}";
String response = rpcServer.handle(request, teamService);
```

See the full service [code](https://github.com/arteam/simple-json-rpc/blob/master/server/src/test/java/com/github/arteam/simplejsonrpc/server/simple/service/TeamService.java)
and more examples in [tests](https://github.com/arteam/simple-json-rpc/blob/master/server/src/test/java/com/github/arteam/simplejsonrpc/server/simple).

## Setup
Maven:
```xml
<dependency>
   <groupId>com.github.arteam</groupId>
   <artifactId>simple-json-rpc-server</artifactId>
   <version>0.3</version>
</dependency>
```
Artifacts are available in [jCenter](https://bintray.com/bintray/jcenter) repository.

## Requirements

JDK 1.6 and higher

## Dependencies

* [Jackson](https://github.com/FasterXML/jackson) 2.4.1
* [Guava](http://code.google.com/p/guava-libraries/) 17.0
* [SLF4J](http://www.slf4j.org/) 1.7.6
* [IntelliJ IDEA Annotations](http://mvnrepository.com/artifact/com.intellij/annotations/12.0) 12.0