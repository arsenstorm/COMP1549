# COMP1549 Group Chat

A Java-based networked distributed system for group-based client-server communication.

## Building the Project

The project uses Maven for dependency management. To build the project:

```bash
mvn clean package
```

You can also use `npm run build` to build the project.

## Running the App

### Starting the Server

```bash
java -cp target/groupchat-1.0-SNAPSHOT.jar com.comp1549.groupchat.server.GroupServer [port]
```

You can also use `npm run start` to run the server.

Default port is 8080 if not specified.

### Starting a Client

```bash
java -cp target/groupchat-1.0-SNAPSHOT.jar com.comp1549.groupchat.client.GroupClient <client-id> [server-host] [server-port]
```

Example:
```bash
java -cp target/groupchat-1.0-SNAPSHOT.jar com.comp1549.groupchat.client.GroupClient user1 localhost 8080
```

You can also use `npm run client <client-id> <server-host> <server-port>` to start a client.

Server host and port are optional, and default to `localhost` and `8080` respectively.

## Client Commands

Once connected, the following commands are available:

1. `broadcast <message>` - Send a message to everyone
2. `private <user_id> <message>` - Send a private message
3. `quit` - Leave

## Testing

The project includes JUnit tests for core components. Run the tests with:

```bash
mvn test
```

You can also use `npm run test` to run the tests.

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/
│           └── comp1549/
│               └── groupchat/
│                   ├── client/
│                   │   └── GroupClient.java
│                   ├── server/
│                   │   └── GroupServer.java
│                   └── model/
│                       ├── Member.java
│                       └── Message.java
└── test/
    └── java/
        └── com/
            └── comp1549/
                └── groupchat/
                    └── model/
                        ├── MemberTest.java
                        └── MessageTest.java
``` 