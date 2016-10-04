package com.hpe.adm.nga.sdk.utils;

import com.hpe.adm.nga.sdk.Query;

import java.util.List;

/**
 * Created by Guy Guetta on 21/04/2016.
 */
public class QueryUtils {
    public static Query getQueryForIds(List<Integer> entityIds) {
        Query query = new Query();
        for (int i = 0; i < entityIds.size(); i++) {
            Query.Field.Logical logical = query.field("id").equalTo(entityIds.get(i));
            if (i < entityIds.size() - 1) {
                query = logical.or();
            } else {
                query = logical.build();
            }
        }
        return query;
    }
}
