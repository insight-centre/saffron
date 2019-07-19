Saffron API Documentation
=========================


**Base URL: http://{saffron_ip_address}:8080/api/v1**

API Routes in place:

* **GET /run**

> This GET request will return all previously ran Saffron taxonomies. 
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_all_runs.json)

---

* **DELETE /run/{saffron-run-id}**

> This DELETE request will a given saffron run for the given saffron run ID. 

---

* **GET /run/{saffron-run-id}**

> This GET request will return a Saffron Taxonomy object for a given saffron run ID. 
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_run_response.json)

---

* **GET /run/{saffron-run-id}/topics**

> This GET request will return all topics for for a given saffron run ID. 
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_topics_response.json)

---

* **GET /run/{saffron-run-id}/search/{string}**

> This GET request will return search results within a given Corpus for a given saffron run ID and string. 
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_search_response.json)

---

* **GET /run/{saffron-run-id}/topics/{topic}/children**

> This GET request will return all children topics for a given saffron run ID and topic string. 
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_children_response.json)
 
--- 
 
* **GET /run/{saffron-run-id}/topics/{topic}/parent**

> This GET request will return the parent topic for a given saffron run ID and topic string. 
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_parent_response.json)

---

* **POST /run/{saffron-run-id}/topics/changeroot (Change Topic root)**

> This POST request will change the parent topic for a given set of saffron topics and for a given saffron run ID.
> [Sample Request](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/change_root_rq.json)
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/change_root_resp.json)

---

* **POST /run/{saffron-run-id}/topics/update (Update accept/reject/none status)**

> This POST request will change topic status (Accepted/Rejected/None) for a given set of saffron topics and for a given saffron run ID.
> [Sample Request](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/accept_reject_topic_rq.json)
> [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/accept_reject_topic_rs.json)

---
 
* **POST /run/rerun/{saffron-run-id}**

> This POST request will rerun the taxonomy in order to recalculate the scoring based on changes made for a given saffron run ID.
> [request] - Empty body 
 
---