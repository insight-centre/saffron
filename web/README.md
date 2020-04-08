Saffron API Documentation
=========================


**Base URL: http://{saffron_ip_address}:8080/api/v1**

API Routes in place:

* **GET /run**

> This GET request will return all previously ran Saffron taxonomies.  
> <p><button>[Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_all_runs.json)</button></p>

---

* **DELETE /run/{saffron-run-id}**

> This DELETE request will a given saffron run for the given saffron run ID.

---

* **GET /run/{saffron-run-id}**

> This GET request will return a Saffron Taxonomy object for a given saffron run ID.  
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_run_response.json)

---

* **GET /run/{saffron-run-id}/terms**

> This GET request will return all terms for for a given saffron run ID.  
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_terms_response.json)

---

* **GET /run/{saffron-run-id}/search/{string}**

> This GET request will return search results within a given Corpus for a given saffron run ID and string.  
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_search_response.json)

---

* **GET /run/{saffron-run-id}/terms/{term}/children**

> This GET request will return all children terms for a given saffron run ID and term string.  
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_children_response.json)

---

* **GET /run/{saffron-run-id}/terms/{term}/parent**

> This GET request will return the parent term for a given saffron run ID and term string.  
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_parent_response.json)

---

* **POST /run/{saffron-run-id}/terms/changeroot (Change Term root)**

> This POST request will change the parent term for a given set of saffron terms and for a given saffron run ID.

> - [Sample Request](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/change_root_rq.json)
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/change_root_resp.json)

---

* **POST /run/{saffron-run-id}/terms/update (Update accept/reject/none status)**

> This POST request will change term status (Accepted/Rejected/None) for a given set of saffron terms and for a given saffron run ID.  

> - [Sample Request](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/accept_reject_term_rq.json)
> - [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/accept_reject_term_rs.json)

---

* **POST /run/rerun/{saffron-run-id}**

> This POST request will rerun the taxonomy in order to recalculate the scoring based on changes made for a given saffron run ID.  
> - [request] - Empty body

---

* **POST /run/{saffron-run-id}/terms/updaterelationship**

> This POST request will change the status of taxonomy relationships. Currently this relationship can be "accepted" or "none"
> - [Sample Request](https://gitlab.insight-centre.org/saffron/saffron/blob/issue92/examples/api/post_change_relationship_request.json)

---

* **POST /run/new/zip/{saffron-run-id}**

> This will start the run with a given ZIP file
> The request body should be the ZIP file

---

* **POST /run/new/json/{saffron-run-id}**

> This will start the run with a JSON file
> The request body should be the JSON file

----

* **GET /run/new/crawl/{saffron-run-id}?url={url}&max_pages={max_pages}&domain={true|false}**

> This will start the run with a crawler for a website
> `url`: The URL to start crawling from
> `max_pages`: The maximum number of pages to crawl
> `domain`: Whether to limit the search only to the initial domain (default=true)

----

* **GET /run/status/{saffron-run-id}**

> Get the status of an executing run
> - [Sample Request](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/status_response.json)
