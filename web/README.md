Saffron API Documentation
=========================


Base URL: http://{saffron_ip_address}:8080/api/v1

API Routes in place:

* GET /run


* POST /run


* DELETE /run/{saffron-run-id}


* GET /run/{saffron-run-id}
    [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_run_response.json)

* GET /run/{saffron-run-id}/topics
    [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_topics_response.json)

* GET /run/{saffron-run-id}/search/{topic}
    [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_search_response.json)

* GET /run/{saffron-run-id}/topics/{topic}/children
    [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_children_response.json)
 
* GET /run/{saffron-run-id}/topics/{topic}/parent
    [Sample Response](https://gitlab.insight-centre.org/saffron/saffron/blob/master/examples/api/get_parent_response.json)


* POST /run/{saffron-run-id}/topics/{topic_id}/{topic_id2}/{status}
    


* DELETE /run/{saffron-run-id}/topics/{topic_id} (SINGLE Topic)



* POST /run/{saffron-run-id}/topics (Delete multiple)


* POST /run/{saffron-run-id}/topics/changeroot (Change Topic root)


* POST /run/{saffron-run-id}/topics/update (Update accept/reject/none status)


* PUT /run/{saffron-run-id}/topics/{topic_id}

 
* GET /run/{saffron-run-id}/authortopics

 
* GET /run/{saffron-run-id}/authortopics/{topic}


* GET /run/{saffron-run-id}/authorsimilarity


* GET /run/{saffron-run-id}/authorsimilarity/{topic}


* GET /run/{saffron-run-id}/authorsimilarity/{topic1}/{topic2} 


* GET /run/{saffron-run-id}/topiccorrespondence


* GET /run/{saffron-run-id}/topiccorrespondence/{topic}


* GET /run/{saffron-run-id}/docs/{document_id}
 

* GET /run/{saffron-run-id}/topicextraction


* GET /run/{saffron-run-id}/topicextraction/{topic}


* GET /run/{saffron-run-id}/topicsimilarity


* GET /run/{saffron-run-id}/topicsimilarity/{topic}


* GET /run/{saffron-run-id}/topicsimilarity/{topic1}/{topic2}




