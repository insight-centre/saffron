Saffron API Documentation
=========================


Base URL: http://{saffron_ip_address}:8080/api/v1

API Routes in place:

* GET /run

`This API call returns all `

* POST /run
* GET /run/{saffron-run-id}
* GET /run/{saffron-run-id}/topics
* DELETE /run/{saffron-run-id}/topics/{topic_id} (SINGLE Topic)
* POST /run/{saffron-run-id}/topics (Delete multiple)
* POST /run/{saffron-run-id}/topics/{topic_id} (Change Topic root)
* POST /run/{saffron-run-id}/topics/update (Update accept/reject/none status)
* PUT /run/{saffron-run-id}/topics/{topic_id}
