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

import org.elasticsearch.action.ListenableActionFuture
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.count.CountRequest
import org.elasticsearch.action.count.CountResponse
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse
import org.elasticsearch.action.explain.ExplainRequest
import org.elasticsearch.action.explain.ExplainResponse
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.get.MultiGetRequest
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.indexedscripts.delete.DeleteIndexedScriptRequest
import org.elasticsearch.action.indexedscripts.delete.DeleteIndexedScriptResponse
import org.elasticsearch.action.indexedscripts.get.GetIndexedScriptRequest
import org.elasticsearch.action.indexedscripts.get.GetIndexedScriptResponse
import org.elasticsearch.action.indexedscripts.put.PutIndexedScriptRequest
import org.elasticsearch.action.indexedscripts.put.PutIndexedScriptResponse
import org.elasticsearch.action.mlt.MoreLikeThisRequest
import org.elasticsearch.action.percolate.MultiPercolateRequest
import org.elasticsearch.action.percolate.MultiPercolateResponse
import org.elasticsearch.action.percolate.PercolateRequest
import org.elasticsearch.action.percolate.PercolateResponse
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.ClearScrollResponse
import org.elasticsearch.action.search.MultiSearchRequest
import org.elasticsearch.action.search.MultiSearchResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.action.suggest.SuggestRequest
import org.elasticsearch.action.suggest.SuggestResponse
import org.elasticsearch.action.termvectors.MultiTermVectorsRequest
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse
import org.elasticsearch.action.termvectors.TermVectorsRequest
import org.elasticsearch.action.termvectors.TermVectorsResponse
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.AdminClient
import org.elasticsearch.client.Client
import org.elasticsearch.client.Requests
import org.elasticsearch.common.settings.Settings

/**
 * {@code ClientExtensions} provides extensions to the Elasticsearch {@link Client} to enable Groovy-friendly
 * requests.
 * <p />
 * This enables support for using {@link Closure}s to configure (and execute) the various action requests. For example:
 * <pre>
 * ListenableActionFuture&lt;IndexResponse&gt; indexResponse = client.index {
 *     index "index-name"
 *     type "type-name"
 *     id "id-value"
 *     source {
 *         name "kimchy"
 *         timestamp = new Date()
 *         nested {
 *             other = 1.23
 *             data {
 *                 count = 1234
 *                 values = ["abc", "def"]
 *             }
 *         }
 *     }
 * }
 * </pre>
 * The above code would create an {@link IndexRequest}, call {@link IndexRequest#index(String) index("index-name")},
 * {@link IndexRequest#type(String) type("type-name")}, {@link IndexRequest#id(String) id("id-value")}, and {@link
 * IndexRequest#source source(Closure)}.
 * <p />
 * Note: All requests made by the {@code ClientExtensions} methods are asynchronous and they are invoked immediately.
 * To block until a response is returned, then call {@link ListenableActionFuture#actionGet()} or one of its overloads
 * that allow you to provide a timeout.
 */
class ClientExtensions extends AbstractClientExtensions {
    /**
     * Get the admin client that can be used to perform administrative operations.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @return Always {@link Client#admin()}.
     * @throws NullPointerException if {@code self} is {@code null}
     */
    static AdminClient getAdmin(Client self) {
        self.admin()
    }

    /**
     * Get the client {@link Settings}.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @return Always {@link Client#settings()}.
     * @throws NullPointerException if {@code self} is {@code null}
     */
    static Settings getSettings(Client self) {
        self.settings()
    }

    // REQUEST/RESPONSE

