/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

import static java.nio.charset.StandardCharsets.UTF_8;


import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.ProposalPackage.SignedProposal;
import org.hyperledger.fabric.shim.Chaincode.Response;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.QueryResultsIteratorWithMetadata;

import org.hyperledger.fabric.protos.peer.ProposalPackage;


public class ABstore extends ChaincodeBase {

    private static Log _logger = LogFactory.getLog(ABstore.class);

    @Override
    public Response init(ChaincodeStub stub) {
        try {
            _logger.info("Init java simple chaincode");
            List<String> args = stub.getParameters();
            if (args.size() != 4) {
                newErrorResponse("Incorrect number of arguments. Expecting 4");
            }
            // Initialize the chaincode
            String account1Key = args.get(0);
            int account1Value = Integer.parseInt(args.get(1));
            String account2Key = args.get(2);
            int account2Value = Integer.parseInt(args.get(3));

            _logger.info(String.format("account %s, value = %s; account %s, value %s", account1Key, account1Value, account2Key, account2Value));
            stub.putStringState(account1Key, args.get(1));
            stub.putStringState(account2Key, args.get(3));

            return newSuccessResponse();
        } catch (Throwable e) {
            return newErrorResponse(e);
        }
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        try {
            _logger.info("Invoke java simple chaincode");
            String func = stub.getFunction();
            List<String> params = stub.getParameters();
            if (func.equals("invoke")) {
                return invoke(stub, params);
            }
            if (func.equals("delete")) {
                return delete(stub, params);
            }
            if (func.equals("query")) {
                return query(stub, params);
            }
            return newErrorResponse("Invalid invoke function name. Expecting one of: [\"invoke\", \"delete\", \"query\"]");
        } catch (Throwable e) {
            return newErrorResponse(e);
        }
    }

    private Response invoke(ChaincodeStub stub, List<String> args) {
        if (args.size() != 3) {
            return newErrorResponse("Incorrect number of arguments. Expecting 3");
        }
        String accountFromKey = args.get(0);
        String accountToKey = args.get(1);

        String accountFromValueStr = stub.getStringState(accountFromKey);
        if (accountFromValueStr == null) {
            return newErrorResponse(String.format("Entity %s not found", accountFromKey));
        }


        int accountFromValue = Integer.parseInt(accountFromValueStr);

        String accountToValueStr = stub.getStringState(accountToKey);
        if (accountToValueStr == null) {
            return newErrorResponse(String.format("Entity %s not found", accountToKey));
        }

        int accountToValue = Integer.parseInt(accountToValueStr);

        int amount = Integer.parseInt(args.get(2));

        if (amount > accountFromValue) {
            return newErrorResponse(String.format("not enough money in account %s", accountFromKey));
        }

        accountFromValue -= amount;
        accountToValue += amount;

        _logger.info(String.format("new value of A: %s", accountFromValue));
        _logger.info(String.format("new value of B: %s", accountToValue));

        stub.putStringState(accountFromKey, Integer.toString(accountFromValue));
        stub.putStringState(accountToKey, Integer.toString(accountToValue));

        _logger.info("Transfer complete");

        return newSuccessResponse("invoke finished successfully", ByteString.copyFrom(accountFromKey + ": " + accountFromValue + " " + accountToKey + ": " + accountToValue, UTF_8).toByteArray());
    }

