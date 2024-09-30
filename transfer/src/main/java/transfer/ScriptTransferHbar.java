package transfer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.net.URI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.hedera.hashgraph.sdk.*;

import io.github.cdimascio.dotenv.Dotenv;

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

        System.out.println("🟣 Creating, signing, and submitting the transfer transaction");
        AccountId recipientAccount1 = AccountId.fromString("0.0.200");
        AccountId recipientAccount2 = AccountId.fromString("0.0.201");
        TransferTransaction transferTx = new TransferTransaction()
		    .setTransactionMemo("Hello Future World transfer - xyz")
            // Debit  3 HBAR from the operator account (sender)
            .addHbarTransfer(operatorId, Hbar.from(-3, HbarUnit.HBAR))
            // Credit 1 HBAR to account 0.0.200 (1st recipient)
            .addHbarTransfer(recipientAccount1, Hbar.from(1, HbarUnit.HBAR))
            // Credit 2 HBAR to account 0.0.201 (2nd recipient)
            .addHbarTransfer(recipientAccount2, Hbar.from(2, HbarUnit.HBAR))
            // Freeze the transaction to prepare for signing
            .freezeWith(client);

        // Get the transaction ID for the transfer transaction
        TransactionId transferTxId = transferTx.getTransactionId();
        System.out.println("The transfer transaction ID: " + transferTxId.toString());

        // Sign the transaction with the account that is being debited (operator account) and the transaction fee payer account (operator account)
        // Since the account that is being debited and the account that is paying for the transaction are the same, only one account's signature is required
        TransferTransaction transferTxSigned = transferTx.sign(operatorKey);

        // Submit the transaction to the Hedera Testnet
        TransactionResponse transferTxSubmitted = transferTxSigned.execute(client);

        // Get the transfer transaction receipt
        TransactionReceipt transferTxReceipt = transferTxSubmitted.getReceipt(client);
        Status transactionStatus = transferTxReceipt.status;
        System.out.println(
            "The transfer transaction status is: " +
            transactionStatus.toString()
        );

        // Query HBAR balance using AccountBalanceQuery
        AccountBalance newAccountBalance = new AccountBalanceQuery()
            .setAccountId(operatorId)
            .execute(client);
        Hbar newHbarBalance = newAccountBalance.hbars;
        System.out.println(
            "The new account balance after the transfer: " +
            newHbarBalance.toString()
        );

        client.close();

        // View the transaction in HashScan
        System.out.println(
          "🟣 View the transfer transaction transaction in HashScan"
        );
        String transferTxVerifyHashscanUrl =
            "https://hashscan.io/testnet/transaction/" + transferTxId;
        System.out.println(
          "Transaction Hashscan URL:\n" +
          transferTxVerifyHashscanUrl
        );

        // Verify transfer transaction using Mirror Node API
        System.out.println("🟣 Get transfer transaction data from the Hedera Mirror Node");
        Thread.sleep(6_000);

        String transferTxIdMirrorNodeFormat =
          convertTransactionIdForMirrorNodeApi(transferTxId.toString());
        String transferTxVerifyMirrorNodeApiUrl =
            "https://testnet.mirrornode.hedera.com/api/v1/transactions/" + transferTxIdMirrorNodeFormat + "?nonce=0";

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(transferTxVerifyMirrorNodeApiUrl))
                .build();
        final HttpClient httpClient = HttpClient.newHttpClient();
        final var mirrorNodeResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
        JsonObject jsonResponse = JsonParser.parseString(mirrorNodeResponse).getAsJsonObject();
        JsonArray accountTransfers = jsonResponse
                .getAsJsonArray("transactions")
                .get(0)
                .getAsJsonObject()
                .getAsJsonArray("transfers");
        List<JsonObject> filteredAndSortedTransfers = new ArrayList<>();
        for (JsonElement element : accountTransfers) {
            // Discard all entries whose values are less than 0.1 hbars
            // as these are network fees
            JsonObject obj = element.getAsJsonObject();
            long amount = obj.get("amount").getAsLong();
            if (Math.abs(amount) >= 100_000_00) {
                filteredAndSortedTransfers.add(obj);
            }
        }
        filteredAndSortedTransfers.sort(
            Comparator.comparingLong(obj -> obj.get("amount").getAsLong())
        );

        System.out.printf("%-15s %-15s%n", "AccountID", "Amount");
        System.out.println("-".repeat(30));
        for (JsonObject entry : filteredAndSortedTransfers) {
            String account = entry.get("account").getAsString();
            String amount = Hbar.from(entry.get("amount").getAsLong(), HbarUnit.TINYBAR).toString(HbarUnit.HBAR);
            System.out.printf("%-15s %-15s%n", account, amount);
        }

        System.out.println("🎉 Hello Future World - Transfer HBAR - complete");
    }

    public static String convertTransactionIdForMirrorNodeApi(String txId) {
        Objects.requireNonNull(txId, "txId cannot be null");
        String[] parts = txId.split("@", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid txId format: " + txId);
        }
        String txIdB = parts[1].replace('.', '-');
        return String.format("%s-%s", parts[0], txIdB);
    }
}
