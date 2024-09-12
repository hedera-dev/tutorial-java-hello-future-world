package transfer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class ScriptTransferHbar {
    public static void main(String[] args) throws Exception {
        System.out.println("🏁 Hello Future World - Transfer HBAR - start");

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

        System.out.println("🟣 ");
        // TODO ... SDK methods

        // Verify token using Mirror Node API
        System.out.println("🟣 View the ____ on HashScan");
        String ____HashscanUrl =
            "https://hashscan.io/testnet/____/" + "____.String()";
        System.out.println("____ Hashscan URL: " + ____HashscanUrl);

        client.close();

        // Wait for 6s for record files (blocks) to propagate to mirror nodes
        Thread.sleep(6000);

        // Verify ____ using Mirror Node API
        System.out.println("🟣 Get ______ data from the Hedera Mirror Node");
        String ____MirrorNodeApiUrl =
            "https://testnet.mirrornode.hedera.com/api/v1/____/" +
            "____.toString()";

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(____MirrorNodeApiUrl))
                .build();
        final HttpClient httpClient = HttpClient.newHttpClient();
        final var mirrorNodeResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject jsonResponse = JsonParser.parseString(mirrorNodeResponse).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        // String tokenName = jsonResponse.get("name").getAsString();
        // System.out.println("The name of this token: " + tokenName);
        // long tokenTotalSupply = jsonResponse.get("total_supply").getAsLong();
        // System.out.println("The total supply of this token: " + Long.toString(tokenTotalSupply));

        System.out.println("🎉 Hello Future World - Transfer HBAR - complete");
    }
}
