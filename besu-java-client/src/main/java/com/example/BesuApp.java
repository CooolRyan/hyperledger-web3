package com.example;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class BesuApp {
    public static void main(String[] args) throws Exception {
        Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:8545"));
        System.out.println("Connected to Besu! Client version: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        String privateKey = "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
        Credentials credentials = Credentials.create(privateKey);
        System.out.println("Using account: " + credentials.getAddress());

        String bytecode = new String(Files.readAllBytes(Paths.get("build/src_main_solidity_SimpleERC20_sol_SimpleERC20.bin"))).trim();
        String constructorArgs = "00000000000000000000000000000000000000000000000000000000000003e8";
        String deployData = bytecode + constructorArgs;

        System.out.println("Deploying SimpleERC20 by sending raw transaction...");
        
        BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().getTransactionCount();
        RawTransaction rawTransaction = RawTransaction.createContractTransaction(
                nonce,
                BigInteger.ZERO,
                BigInteger.valueOf(3000000),
                BigInteger.ZERO,
                deployData
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        String txHash = ethSendTransaction.getTransactionHash();
        System.out.println("Transaction hash: " + txHash);

        if (ethSendTransaction.getError() != null) {
            System.err.println("Error: " + ethSendTransaction.getError().getMessage());
            System.exit(1);
        }

        Optional<TransactionReceipt> receipt = Optional.empty();
        for (int i = 0; i < 20; i++) {
            Thread.sleep(2000);
            receipt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (receipt.isPresent()) {
                break;
            }
        }

        if (receipt.isPresent()) {
            System.out.println("Contract deployed at address: " + receipt.get().getContractAddress());
            System.out.println("Done!");
        } else {
            System.out.println("Transaction receipt not found after waiting.");
        }
        System.exit(0);
    }
}
