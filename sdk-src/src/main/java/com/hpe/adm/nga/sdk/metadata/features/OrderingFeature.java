/*
 * Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.adm.nga.sdk.metadata.features;

/**
 *
 * Created by ngthien on 8/3/2016.
 */
public class OrderingFeature extends Feature {

    private Aspect[] aspects;

    /**
     * get aspects
     * @return the ordering aspects
     */
    public Aspect[] getAspects() { return aspects; }

    public class Aspect {
        private String aspect_name;
        private String order_field_name;
        private String context_field_name;

        public String getAspectName() { return aspect_name; }
        public String getOrderFieldName() { return order_field_name; }
        public String getContextFieldName() { return context_field_name; }
    }
}
