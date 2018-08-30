package com.malt.mongopostgresqlstreamer.model;

import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class FlattenMongoDocument {
    private Map<String, Object> values;

    public static FlattenMongoDocument fromMap(Map map) {
        return fromDocument(new Document(map));
    }


    public static FlattenMongoDocument fromDocument(Document document) {
        FlattenMongoDocument flattenMongoDocument = new FlattenMongoDocument();
        flattenMongoDocument.setValues(
                filters(
                        new JsonFlattener(document.toJson())
                                .withFlattenMode(FlattenMode.KEEP_ARRAYS)
                                .flattenAsMap()
                )
        );
        addCreationDateIfPossible(flattenMongoDocument);

        return flattenMongoDocument;
    }

    private static Map<String, Object> filters(Map<String, Object> flattenAsMap) {
        Map<String, Object> filteredMap = new HashMap<>();
        flattenAsMap.forEach((k, v) -> {
            if (k.contains(".$oid")) {
                k = k.replace(".$oid", "");
            } else if (k.contains(".$date")) {
                k = k.replace(".$date", "");
                v = new Date(((Number) v).longValue());
            }
            filteredMap.put(k.replace(".$oid", ""), v);
        });

        return filteredMap;
    }

    private static void addCreationDateIfPossible(FlattenMongoDocument flattenMongoDocument) {
        flattenMongoDocument.get("_id").ifPresent( id -> {
            if (id instanceof ObjectId) {
                Date creationDate = ((ObjectId) id).getDate();
                flattenMongoDocument.getValues().put("_creationdate", creationDate);
            } else if (id instanceof String && ObjectId.isValid((String) id)) {
                Date creationDate = ((new ObjectId((String) id))).getDate();
                flattenMongoDocument.getValues().put("_creationdate", creationDate);
            }
        });
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(values.getOrDefault(key, null));
    }

    public FlattenMongoDocument withField(String key, Object value) {
        this.values.put(key, value);
        return this;
    }
}