    // Deletes an entity from state
    private Response delete(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting 1");
        }
        String key = args.get(0);
        // Delete the key from the state in ledger
        stub.delState(key);
        return newSuccessResponse();
    }

    // query callback representing the query of a chaincode
    private Response query(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting name of the person to query");
        }
        String key = args.get(0);
        //byte[] stateBytes
        String val	= stub.getStringState(key);
        if (val == null) {
            return newErrorResponse(String.format("Error: state for %s is null", key));
        }
        _logger.info(String.format("Query Response:\nName: %s, Amount: %s\n", key, val));
        return newSuccessResponse(val, ByteString.copyFrom(val, UTF_8).toByteArray());
    }

    public static void main(String[] args) {
        ABstore ab = new ABstore();
//        ab.start(args);
        ChaincodeStubImp stub = new ChaincodeStubImp();
//        ab.init(stub);
//        ab.invoke(stub);

        try {
            Scanner sc = new Scanner(new FileReader(args[0]));
            String s = sc.next();
            List<String> parameters = new ArrayList<>();
            parameters.add(s);
            ab.delete(stub, parameters);

            String s1 = sc.next();
            List<String> parameters1 = new ArrayList<>();
            parameters1.add(s1);
            ab.query(stub, parameters1);

            List<String> parameters2 = new ArrayList<>();
            while (sc.hasNext()) {
                String s2 = sc.next();
                parameters2.add(s2);
            }
            ab.invoke(stub, parameters2);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }

//            Scanner sc = new Scanner(new FileReader(args[8]));
//            List<String> pm1 = new ArrayList<>();
//            String s = sc.next();
//            pm.add(s);
//            ab.delete(stub, pm);
//
//            List<String> pm2 = new ArrayList<>();
//            String s = sc.next();
//            pm2.add(s);
//            ab.query(stub, pm2);

	System.out.println("Done.");

    }

}

class ChaincodeStubImp implements ChaincodeStub{
    public List<String> getParameters(){
//        Scanner sc = new Scanner(new FileReader(args[0]));
//        List<String> parameters = new ArrayList<>();
//        while(sc.hasNext()){
//            String s = sc.next();
//            parameters.add(s);
//        }
//        return parameters;
        return null;

//        FileInputStream stream = new FileInputStream(args[0]);
//        List<String> parameters = new ArrayList<>();
//        for(int i = 0;i < 4; i++){
//            int a = stream.read();
//            String s = Integer.toString(a);
//            parameters.add(s);
//        }
//        return parameters;
    }

    public void putStringState(final String key, final String value) {
        putState(key, value.getBytes(UTF_8));
    }

    public void putState(String key, byte[] value){
        System.out.println("putState Successfully!");
    }


    public String getFunction(){

//        Scanner sc = new Scanner(new FileReader(args[4]));
//        String s = sc.next();
//        return  s;

//        FileInputStream stream = new FileInputStream(args[4]);
//        int a = stream.read();
//        String s = Integer.toString(a);
//        return s;
        return null;
    }


    public void delState(String key){
        System.out.println("delState Successfully!");
    }


    public String getStringState(final String key) {
        return new String(getState(key), UTF_8);
    }


    public byte[] getState(String key){
        byte[] b = new byte[]{'2','1','4','7','4','8','0','6','4','7'};
        // byte[] b = new byte[]{'2','1','4','7','4','8','3','6','4','7','8','1','1'};
        // byte[] b = new byte[]{'-','2'};
        return b;
    }





    /*simple implement */
    public String getMspId(){
        String s = "MspId";
        return s;
    }
    public List<byte[]> getArgs(){
        List<byte[]> parameters = new ArrayList<>();
        return parameters;
    }


    public List<String> getStringArgs(){
        List<String> parameters = new ArrayList<>();
        parameters.add("aaa");
        return parameters;
    }


    public String getTxId(){
        String s = "TxId";
        return s;
    }


    public String getChannelId(){
        String s = "TxId";
        return s;
    }


    public Response invokeChaincode(String chaincodeName, List<byte[]> args, String channel){
        return null;
    }


    public byte[] getStateValidationParameter(String key){
        byte[] b = new byte[]{'S','t','a','t','e',' ','o','f',' ','k','e','y',' ','!'};
        return b;
    }


    public void setStateValidationParameter(String key, byte[] value){
        System.out.println("Successfully!");
    }


    public QueryResultsIterator<KeyValue> getStateByRange(String startKey, String endKey){
        return null;
    }


