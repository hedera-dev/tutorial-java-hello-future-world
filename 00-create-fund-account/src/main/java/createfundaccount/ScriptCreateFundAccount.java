package createfundaccount;

import com.hedera.hashgraph.sdk.PrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.security.Security;
import java.text.NumberFormat;
import java.util.Locale;

public class ScriptCreateFundAccount {
    public static void main(String[] args) throws Exception {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.load();

        String seedPhrase = dotenv.get("SEED_PHRASE");
        if (seedPhrase == null || seedPhrase.isEmpty()) {
            throw new IllegalArgumentException("Please set required keys in .env file.");
        }

        // Create an ECSDA secp256k1 private key based on a BIP-39 seed phrase,
        // plus the default BIP-32/BIP-44 HD Wallet derivation path used by Metamask.
        Security.addProvider(new BouncyCastleProvider());

        // BIP-39 Generate seed from seed phrase
        byte[] seed = MnemonicUtils.generateSeed(seedPhrase, null);

        // BIP-32 Derive master key
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(seed);
        final int[] path = {
            44 | Bip32ECKeyPair.HARDENED_BIT,
            60 | Bip32ECKeyPair.HARDENED_BIT,
            0 | Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        };

        // BIP-44 Derive private key of single account from BIP-32 master key + BIP-44 HD path
        // Using m/44'/60'/0'/0/0 for compatibility with Metamask hardcoded default
        Bip32ECKeyPair derivedKeypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path);
        Credentials credentials = Credentials.create(derivedKeypair);
        String privateKeyString = credentials.getEcKeyPair().getPrivateKey().toString(16);
        final int privateKeyStringMissingCharCount = 64 - privateKeyString.length();
        if (privateKeyStringMissingCharCount > 0) {
            privateKeyString = ("0").repeat(privateKeyStringMissingCharCount) + privateKeyString;
        }
        PrivateKey privateKey = PrivateKey.fromStringECDSA(privateKeyString);

        // At this point the account technically does not yet exist,
        // and will need to be created when it receives its first transaction (later).
        // Convert the private key to string format as well as an EVM address.
        String privateKeyHex = "0x" + privateKey.toStringRaw();
        String evmAddress = credentials.getAddress();
        String accountExplorerUrl = "https://hashscan.io/testnet/account/" + evmAddress;
        String accountBalanceFetchApiUrl = "https://testnet.mirrornode.hedera.com/api/v1/balances?account.id=" + evmAddress + "&limit=1&order=asc";

        // Query account ID and balance using mirror node API
        Long accountBalanceTinybar = null;
        String accountBalanceHbar = null;
        String accountId = null;
        try {
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
            JSONArray balancesArray = jsonResponse.getJSONArray("balances");
            JSONObject balanceObject = balancesArray.getJSONObject(0);
            accountId = balanceObject.getString("account");
            accountBalanceTinybar = balanceObject.getLong("balance");
        } catch (Exception e) {
            // do nothing
        }

        if (accountBalanceTinybar != null) {
            accountBalanceHbar = NumberFormat
                .getNumberInstance(Locale.UK)
                .format(accountBalanceTinybar * Math.pow(10, -8));
        }

        // Output results
        System.out.println("privateKeyHex: " + privateKeyHex);
        System.out.println("evmAddress: " + evmAddress);
        System.out.println("accountExplorerUrl: " + accountExplorerUrl);
        System.out.println("accountId: " + accountId);
        System.out.println("accountBalanceHbar: " + accountBalanceHbar);
    }
}
