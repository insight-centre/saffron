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
        topic: '<',
        doc: '<',
        author: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function() {
            if(ctrl.topic) {
                ctrl.title = "Related topics";
                $http.get('/topic-sim?n=20&offset=' + ctrl.n2 + '&topic1=' + ctrl.topic).then(function (response) {
                    ctrl.topics = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.topics.push({
                            "topic_string": response.data[t].topic2_id,
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.data.length / 2,
                            "right": t >= response.data.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            } else if(ctrl.doc) {
                ctrl.title = "Main topics";
                $http.get('/doc-topics?n=20&offset=' + ctrl.n2 + '&doc=' + ctrl.doc).then(function(response) {
                    ctrl.topics = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.topics.push({
                            "topic_string": response.data[t].topic_string,
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.data.length / 2,
                            "right": t >= response.data.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            } else if(ctrl.author) {
                ctrl.title = "Main topics";
                $http.get('/author-topics?n=20&offset=' + ctrl.n2 + '&author=' + ctrl.author).then(function(response) {
                    ctrl.topics = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.topics.push({
                            "topic_string": response.data[t].topic_id,
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.data.length / 2,
                            "right": t >= response.data.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            }
        };
        this.topicForward = function () {
            ctrl.n2 += 20;
            this.loadTopics();
        }
        this.topicBack = function () {
            ctrl.n2 -= 20;
            this.loadTopics();
        }
        this.loadTopics();
        
    }
});

angular.module('app').component('relatedauthors', {
    templateUrl: '/author-list.html',
    bindings: {
        topic: '<',
        author: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        if(ctrl.topic) {
            ctrl.title = "Major authors on this topic";
            $http.get('/author-topics?topic=' + ctrl.topic).then(function (response) {
                ctrl.authors = [];
                for (t = 0; t < response.data.length; t++) {
                    ctrl.authors.push({
                        "id": response.data[t].id,
                        "name": response.data[t].name,
                        "pos": (t + 1),
                        "left": t < response.data.length / 2,
                        "right": t >= response.data.length / 2
                    });
                }
            });
        } else if(ctrl.author) {
            ctrl.title = "Similar authors"
            $http.get('/author-sim?author1=' + ctrl.author).then(function (response) {
                ctrl.authors = [];
                for(t = 0; t < response.data.length; t++) {
                    ctrl.authors.push({
                        "id": response.data[t].id,
                        "name": response.data[t].name,
                        "pos": (t + 1),
                        "left": t < response.data.length / 2,
                        "right": t >= response.data.length / 2
                        
                    });
                }
            });
        }
    }
});

angular.module('app').component('relateddocuments', {
    templateUrl: '/document-list.html',
    bindings: {
        topic: '<',
        author: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        if(ctrl.topic) {
            $http.get('/doc-topics?topic=' + ctrl.topic).then(function (response) {
                ctrl.docs = [];
                for (t = 0; t < response.data.length; t++) {
                    ctrl.docs.push({
                        "doc": response.data[t],
                        "pos": (t + 1)
                    });
                }
            });
        } else if(ctrl.author) {
            $http.get('/author-docs?author=' + ctrl.author).then(function (response) {
                ctrl.docs = [];
                for (t = 0; t < response.data.length; t++) {
                    ctrl.docs.push({
                        "doc": response.data[t],
                        "pos": (t + 1)
                    });
                }
            })
        }
    }
});

angular.module('app').component('author', {
    templateUrl: '/authors.html',
    controller: function () {
        var ctrl = this;
        if (author) {
            ctrl.author = author;
        }
    }
});

angular.module('app').component('doc', {
    templateUrl: '/docs.html',
    controller: function () {
        var ctrl = this;
        if (doc) {
            ctrl.doc = doc;
        }
    }
});

angular.module('app').controller('Breadcrumbs', function($scope,$http) {
   if(topic) {
       $scope.parents = [];
       $http.get('/parents?topic=' + topic.topic_string).then(function(response) {
           $scope.parents = response.data;
       })
   } 
});

angular.module('app').component('childtopics', {
    templateUrl: '/topic-list.html',
    bindings: {
        topic: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        ctrl.title = "Child topics";
        $http.get('/children?topic=' + ctrl.topic).then(function (response) {
            ctrl.topics = [];
            for (t = 0; t < response.data.length; t++) {
                ctrl.topics.push({
                    "topic_string": response.data[t],
                    "pos": (t + 1),
                    "left": t < response.data.length / 2,
                    "right": t >= response.data.length / 2
                });
            }
        });
    }
});
