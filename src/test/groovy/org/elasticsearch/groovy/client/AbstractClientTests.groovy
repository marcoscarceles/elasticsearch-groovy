/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.groovy.client

import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.Client
import org.elasticsearch.client.Requests
import org.elasticsearch.groovy.common.xcontent.GXContentBuilder
import org.elasticsearch.test.ElasticsearchIntegrationTest

import org.junit.Before

/**
 * {@code AbstractClientTests} provides helper functionality to tests that make use of {@code Client}s to perform
 * actions.
 */
abstract class AbstractClientTests extends ElasticsearchIntegrationTest {
    /**
     * Test {@link Client} used to process requests.
     */
    Client client

    @Before
    void setupClient() {
        client = client()
    }

    /**
     * Temporary helper function used to convert {@code closure}s into byte arrays for inner-closure settings.
     * <p />
     * Once {@code ActionRequest} extensions are added, this method should become unnecessary.
     * @param closure The closure to convert
     * @return Never {@code null}.
     */
    byte[] toBytes(Closure closure) {
        new GXContentBuilder().buildAsBytes(closure)
    }

    /**
     * Temporary helper function used to convert {@code closure}s into byte arrays for inner-closure settings.
     * <p />
     * Once {@code ActionRequest} extensions are added, this method should become unnecessary.
     * @param closure The closure to convert
     * @return Never {@code null}.
     */
    byte[] toIndexBytes(Closure closure) {
        new GXContentBuilder().buildAsBytes(closure, Requests.INDEX_CONTENT_TYPE)
    }

    /**
     * Index the {@code doc} in the {@code indexName} and {@code typeName} with a randomly generated ID that is
     * returned.
     *
     * @param indexName The name of the index.
     * @param typeName The name of the type.
     * @param doc The indexed source.
     * @return The randomly generated ID used to index the {@code doc}. Never {@code null}.
     */
    String indexDoc(String indexName, String typeName, Closure doc) {
        String docId = randomInt()

        IndexResponse indexResponse = client.index {
            index indexName
            type typeName
            id docId
            source toIndexBytes(doc)
        }.actionGet()

        // sanity check
        assert indexResponse.id == docId

        docId
    }
}
