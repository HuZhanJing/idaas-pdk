package io.tapdata.mongodb.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import io.tapdata.kit.EmptyKit;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class MongodbConfig implements Serializable {
    private String collection;
    private String uri;

    public static MongodbConfig load(String jsonFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        return mapper.readValue(new File(jsonFile), MongodbConfig.class);
    }

    public static MongodbConfig load(Map<String, Object> map) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue((new ObjectMapper()).writeValueAsString(map), MongodbConfig.class);
    }

    public String getUri() {
        return uri;
    }

    public String getDatabase() {
        if (EmptyKit.isNotEmpty(uri)) {
            ConnectionString connectionString = new ConnectionString(uri);
            return connectionString.getDatabase();
        }
        return null;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

}
