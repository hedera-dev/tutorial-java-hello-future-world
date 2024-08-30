package hcstopic;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class ScriptHcsTopic {
    public static void main(String[] args) throws Exception {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.load();
        String accountIdStr = dotenv.get("ACCOUNT_ID");
        String privateKeyStr = dotenv.get("ACCOUNT_PRIVATE_KEY");
        if (accountIdStr == null || privateKeyStr == null) {
            throw new RuntimeException("Please set required keys in .env file.");
        }
        if (privateKeyStr.startsWith("0x")) {
            privateKeyStr = privateKeyStr.substring(2);
        }

        // Configure client using environment variables
        AccountId accountId = AccountId.fromString(accountIdStr);
        PrivateKey accountKey = PrivateKey.fromStringECDSA(privateKeyStr);
        Client client = Client.forTestnet().setOperator(accountId, accountKey);

        String accountExplorerUrl = "https://hashscan.io/testnet/address/" + accountId;

        // NOTE: Create a topic
        // Step (1) in the accompanying tutorial
        // TopicCreateTransaction topicCreateTx =
        //      /* ... */
        //         .freezeWith(client);
        TopicCreateTransaction topicCreateTx =
            new TopicCreateTransaction()
                .freezeWith(client);
        TopicCreateTransaction topicCreateTxSigned = topicCreateTx.sign(accountKey);
        TransactionResponse topicCreateTxResponse = topicCreateTxSigned.execute(client);
        TransactionReceipt topicCreateTxReceipt = topicCreateTxResponse
            .setValidateStatus(true)
            .getReceipt(client);
        TransactionId topicCreateTxId = topicCreateTxResponse.transactionId;
        TopicId topicId = topicCreateTxReceipt.topicId;

        String topicExplorerUrl = "https://hashscan.io/testnet/topic/" + topicId.toString();

        // Publish a message to this topic
        // NOTE: Publish message to topic
        // Step (2) in the accompanying tutorial
        // TopicMessageSubmitTransaction topicMsgSubmitTx =
        //     /* ... */
        //         .freezeWith(client);
        TopicMessageSubmitTransaction topicMsgSubmitTx =
            new TopicMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage("Hello HCS - bguiz")
                .freezeWith(client);
        TopicMessageSubmitTransaction topicMsgSubmitTxSigned = topicMsgSubmitTx.sign(accountKey);
        TransactionResponse topicMsgSubmitTxResponse = topicMsgSubmitTxSigned.execute(client);
        TransactionId topicMsgSubmitTxId = topicMsgSubmitTxResponse.transactionId;
        TransactionReceipt topicMsgSubmitTxReceipt = topicMsgSubmitTxResponse
            .setValidateStatus(true)
            .getReceipt(client);

        String topicMessageMirrorUrl =
            "https://testnet.mirrornode.hedera.com/api/v1/topics/" +
            topicId.toString() +
            "/messages/" +
            topicMsgSubmitTxReceipt.topicSequenceNumber.toString();

        client.close();

        // Output results
        System.out.println("accountId: " + accountId);
        System.out.println("topicId: " + topicId);
        System.out.println("topicExplorerUrl: " + topicExplorerUrl);
        System.out.println("topicCreateTxId: " + topicCreateTxId);
        System.out.println("topicMsgSubmitTxId: " + topicMsgSubmitTxId);
        System.out.println("topicMessageMirrorUrl: " + topicMessageMirrorUrl);
    }
}
