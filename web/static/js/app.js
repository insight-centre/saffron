angular.module('app', ['ngMaterial']);

angular.module('app').component('toptopics', {
    templateUrl: '/top-topics.html',
    controller: function ($http) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function () {
            $http.get('/' + saffronDatasetName + "/top-topics?n=" + ctrl.n2 + "&offset=30").then(
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
    controller: function ($http, $scope, $window) {
        var ctrl = this;
        if (topic) {
            ctrl.topic = topic;
        }

        // Functionality for the new Saffron
        // editing abilities
        ctrl.saffronDatasetName = saffronDatasetName;
        ctrl.topic.topic_id = ctrl.saffronDatasetName + '_' + ctrl.topic.topic_string;

        // getting the current parent name to be used later
        $http.get('/' + saffronDatasetName + '/parents?topic=' + topic.topic_string).then(
            function (response) {
                ctrl.parent = response.data[response.data.length - 1];
                ctrl.current_parent_id = ctrl.parent;
                ctrl.parent_id = ctrl.parent;
                // console.log(response);
                console.log("Got parent name: " + ctrl.parent_id);
            },
            function (response) {
                console.log(response);
                console.log("Failed to get parent name");
            }
        );

        // this method is used to fill the select for parent name
        $http.get('/api/v1/run/' + saffronDatasetName + '/topics').then(
            function (response) {
                ctrl.topics =  [];
                for (let t = 0; t < response.data.length; t++) {
                    ctrl.topics.push({
                        "topic_string": response.data[t].topicString,
                        "topic_id": response.data[t].topicString,
                        "pos": (t + 1)
                    });
                }
                // console.log(response);
                console.log("Got topic: " + response.data);
            },
            function (response) {
                console.log(response);
                console.log("Failed to get topics");
            }
        );

        $scope.ApiSaveTopic = function(old_topic_id, new_topic_name, old_topic_parent, new_topic_parent, $event){
            $event.preventDefault();
            let JsonData = {
                    "topics": [
                        {
                            "id": old_topic_id,
                            "new_id": new_topic_name,
                            "current_parent": old_topic_parent,
                            "new_parent": new_topic_parent
                        }
                    ]
                };

            console.log(JsonData);

            $http.post('/api/v1/run/' + saffronDatasetName + '/topics/changeroot', JsonData).then(
                function (response) {
                    console.log(response);
                    console.log("Post topic: " + new_topic_name);
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to put topic");
                }
            );

        };

        $scope.ApiDeleteTopic = function(topic_string, $event){
            $event.preventDefault();
            // this needs to be discussed with Andy for deleting the main topic
            $http.delete('/api/v1/run/' + saffronDatasetName + '/topics/' + topic_string).then(
                function (response) {
                    console.log(response);
                    console.log("Deleted: " + topic_string);
                    //$window.location.href = '/' + saffronDatasetName + '/';
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to delete topic");
                }
            );
        };
    }
});

angular.module('app').component('relatedtopics', {
    templateUrl: '/topic-list.html',
    bindings: {
        topic: '<',
        doc: '<',
        author: '<'
    },
    controller: function ($http, $scope) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function () {
            if (ctrl.topic) {
                ctrl.title = "Related topics";
                $http.get('/' + saffronDatasetName + '/topic-sim?n=20&offset=' + ctrl.n2 + '&topic1=' + ctrl.topic).then(function (response) {
                    ctrl.topics = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.topics.push({
                            "topic_string": response.data[t].topic2_id,
                            "score": Math.round(response.data[t].similarity * 100) + "%",
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.data.length / 2,
                            "right": t >= response.data.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            } else if (ctrl.doc) {
                ctrl.title = "Main topics";
                $http.get('/' + saffronDatasetName + '/doc-topics?n=20&offset=' + ctrl.n2 + '&doc=' + ctrl.doc).then(function (response) {
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
            } else if (ctrl.author) {
                ctrl.title = "Main topics";
                $http.get('/' + saffronDatasetName + '/author-topics?n=20&offset=' + ctrl.n2 + '&author=' + ctrl.author).then(function (response) {
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

        // Functionality for the new Saffron
        // editing abilities
        ctrl.saffronDatasetName = saffronDatasetName;

        ctrl.checkedStatus = false;
        $scope.checkStatus = function(){
            angular.forEach(ctrl.topics,function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        // $scope.removeParent = function (e) {
        //     console.log(e.target);
        //     angular.element(e.target).parent().parent().remove();
        // };

        $scope.ApiDeleteSingleTopic = function(topic_string, $event){
            $event.preventDefault();
            // this needs to be discussed with Andy for deleting the main topic
            $http.delete('/api/v1/run/' + saffronDatasetName + '/topics/' + saffronDatasetName + '_' + topic_string).then(
                function (response) {
                    console.log(response);
                    console.log("Deleted: " + topic_string);
                    ctrl.topics = ctrl.topics.filter(function(item) {
                        return item.topic_string !== topic_string;
                    });
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to delete topic");
                }
            );
        };

        $scope.checkAll = function() {
            $scope.checkedAll = (!$scope.checkedAll);
            ctrl.checkedStatus = ($scope.checkedAll);

            if($scope.checkedAll){
                angular.forEach(ctrl.topics,function(item){
                   item.checked = true;
                });
            } else {
                angular.forEach(ctrl.topics,function(item){
                    item.checked = false;
                });
            }
        };

        $scope.ApiDeleteMultiTopics = function($event){
            $event.preventDefault();

            $scope.topicsContainer=[];
            angular.forEach(ctrl.topics,function(item, index){
                if (item.checked === true){
                    ctrl.topics.splice(index, 1);
                    console.log(index);
                    $scope.topicsContainer.push({"id": saffronDatasetName + '_' + item.topic_string});
                }

            });

            let finalTopics = {"topics": $scope.topicsContainer};

            $http.post('/api/v1/run/' + saffronDatasetName + '/topics/', finalTopics).then(
                function (response) {
                    console.log(response);
                    console.log("Deleted Multi: " + finalTopics);
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to delete topics");
                }
            );
        };

        // $http.put('/api/v1/run/' + saffronDatasetName + '/topics/' + old_topic_id, JsonData).then(
        //     function (response) {
        //         console.log(response);
        //         console.log("Put topic: " + new_topic_name);
        //     },
        //     function (response) {
        //         console.log(response);
        //         console.log("Failed to put topic");
        //     }
        // );
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
        if (ctrl.topic) {
            ctrl.title = "Major authors on this topic";
            $http.get('/' + saffronDatasetName + '/author-topics?topic=' + ctrl.topic).then(function (response) {
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
        } else if (ctrl.author) {
            ctrl.title = "Similar authors"
            $http.get('/' + saffronDatasetName + '/author-sim?author1=' + ctrl.author).then(function (response) {
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
        }
    }
});

angular.module('app').component('relateddocuments', {
    templateUrl: '/document-list.html',
    bindings: {
        topic: '<',
        author: '<'
    },
    controller: function ($http, $sce) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function () {
            if (ctrl.topic) {
                $http.get('/' + saffronDatasetName + '/doc-topics?n=20&offset=' + ctrl.n2 + '&topic=' + ctrl.topic).then(function (response) {
                    ctrl.docs = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.docs.push({
                            "doc": response.data[t],
                            "contents_highlighted": $sce.trustAsHtml(response.data[t].contents.split(ctrl.topic).join("<b>" + ctrl.topic + "</b>")),
                            "pos": (t + 1)
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            } else if (ctrl.author) {
                $http.get('/' + saffronDatasetName + '/author-docs?n=20&offset=' + ctrl.n2 + '&author=' + ctrl.author).then(function (response) {
                    ctrl.docs = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.docs.push({
                            "doc": response.data[t],
                            "pos": (t + 1)
                        });
                    }
                    ctrl.n = ctrl.n2;
                })
            }
        };
        this.docForward = function () {
            ctrl.n2 += 20;
            this.loadTopics();
        };
        this.docBackward = function () {
            ctrl.n2 -= 20;
            this.loadTopics();
        };
        this.loadTopics();
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

angular.module('app').controller('Breadcrumbs', function ($scope, $http) {
    if (topic) {
        $scope.parents = [];
        $http.get('/' + saffronDatasetName + '/parents?topic=' + topic.topic_string).then(function (response) {
            $scope.parents = response.data;
        })
    }
});

angular.module('app').component('childtopics', {
    templateUrl: '/topic-list.html',
    bindings: {
        topic: '<'
    },
    controller: function ($http, $scope) {
        var ctrl = this;
        ctrl.title = "Child topics";
        $http.get('/' + saffronDatasetName + '/children?topic=' + ctrl.topic).then(function (response) {
            ctrl.topics = [];
            for (t = 0; t < response.data.length; t++) {
                ctrl.topics.push({
                    "topic_string": response.data[t].topic,
                    "score": Math.round(response.data[t].score * 100) + "%",
                    "pos": (t + 1),
                    "left": t < response.data.length / 2,
                    "right": t >= response.data.length / 2
                });
            }
        });

        // Functionality for the new Saffron
        // editing abilities
        ctrl.saffronDatasetName = saffronDatasetName;

        ctrl.checkedStatus = false;
        $scope.checkStatus = function(){
            angular.forEach(ctrl.topics,function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        $scope.ApiDeleteSingleTopic = function(topic_string, $event){
            $event.preventDefault();
            // this needs to be discussed with Andy for deleting the main topic
            $http.delete('/api/v1/run/' + saffronDatasetName + '/topics/' + saffronDatasetName + '_' + topic_string).then(
                function (response) {
                    console.log(response);
                    console.log("Deleted: " + topic_string);
                    ctrl.topics = ctrl.topics.filter(function(item) {
                        return item.topic_string !== topic_string;
                    });
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to delete topic");
                }
            );
        };

        $scope.checkAll = function() {
            $scope.checkedAll = (!$scope.checkedAll);
            ctrl.checkedStatus = ($scope.checkedAll);

            if($scope.checkedAll){
                angular.forEach(ctrl.topics,function(item){
                    item.checked = true;
                });
            } else {
                angular.forEach(ctrl.topics,function(item){
                    item.checked = false;
                });
            }
        };

        $scope.ApiDeleteMultiTopics = function($event){
            $event.preventDefault();

            $scope.topicsContainer=[];
            angular.forEach(ctrl.topics,function(item, index){
                if (item.checked === true){
                    ctrl.topics.splice(index, 1);
                    console.log(index);
                    $scope.topicsContainer.push({"id": saffronDatasetName + '_' + item.topic_string});
                }

            });

            let finalTopics = {"topics": $scope.topicsContainer};

            $http.post('/api/v1/run/' + saffronDatasetName + '/topics/', finalTopics).then(
                function (response) {
                    console.log(response);
                    console.log("Deleted Multi: " + finalTopics);
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to delete topics");
                }
            );
        };
    }
});

angular.module('app').component('searchresults', {
    templateUrl: '/search-results.html',
    controller: function ($http, $sce) {
        var ctrl = this;
        ctrl.title = "Topics";
        $http.get('/' + saffronDatasetName + '/search_results?query=' + searchQuery).then(function (response) {
            ctrl.results = response.data;
            for (i in ctrl.results) {
                ctrl.results[i].contents_highlighted = $sce.trustAsHtml(ctrl.results[i].contents.split(searchQuery).join("<b>" + searchQuery + "</b>"));
            }
        });
    }
});

angular.module('app').component('metadata', {
    templateUrl: '/metadata.html',
    bindings: {
        doc: '<'
    },
    controller: function ($http) {
        var ctrl = this;
        ctrl.title = "Metadata";
        if (doc) {
            ctrl.doc = doc;
        }
        this.hasMetadata = function () {
            return Object.keys(ctrl.doc.metadata).length > 0;
        };
        this.hasNoMetadata = function () {
            return Object.keys(ctrl.doc.metadata).length === 0;
        };
    }
});