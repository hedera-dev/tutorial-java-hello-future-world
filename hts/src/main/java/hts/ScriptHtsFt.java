package hts;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.hedera.hashgraph.sdk.*;

import io.github.cdimascio.dotenv.Dotenv;

public class ScriptHtsFt {
    public static void main(String[] args) throws Exception {
        System.out.println("🏁 Hello Future World - HTS Fungible Token - start");

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

        // Create a HTS token
        System.out.println("🟣 Creating new HTS token");
        TokenCreateTransaction tokenCreateTx = new TokenCreateTransaction()
            //Set the transaction memo
            .setTransactionMemo("Hello Future World token - xyz")
            // HTS "TokenType.FungibleCommon" behaves similarly to ERC20
            .setTokenType(TokenType.FUNGIBLE_COMMON)
            // Configure token options: name, symbol, decimals, initial supply
            .setTokenName("htsFt coin")
            // Set the token symbol
            .setTokenSymbol("HTSFT")
            // Set the token decimals to 2
            .setDecimals(2)
            // Set the initial supply of the token to 1,000,000
            .setInitialSupply(1_000_000)
            // Configure token access permissions: treasury account, admin, freezing
            .setTreasuryAccountId(operatorId)
            // Set the freeze default value to false
            .setFreezeDefault(false)
            //Freeze the transaction and prepare for signing
            .freezeWith(client);

        // Get the transaction ID of the transaction. The SDK automatically generates and assigns a transaction ID when the transaction is created
        TransactionId tokenCreateTxId = tokenCreateTx.getTransactionId();
        System.out.println("The token create transaction ID: " + tokenCreateTxId.toString());

        // Sign the transaction with the private key of the treasury account (operator key)
        TokenCreateTransaction tokenCreateTxSigned = tokenCreateTx.sign(operatorKey);

        // Submit the transaction to the Hedera Testnet
        TransactionResponse tokenCreateTxSubmitted = tokenCreateTxSigned.execute(client);

        // Get the transaction receipt
        TransactionReceipt tokenCreateTxReceipt = tokenCreateTxSubmitted.getReceipt(client);

        // Get the token ID
        TokenId tokenId = tokenCreateTxReceipt.tokenId;
        System.out.println("Token ID: " + tokenId.toString());

        client.close();

        // Verify token using Mirror Node API
        // This is a manual step, the code below only outputs the URL to visit

        // View your token on HashScan
        System.out.println("View the token on HashScan");
        String tokenHashscanUrl = "https://hashscan.io/testnet/token/" + tokenId.toString();
        System.out.println(
            "Token Hashscan URL:\n" +
            tokenHashscanUrl
        );

        // Wait for 6s for record files (blocks) to propagate to mirror nodes
        Thread.sleep(6000);

        // Verify token using Mirror Node API
        System.out.println("🟣 Get token data from the Hedera Mirror Node");
        String tokenMirrorNodeApiUrl = "https://testnet.mirrornode.hedera.com/api/v1/tokens/" + tokenId.toString();
        System.out.println(
          "The token Hedera Mirror Node API URL:\n" +
          tokenMirrorNodeApiUrl
        );

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenMirrorNodeApiUrl))
                .build();
        final HttpClient httpClient = HttpClient.newHttpClient();
        final var mirrorNodeResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject jsonResponse = JsonParser.parseString(mirrorNodeResponse).getAsJsonObject();

        String tokenName = jsonResponse.get("name").getAsString();
        System.out.println("The name of this token: " + tokenName);
        long tokenTotalSupply = jsonResponse.get("total_supply").getAsLong();
        System.out.println("The total supply of this token: " + Long.toString(tokenTotalSupply));

        System.out.println("🎉 Hello Future World - HTS Fungible Token - complete");
    }
}
