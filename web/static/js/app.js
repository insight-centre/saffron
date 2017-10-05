angular.module('app', ['ngMaterial']);

angular.module('app').component('toptopics', {
    templateUrl: '/top-topics.html',
    controller: function ($http) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function () {
            $http.get("/top-topics?n=" + ctrl.n2 + "&offset=30").then(
                    function (response) {
                        ctrl.topics = [];
                        for (t = 0; t < response.data.length; t++) {
                            ctrl.topics.push({
                                "topic_string": response.data[t],
                                "pos": (t + 1 + ctrl.n2)
                            });
                        }
                        ctrl.n = ctrl.n2;
                    },
                    function (response) {
                        console.log("Failed to get top topics")
                    }
            );
        }
        this.topicForward = function () {
            ctrl.n2 += 30;
            this.loadTopics();
        }
        this.topicBack = function () {
            ctrl.n2 -= 30;
            this.loadTopics();
        }
        this.loadTopics();
    }
});

angular.module('app').component('topic', {
    templateUrl: '/topics.html',
    controller: function () {
        var ctrl = this;
        if (topic) {
            ctrl.topic = topic;
        }
    }
});

angular.module('app').component('relatedtopics', {
    templateUrl: '/topic-list.html',
    bindings: {
        topic: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        $http.get('/topic-sim?topic1=' + ctrl.topic).then(function (response) {
            ctrl.topics = [];
            for (t = 0; t < response.data.length; t++) {
                ctrl.topics.push({
                    "topic_string": response.data[t].topic2,
                    "pos": (t + 1),
                    "left": t <= response.data.length / 2,
                    "right": t > response.data.length / 2
                });
            }
        })
    }
});

angular.module('app').component('relatedauthors', {
    templateUrl: '/author-list.html',
    bindings: {
        topic: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        /*$http.get('/author-topics?topic=' + ctrl.topic).then(function (response) {
            ctrl.authors = [];
            for (t = 0; t < response.data.length; t++) {
                ctrl.authors.push({
                    "name": response.data[t].author_id,
                    "pos": (t + 1),
                    "left": t <= response.data.length / 2,
                    "right": t > response.data.length / 2
                });
            }
        })*/
    }
});

angular.module('app').component('relateddocuments', {
    templateUrl: '/document-list.html',
    bindings: {
        topic: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        console.log(ctrl);
        $http.get('/doc-topics?topic=' + ctrl.topic).then(function (response) {
            ctrl.docs = [];
            console.log(response.data);
            for (t = 0; t < response.data.length; t++) {
                ctrl.docs.push({
                    "doc": response.data[t],
                    "pos": (t + 1),
                    "left": t <= response.data.length / 2,
                    "right": t > response.data.length / 2
                });
            }
        })
    }
});