    /**
     * Index a document associated with a given index and type, then get the future result.
     * <p/>
     * The id is optional. If it is not provided, one will be generated automatically.
     * <pre>
     * IndexResponse response = client.index {
     *   index "my-index"
     *   type "my-type"
     *   // optional ID
     *   id "my-id"
     *   source {
     *     user = "kimchy"
     *     postedDate = new Date()
     *     nested {
     *       object {
     *         field = 123
     *       }
     *     }
     *   }
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link IndexRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    static ListenableActionFuture<IndexResponse> index(Client self, Closure requestClosure) {
        doRequest(self, Requests.indexRequest(), requestClosure, self.&index)
    }

    /**
     * Executes a bulk of index, update, or delete operations.
     * <p />
     * An example usage of the Bulk API would be to bulk index your own data, which may make sense to wrap for your own
     * convenience:
     * <pre>
     * BulkResponse bulkIndex(String indexName,
     *                        String typeName,
     *                        List&lt;Closure&gt; sources) {
     *     client.bulk {
     *         // Note: This creates a List&lt;IndexRequest&gt;
     *         add sources.collect {
     *             Requests.indexRequest(indexName).type(typeName).source(it)
     *         }
     *     }.actionGet()
     * }
     * </pre>
     * Such a method could then be used to build {@code List}s of {@code Closure}s to more clearly bulk index.
     * <pre>
     * // Index three documents
     * BulkResponse response = bulkIndex("my-index", "my-type", [
     *     { user = "kimchy" },
     *     { user = "pickypg" },
     *     { user = "dadoonet" }
     * ])
     * </pre>
     * You could build the {@code List} dynamically in a more realistic example:
     * <pre>
     * Closure convertMyObject(MyObject value) {
     *     // return is used explicitly so that the compiler knows this is
     *     //  not an arbitrary code block
     *     return {
     *         user = value.username
     *     }
     * }
     *
     * void indexDocuments(List&lt;MyObject&gt; objects) {
     *     // objects.collect(this.&convertMyObject) returns a List with each item the 1:1 result of calling
     *     //   convertMyObject(objects[i])
     *     bulkIndex("my-index", "my-type", objects.collect(this.&convertMyObject))
     * }
     * </pre>
     * If you wanted to mix-and-match indexing, updating, and deletions, then this approach would have to be modified,
     * but for the common use case of only adding new documents, then this should simplify a lot of bulk insertions. If
     * you wanted to mix-and-match different indices or types, then a variation of this could be created using the
     * Groovy-supplied {@code with} method at the expense of complicating each {@code Closure}.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link BulkRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    static ListenableActionFuture<BulkResponse> bulk(Client self, Closure requestClosure) {
        doRequest(self, new BulkRequest(), requestClosure, self.&bulk)
    }

    /**
     * Updates a document based on a script or given source.
     * <p />
     * For an unscripted example, you could simply replace fields (all or partially) in an existing document:
     * <pre>
     * UpdateResponse response = client.update {
     *     index "my-index"
     *     type "my-type"
     *     id "my-id"
     *     // Add/replace document fields
     *     doc {
     *         new_field = 456.7
     *     }
     * }.actionGet()
     * </pre>
     * For a scripted example, you might do something like:
     * <pre>
     * UpdateResponse response = client.update {
     *     index "my-index"
     *     type "my-type"
     *     id "my-id"
     *     script "ctx._source.counter += count"
     *     scriptParams {
     *         count = 1
     *     }
     *     upsert {
     *         some {
     *           other {
     *               info = "indexed if document does not exist"
     *           }
     *         }
     *         counter = 1
     *     }
     * }.actionGet()
     * </pre>
     * Note: All updates are really delete-then-index operations and partial updates <em>require</em> that the
     * document's source be stored (defaults to {@code true}, but changeable in the type's mapping).
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link UpdateRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<UpdateResponse> update(Client self, Closure requestClosure) {
        doRequest(self, new UpdateRequest(), requestClosure, self.&update)
    }

    /**
     * Deletes a document from the index based on the index, type and id.
     * <pre>
     * DeleteResponse response = client.delete {
     *     index "my-index"
     *     type "my-type"
     *     id "my-id"
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link DeleteRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<DeleteResponse> delete(Client self, Closure requestClosure) {
        doRequest(self, new DeleteRequest(), requestClosure, self.&delete)
    }

    /**
     * Deletes all documents from one or more indices based on a query.
     * <pre>
     * DeleteByQueryResponse response = client.deleteByQuery {
     *     indices "my-index"
     *     types "my-type"
     *     source {
     *         query {
     *             range {
     *                 // Note: "value" is the field name
     *                 value {
     *                     gte = 100
     *                 }
     *             }
     *         }
     *     }
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link DeleteByQueryRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<DeleteByQueryResponse> deleteByQuery(Client self, Closure requestClosure) {
        doRequest(self, Requests.deleteByQueryRequest(), requestClosure, self.&deleteByQuery)
    }

    /**
     * Gets a document from the index based on the index, type and id.
     * <p />
     * Note: Get retrievals are performed in real time.
     * <pre>
     * GetResponse response = client.get {
     *     index "my-index"
     *     type "my-type"
     *     id "my-id"
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link GetRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<GetResponse> get(Client self, Closure requestClosure) {
        // index is expected to be set by the closure
        doRequest(self, Requests.getRequest(null), requestClosure, self.&get)
    }

    /**
     * Multi-get documents. This provides the mechanism to perform bulk requests (as opposed to bulk indexing) to avoid
     * unnecessary back-and-forth requests.
     * <pre>
     * MultiGetResponse response = client.multiGet {
     *     // You can still do code constructs in your Closures, like
     *     //  this loop to invoke add multiple times
     *     for (String id : ["my-id1", "my-id2", "my-id3"]) {
     *         add "my-index", "my-type", id
     *     }
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link MultiGetRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<MultiGetResponse> multiGet(Client self, Closure requestClosure) {
        doRequest(self, new MultiGetRequest(), requestClosure, self.&multiGet)
    }

    /**
     * Request suggestion matching for a specific query.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link SuggestRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<SuggestResponse> suggest(Client self, Closure requestClosure) {
        doRequest(self, new SuggestRequest(), requestClosure, self.&suggest)
    }

    /**
     * Search across one or more indices and one or more types with a query.
     * <pre>
     * SearchResponse response = client.search {
     *     indices "my-index1", "my-index2"
     *     types "my-types1", "my-types2"
     *     source {
     *         query {
     *             match_all { }
     *         }
     *     }
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link SearchRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<SearchResponse> search(Client self, Closure requestClosure) {
        doRequest(self, Requests.searchRequest(), requestClosure, self.&search)
    }

    /**
     * Perform multiple search requests similar to multi-get.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link MultiSearchRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<MultiSearchResponse> multiSearch(Client self, Closure requestClosure) {
        doRequest(self, new MultiSearchRequest(), requestClosure, self.&multiSearch)
    }

    /**
     * Request a count of documents matching a specified query.
     * <pre>
     * CountResponse response = client.count {
     *     indices "my-index1", "my-index2"
     *     types "my-types1", "my-types2"
     *     source {
     *         query {
     *             match_all { }
     *         }
     *     }
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link CountRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<CountResponse> count(Client self, Closure requestClosure) {
        doRequest(self, Requests.countRequest(), requestClosure, self.&count)
    }

    /**
     * A search scroll request to continue searching a previous scrollable search request.
     * <pre>
     * // Open the scan
     * SearchResponse searchResponse = client.search {
     *     indices "my-index"
     *     types "my-type"
     *     source {
     *         query {
     *             match_all { }
     *         }
     *         // Note: Size is per shard! 5 shards means 5000 documents per response
     *         size = 1000
     *     }
     *     searchType SearchType.SCAN
     *     // The time that the scroll stays open should be the minimum duration
     *     //  required
     *     scroll "10s"
     * }.actionGet()
     *
     * // Scroll through the results (like a database cursor)
     * SearchResponse response = client.searchScroll {
     *     // Note: next call should use response.scrollId!
     *     scrollId searchResponse.scrollId
     *     // keep the _next_ window open
     *     scroll "10s"
     * }.actionGet()
     * </pre>
     * Note: Each {@link SearchResponse} will contain a new ID to use for subsequent requests.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link SearchScrollRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<SearchResponse> searchScroll(Client self, Closure requestClosure) {
        doRequest(self, new SearchScrollRequest(), requestClosure, self.&searchScroll)
    }

    /**
     * Clears the search contexts associated with specified Scroll IDs.
     * <pre>
     * ClearScrollResponse response = client.clearScroll {
     *     addScrollId lastScrollId
     * }.actionGet()
     * </pre>
     * Technically, this is not a necessary action following any scan/scroll action, but you should <em>always</em> do
     * it to optimistically clean up resources.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link ClearScrollRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<ClearScrollResponse> clearScroll(Client self, Closure requestClosure) {
        doRequest(self, new ClearScrollRequest(), requestClosure, self.&clearScroll)
    }

    /**
     * An action that is the term vectors for a specific document.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link TermVectorsRequest}.
     * @return Never {@code null}.
     */
    @Deprecated
    static ListenableActionFuture<TermVectorsResponse> termVectors(Client self, Closure requestClosure) {
        doRequest(self, new TermVectorsRequest(), requestClosure, self.&termVectors)
    }

