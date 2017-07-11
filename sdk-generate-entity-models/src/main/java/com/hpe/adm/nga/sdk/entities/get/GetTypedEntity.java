/*
 * Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.adm.nga.sdk.entities.get;

import com.hpe.adm.nga.sdk.entities.TypedEntityList;
import com.hpe.adm.nga.sdk.model.TypedEntityModel;
import com.hpe.adm.nga.sdk.network.OctaneHttpClient;
import com.hpe.adm.nga.sdk.network.OctaneRequest;

/**
 * The generic super class for the context of get for typed entities.
 *
 * @param <T> The type of the entity model
 * @param <E> The implementing subclass of this class
 * @param <F> The type of {@link com.hpe.adm.nga.sdk.entities.TypedEntityList.AvailableFields}
 * @see GetEntity for the non typed version
 */
public abstract class GetTypedEntity<T extends TypedEntityModel, E extends GetTypedEntity, F extends TypedEntityList.AvailableFields>
        extends TypedEntityList.TypedEntityRequest<T> {

    private final OctaneRequest octaneRequest;

    protected GetTypedEntity(final Class<T> typedEntityModelClass, final OctaneHttpClient octaneHttpClient, final String urlDomain, final int iEntityId) {
        super(typedEntityModelClass);
        octaneRequest = new OctaneRequest(octaneHttpClient, urlDomain, iEntityId);
    }

    /**
     * Carries out the execution and returns the entity
     *
     * @return The entity
     * @throws RuntimeException Some type of error
     */
    public final T execute() throws RuntimeException {
        return getEntityInstance(GetHelper.getInstance().getEntityModel(octaneRequest));
    }

    /**
     * Set Fields Parameters
     *
     * @param fields An array or comma separated list of fields to be retrieved
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final E addFields(final F... fields) {
        GetTypedHelper.addFields(octaneRequest, fields);
        return (E) this;
    }
}