    public QueryResultsIteratorWithMetadata<KeyValue> getStateByRangeWithPagination(String startKey, String endKey, int pageSize, String bookmark){
        return null;
    }


    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String compositeKey){
        return null;
    }


    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String objectType, String... attributes){
        return null;
    }


    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(CompositeKey compositeKey){
        return null;
    }


    public QueryResultsIteratorWithMetadata<KeyValue> getStateByPartialCompositeKeyWithPagination(CompositeKey compositeKey, int pageSize, String bookmark){
        return null;
    }


    public CompositeKey createCompositeKey(String objectType, String... attributes){
        return null;
    }

    public CompositeKey splitCompositeKey(String compositeKey){
        return null;
    }


    public QueryResultsIterator<KeyValue> getQueryResult(String query){
        return null;
    }


    public QueryResultsIteratorWithMetadata<KeyValue> getQueryResultWithPagination(String query, int pageSize, String bookmark){
        return null;
    }


    public QueryResultsIterator<KeyModification> getHistoryForKey(String key){
        return null;
    }


    public byte[] getPrivateData(String collection, String key){
        byte[] b = new byte[]{'S','t','a','t','e',' ','o','f',' ','k','e','y',' ','!'};
        return b;
    }


    public byte[] getPrivateDataHash(String collection, String key){
        byte[] b = new byte[]{'S','t','a','t','e',' ','o','f',' ','k','e','y',' ','!'};
        return b;
    }


    public byte[] getPrivateDataValidationParameter(String collection, String key){
        byte[] b = new byte[]{'S','t','a','t','e',' ','o','f',' ','k','e','y',' ','!'};
        return b;
    }


    public void putPrivateData(String collection, String key, byte[] value){}


    public void setPrivateDataValidationParameter(String collection, String key, byte[] value){}


    public void delPrivateData(String collection, String key){}


    public QueryResultsIterator<KeyValue> getPrivateDataByRange(String collection, String startKey, String endKey){
        return null;
    }


    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String compositeKey){
        return null;
    }


    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, CompositeKey compositeKey){
        return null;
    }


    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String objectType, String... attributes){
        return null;
    }


    public QueryResultsIterator<KeyValue> getPrivateDataQueryResult(String collection, String query){
        return null;
    }


    public void setEvent(String name, byte[] payload){}


    public Response invokeChaincode(final String chaincodeName, final List<byte[]> args) {
        return invokeChaincode(chaincodeName, args, null);
    }


    public Response invokeChaincodeWithStringArgs(final String chaincodeName, final List<String> args, final String channel) {
        return invokeChaincode(chaincodeName, args.stream().map(x -> x.getBytes(UTF_8)).collect(toList()), channel);
    }


    public Response invokeChaincodeWithStringArgs(final String chaincodeName, final List<String> args) {
        return invokeChaincodeWithStringArgs(chaincodeName, args, null);
    }


    public Response invokeChaincodeWithStringArgs(final String chaincodeName, final String... args) {
        return invokeChaincodeWithStringArgs(chaincodeName, Arrays.asList(args), null);
    }


    public void putPrivateData(final String collection, final String key, final String value) {
        putPrivateData(collection, key, value.getBytes(UTF_8));
    }


    public String getPrivateDataUTF8(final String collection, final String key) {
        return new String(getPrivateData(collection, key), UTF_8);
    }


    public ChaincodeEvent getEvent(){
        return null;
    }


    public SignedProposal getSignedProposal(){
        return null;
    }


    public Instant getTxTimestamp(){
        return null;
    }


    public byte[] getCreator(){
        byte[] b = new byte[]{'S','t','a','t','e',' ','o','f',' ','k','e','y',' ','!'};
        return b;
    }


    public Map<String, byte[]> getTransient(){
        return null;
    }


    public byte[] getBinding(){
        byte[] b = new byte[]{'S','t','a','t','e',' ','o','f',' ','k','e','y',' ','!'};
        return b;
    }

}
