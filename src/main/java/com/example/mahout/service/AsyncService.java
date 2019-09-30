package com.example.mahout.service;

import com.example.mahout.entity.Response;
import com.example.mahout.entity.ResultId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class AsyncService {

    private static Random random = new Random();

    public AsyncService() throws NoSuchAlgorithmException {

    }

    public static void updateClient(Response responseObj, String url) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            httppost.setEntity(new StringEntity(objectMapper.writeValueAsString(responseObj), ContentType.APPLICATION_JSON));
            HttpResponse response = httpclient.execute(httppost);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ResultId getId() {
        return new ResultId(System.currentTimeMillis() + "_" + random.nextInt(1000));
    }

}