    /**
     * An action that is the term vectors for a specific document.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link TermVectorsRequest}.
     * @return Never {@code null}.
     */
    @Deprecated
    static ListenableActionFuture<TermVectorsResponse> termVector(Client self, Closure requestClosure) {
        doRequest(self, new TermVectorsRequest(), requestClosure, self.&termVector)
    }

    /**
     * Multi-get term vectors.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link MultiTermVectorsRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<MultiTermVectorsResponse> multiTermVectors(Client self, Closure requestClosure) {
        doRequest(self, new MultiTermVectorsRequest(), requestClosure, self.&multiTermVectors)
    }

    /**
     * Percolates a requesting the matching documents.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link PercolateRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<PercolateResponse> percolate(Client self, Closure requestClosure) {
        doRequest(self, new PercolateRequest(), requestClosure, self.&percolate)
    }

    /**
     * Performs multiple percolate requests.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link MultiPercolateRequest}.
     * @return Never {@code null}.
     */
    static ListenableActionFuture<MultiPercolateResponse> multiPercolate(Client self, Closure requestClosure) {
        doRequest(self, new MultiPercolateRequest(), requestClosure, self.&multiPercolate)
    }

    /**
     * Computes a score explanation for the specified request.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link ExplainRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    static ListenableActionFuture<ExplainResponse> explain(Client self, Closure requestClosure) {
        doRequest(self, new ExplainRequest(null, null, null), requestClosure, self.&explain)
    }

    /**
     * Put (set/add) the indexed script to be used by other requests.
     * <pre>
     * PutIndexedScriptResponse response = client.putIndexedScript {
     *     id 'my-script-name'
     *     // NOTE1: This will be the Groovy runtime within Elasticsearch
     *     //        and not the Groovy client (this)!
     *     scriptLang 'groovy'
     *     source {
     *         // NOTE2: The script is [in this case] Groovy, but it must
     *         //        be a string that is interpreted on the server
     *         // NOTE3: "count" is a script parameter that must be filled
     *         //        in by the associated update request that makes
     *         //        use of this script
     *         script = "ctx._source.count += count"
     *     }
     * }.actionGet()
     * </pre>
     * Once the above script is added, then you could make use of it by using it with an {@link UpdateRequest}.
     * <pre>
     * UpdateResponse updateResponse = client.update {
     *     index indexName
     *     type typeName
     *     id docId
     *     source {
     *         script_id 'testPutIndexedScriptRequest'
     *         lang 'groovy'
     *         params {
     *             count = 5
     *         }
     *     }
     * }.actionGet()
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link PutIndexedScriptRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    static ListenableActionFuture<PutIndexedScriptResponse> putIndexedScript(Client self, Closure requestClosure) {
        doRequest(self, new PutIndexedScriptRequest(), requestClosure, self.&putIndexedScript)
    }

    /**
     * Get an indexed script.
     * <pre>
     * GetIndexedScriptResponse response = client.getIndexedScript {
     *     id 'my-script-name'
     *     scriptLang 'groovy'
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link GetIndexedScriptRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    static ListenableActionFuture<GetIndexedScriptResponse> getIndexedScript(Client self, Closure requestClosure) {
        doRequest(self, new GetIndexedScriptRequest(), requestClosure, self.&getIndexedScript)
    }

    /**
     * Delete an indexed script.
     * <pre>
     * DeleteIndexedScriptResponse response = client.deleteIndexedScript {
     *     id 'my-script-name'
     *     scriptLang 'groovy'
     * }.actionGet()
     * </pre>
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param requestClosure The map-like closure that configures the {@link DeleteIndexedScriptRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    static ListenableActionFuture<DeleteIndexedScriptResponse> deleteIndexedScript(Client self,
                                                                                   Closure requestClosure) {
        doRequest(self, new DeleteIndexedScriptRequest(), requestClosure, self.&deleteIndexedScript)
    }

    /**
     * A more like this action to search for documents that are "like" a specific document.
     * <p />
     * This method may be deprecated in favor of one that does not <em>require</em> the {@code index} to be supplied to
     * the method in the future.
     *
     * @param self The {@code this} reference for the {@link Client}
     * @param index The index to load the document(s) from
     * @param requestClosure The map-like closure that configures the {@link MoreLikeThisRequest}.
     * @return Never {@code null}.
     * @throws NullPointerException if any parameter is {@code null} except {@code index}
     */
    static ListenableActionFuture<SearchResponse> moreLikeThis(Client self, String index, Closure requestClosure) {
        // the only one that _requires_ the index as a parameter/constructor arg (no public setter)
        doRequest(self, Requests.moreLikeThisRequest(index), requestClosure, self.&moreLikeThis)
    }
}