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

public class AsyncService {

    public static void updateClient(Response responseObj, String url) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            httppost.setEntity(new StringEntity(objectMapper.writeValueAsString(responseObj), ContentType.APPLICATION_JSON));
            int httpStatus;
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            if ((httpStatus >= 200) && (httpStatus < 300)) System.out.println("The connection with the external server was successful");
            else System.out.println("An error occurred when connecting with the external server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
