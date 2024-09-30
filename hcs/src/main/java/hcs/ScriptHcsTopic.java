package hcs;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.net.URI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.hedera.hashgraph.sdk.*;

import io.github.cdimascio.dotenv.Dotenv;

public class ScriptHcsTopic {
    public static void main(String[] args) throws Exception {
        System.out.println("🏁 Hello Future World - HCS Topic - start");

	    // Load environment variables from .env file
        Dotenv dotenv = Dotenv.configure().directory("../").load();
        String operatorIdStr = dotenv.get("OPERATOR_ACCOUNT_ID");
        String operatorKeyStr = dotenv.get("OPERATOR_ACCOUNT_PRIVATE_KEY");
        if (operatorIdStr == null || operatorKeyStr == null) {
            throw new RuntimeException("Must set OPERATOR_ACCOUNT_ID, OPERATOR_ACCOUNT_PRIVATE_KEY");
        }
        if (operatorKeyStr.startsWith("0x")) {
            operatorKeyStr = operatorKeyStr.substring(2);
        }

	    // Initialize the operator account
        AccountId operatorId = AccountId.fromString(operatorIdStr);
        PrivateKey operatorKey = PrivateKey.fromStringECDSA(operatorKeyStr);
        Client client = Client.forTestnet().setOperator(operatorId, operatorKey);
        System.out.println("Using account: " + operatorIdStr);

        //Set the default maximum transaction fee (in HBAR)
        client.setDefaultMaxTransactionFee(new Hbar(100));
        //Set the default maximum payment for queries (in HBAR)
        client.setDefaultMaxQueryPayment(new Hbar(50));

        // Create a Hedera Consensus Service (HCS) topic
        System.out.println("🟣 Creating new HCS topic");
        TopicCreateTransaction topicCreateTx = new TopicCreateTransaction()
            .setTopicMemo("Hello Future World topic - xyz")
            // Freeze the transaction to prepare for signing
            .freezeWith(client);

        // Get the transaction ID of the transaction.
        // The SDK automatically generates and assigns a transaction ID when the transaction is created
        TransactionId topicCreateTxId = topicCreateTx.getTransactionId();
        System.out.println("The topic create transaction ID: " + topicCreateTxId.toString());

        // Sign the transaction with the account key that will be paying for this transaction
        TopicCreateTransaction topicCreateTxSigned = topicCreateTx.sign(operatorKey);

        // Submit the transaction to the Hedera Testnet
        TransactionResponse topicCreateTxSubmitted = topicCreateTxSigned.execute(client);

        // Get the transaction receipt
        TransactionReceipt topicCreateTxReceipt = topicCreateTxSubmitted.getReceipt(client);

        // Get the topic ID
        TopicId topicId = topicCreateTxReceipt.topicId;
        System.out.println("Topic ID:" + topicId.toString());

        // Publish a message to the Hedera Consensus Service (HCS) topic
        System.out.println("🟣 Publish message to HCS topic");
        TopicMessageSubmitTransaction topicMsgSubmitTx = new TopicMessageSubmitTransaction()
            //Set the transaction memo with the hello future world ID
            .setTransactionMemo("Hello Future World topic message - xyz")
            .setTopicId(topicId)
            //Set the topic message contents
            .setMessage("Hello HCS!")
            // Freeze the transaction to prepare for signing
            .freezeWith(client);

        // Get the transaction ID of the transaction.
        // The SDK automatically generates and assigns a transaction ID when the transaction is created
        TransactionId topicMsgSubmitTxId = topicMsgSubmitTx.getTransactionId();
        System.out.println(
            "The topic message submit transaction ID: " +
            topicMsgSubmitTxId.toString()
        );

        // Sign the transaction with the account key that will be paying for this transaction
        TopicMessageSubmitTransaction topicMsgSubmitTxSigned = topicMsgSubmitTx.sign(operatorKey);

        // Submit the transaction to the Hedera Testnet
        TransactionResponse topicMsgSubmitTxSubmitted =
            topicMsgSubmitTxSigned.execute(client);

        // Get the transaction receipt
        TransactionReceipt topicMsgSubmitTxReceipt =
            topicMsgSubmitTxSubmitted.getReceipt(client);

        // Get the topic message sequence number
        Long topicMsgSeqNum = topicMsgSubmitTxReceipt.topicSequenceNumber;
        System.out.println("Topic Message Sequence Number:" + topicMsgSeqNum.toString());

        client.close();

        // Verify transaction using Hashscan
        // This is a manual step, the code below only outputs the URL to visit

        // View your topic on HashScan
        System.out.println("🟣 View the topic on HashScan");
        String topicHashscanUrl = "https://hashscan.io/testnet/topic/" + topicId.toString();
        System.out.println(
            "Topic Hashscan URL:\n" +
            topicHashscanUrl
        );

        // Wait for 6s for record files (blocks) to propagate to mirror nodes
        Thread.sleep(6000);

        // Verify topic using Mirror Node API
        System.out.println("Get topic data from the Hedera Mirror Node");
        String topicMirrorNodeApiUrl =
            "https://testnet.mirrornode.hedera.com/api/v1/topics/" + topicId.toString() + "/messages?encoding=base64&limit=5&order=asc&sequencenumber=1";
        System.out.println(
            "The topic Hedera Mirror Node API URL:\n" +
            topicMirrorNodeApiUrl
        );
        final HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(topicMirrorNodeApiUrl))
            .build();
        final HttpClient httpClient = HttpClient.newHttpClient();
        final var mirrorNodeResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject jsonResponse = JsonParser.parseString(mirrorNodeResponse).getAsJsonObject();

        // Extract messages from the JSON (null check equivalent)
        JsonArray topicVerifyMessages = jsonResponse.has("messages")
            ? jsonResponse.getAsJsonArray("messages")
            : new JsonArray();
        System.out.println("Number of messages retrieved from this topic: " + topicVerifyMessages.size());
        List<String> topicVerifyMessagesParsed = new ArrayList<>();
        for (JsonElement messageElement : topicVerifyMessages) {
            JsonObject msg = messageElement.getAsJsonObject();
            int seqNum = msg.has("sequence_number") ? msg.get("sequence_number").getAsInt() : -1;
            String message = msg.has("message") ? msg.get("message").getAsString() : "";
            String decodedMsg = new String(Base64.getDecoder().decode(message), StandardCharsets.UTF_8);
            topicVerifyMessagesParsed.add(String.format("#%d: %s", seqNum, decodedMsg));
        }
        System.out.println("Messages retrieved from this topic: " + topicVerifyMessagesParsed);

        System.out.println("🎉 Hello Future World - HCS Topic - complete");
    }
}
