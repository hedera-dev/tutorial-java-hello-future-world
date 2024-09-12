package htsft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class ScriptHtsFt {
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
        // Create the token
        TokenCreateTransaction tokenCreateTx = new TokenCreateTransaction()
            // NOTE: Configure HTS token to be created
            // Step (1) in the accompanying tutorial
            // .setTokenType(/* ... */)
            // .setTokenName(/* ... */)
            // .setTokenSymbol(/* ... */)
            // .setDecimals(/* ... */)
            // .setInitialSupply(/* ... */)
            .setTokenType(TokenType.FUNGIBLE_COMMON)
            .setTokenName("bguiz coin")
            .setTokenSymbol("BGZ")
            .setDecimals(2)
            .setInitialSupply(1_000_000)
            .setTreasuryAccountId(accountId)
            .setAdminKey(accountKey)
            .setFreezeDefault(false)
            .freezeWith(client);
        TokenCreateTransaction tokenCreateTxSigned = tokenCreateTx.sign(accountKey);
        TransactionResponse tokenCreateTxSubmitted = tokenCreateTxSigned.execute(client);
        TransactionReceipt tokenCreateTxReceipt = tokenCreateTxSubmitted
            .setValidateStatus(true)
            .getReceipt(client);
        TokenId tokenId = tokenCreateTxReceipt.tokenId;
        String tokenExplorerUrl = "https://hashscan.io/testnet/token/" + tokenId;

        client.close();

        // Wait for 3 seconds for the record files to be ingested by the mirror nodes
        Thread.sleep(3000);

        // Query token balance of account (mirror node)
        // NOTE: Mirror Node API to query specified token balance
        // Step (2) in the accompanying tutorial
        // const accountBalanceFetchApiUrl =
        //     /* ... */;
        String accountBalanceFetchApiUrl =
            "https://testnet.mirrornode.hedera.com/api/v1/accounts/" +
            accountId.toString() +
            "/tokens?token.id=" +
            tokenId.toString() +
            "&limit=1&order=desc";
        URL url = URI.create(accountBalanceFetchApiUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode < 200 || responseCode >= 400) {
            throw new RuntimeException("HTTP response code: " + responseCode);
        }
        String inline = "";
        Scanner scanner = new Scanner(url.openStream());
        while (scanner.hasNext()) {
            inline += scanner.nextLine();
        }
        scanner.close();
        JSONObject jsonResponse = new JSONObject(inline);
        JSONArray tokensArray = jsonResponse.getJSONArray("tokens");
        long accountBalanceToken = tokensArray.getJSONObject(0).getLong("balance");

        // Output results
        System.out.println("accountId: " + accountId);
        System.out.println("tokenId: " + tokenId);
        System.out.println("tokenExplorerUrl: " + tokenExplorerUrl);
        System.out.println("accountTokenBalance: " + accountBalanceToken);
        System.out.println("accountBalanceFetchApiUrl: " + accountBalanceFetchApiUrl);
    }
}
