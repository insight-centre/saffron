const apiUrl = '/api/v1/run/';
const apiUrlWithSaffron = apiUrl + saffronDatasetName + '/';

angular.module('app', ['ngMaterial'])
    // this service is used to collect all public values
    // to be able to pass them among controllers
    .factory('sharedProperties', function ($location) {
        urlArray = $location.absUrl().split('/');

        return {
            getTopic: function () {
                return decodeURI(urlArray[5]);
            },
            setTopic: function(value) {
                topic = value;
            }
        };
    });

// general function to accept or reject child/related topics
function acceptRejectTopics($http, topics, mainTopic, status){
    topicsContainer = [];
    angular.forEach(topics,function(item, index){
        if (item.checked === true){
            topics.splice(index, 1);
            topicsContainer.push({"topic1": item.topic_string, "topic2": mainTopic, "status": status});
        }
    });

    let finalTopics = {"topics": topicsContainer};
    console.log(finalTopics);
    console.log(apiUrlWithSaffron + 'topics/update');

    $http.post(apiUrlWithSaffron + 'topics/update', finalTopics).then(
        function (response) {
            console.log(response);
            console.log("Changed status: " + finalTopics);
        },
        function (response) {
            console.log(response);
            console.log("Failed to change status of topics");
        }
    );
}

// general function to accept or reject child/related topics
function deleteOneRun($http, id){

    $http.delete(apiUrl + id).then(
        function (response) {
            console.log(response);
            console.log("Changed status: " + id);
        },
        function (response) {
            console.log(response);
            console.log("Failed to change status of topics");
        }
    ).then(function() { location.reload(); });

}


// Rerun function
function rerunTaxonomy($http, runId){

    console.log(runId);
    console.log(apiUrlWithSaffron + 'topics/update');
    let finalTopics = {"topics": topicsContainer};

    $http.post(apiUrlWithSaffron + runId, finalTopics).then(
        function (response) {
            console.log(response);
            console.log("Changed status: " + finalTopics);
        },
        function (response) {
            console.log(response);
            console.log("Failed to change status of topics");
        }
    );
}

// getting the top topics to fill the right sidebar on homepage of a run
angular.module('app').component('toptopics', {
    templateUrl: '/top-topics.html',
    controller: function ($http) {
        let ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function () {
            $http.get(apiUrlWithSaffron + "topics").then(
                function (response) {
                    response = response.data;
                    response.sort((a, b) => (a.score < b.score) ? 1 : -1);
                    response = response.slice(ctrl.n2, ctrl.n2+30);
                    ctrl.topics = [];
                    for (t = 0; t < response.length; t++) {
                        if(response[t].status !== "rejected") {
                            ctrl.topics.push({
                                "topic_string": response[t].topicString,
                                "pos": (t + 1 + ctrl.n2)
                            });                            
                        }
                    }
                    ctrl.n = ctrl.n2;
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to get top topics");
                }
            );
        };
        this.topicForward = function () {
            ctrl.n2 += 30;
            this.loadTopics();
        };
        this.topicBack = function () {
            ctrl.n2 -= 30;
            this.loadTopics();
        };
        this.loadTopics();
    }
});

// Edit component
angular.module('app').component('edittopics', {
    templateUrl: '/edit-topics-component.html',
    controller: function ($http, $scope, $window, $location, sharedProperties) {
        var ctrl = this;

        $http.get(apiUrlWithSaffron + 'topics').then(
            function (response) {
                ctrl.topics =  [];
                for (let t = 0; t < response.data.length; t++) {
                    ctrl.topics.push({
                        "topic_string": response.data[t].topicString,
                        "topic_id": response.data[t].topicString,
                        "pos": (t + 1)
                    });                       
                }
            },

            function (response) {
                console.log(response);
                console.log("Failed to get topics");
            }
        );

        // enabling multiple selections
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

        // adding selected status to item
        ctrl.checkedStatus = false;
        $scope.checkStatus = function(){
            angular.forEach(ctrl.topics,function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        // accept one or multiple topics only in the UI
        $scope.acceptTopics = function($event, topic){
            $event.preventDefault();
            if (topic == null) {
                angular.forEach(ctrl.topics,function(element){
                    if (element.checked === true) {
                        element.status = "accepted";
                        element.checkedStatus = false;
                        element.checked = false;
                    }
                });
                $scope.checkedAll = false;
            } else {
                ctrl.topics.forEach(function(element) {
                    if (element.topic_string === topic.topic_string) {
                        element.status = "accepted";
                    }
                }, topic);
            }
        };

        // reject one or multiple topics only in the UI
        $scope.rejectTopics = function($event, topic){
            $event.preventDefault();
            if (topic == null) {
                angular.forEach(ctrl.topics,function(element){
                    if (element.checked === true) {
                        element.status = "rejected";
                        element.checkedStatus = false;
                        element.checked = false;
                    }
                });
                $scope.checkedAll = false;
            } else {
                ctrl.topics.forEach(function(element) {
                    if (element.topic_string === topic.topic_string) {
                        element.status = "rejected";
                    }
                }, topic);
            }
        };

        // send all modifications to the API
        $scope.saveTopics = function() {
            /* $event.preventDefault();
            
            let JsonData = {
                    ???
                };
            
            $http.post(apiUrlWithSaffron + 'topics/update', JsonData).then(
                function (response) {
                    console.log(response);
                    console.log("Topics' status update successfully");
                    $window.location.href = '/' + saffronDatasetName + '/edit/parents';
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to update topics' status");
                }
            );*/
            $window.location.href = '/' + saffronDatasetName + '/edit/parents';
        }
    }
});

angular.module('app').component('editparents', {
    templateUrl: '/edit-parents-component.html',
    controller: function ($http, $scope, $window, $location, sharedProperties) {
        var ctrl = this;
       
        $scope.loadTopics = function() {
             ctrl.topics = [];
            $http.get(apiUrlWithSaffron).then(
                function (response) {
                    $scope.getChildren(response.data, "", null);
                },
                function (error) {
                    console.log(error);
                    console.log("Failed to get taxonomy structure");
                }
            );
        }
        
        $scope.getChildren = function(topic, parent_branch, parent) {
            var current_topic = {
                "topic_string": topic.root,
                "branch": parent_branch,
                "topic_id": topic.root,
                "parent": parent 
            }
            ctrl.topics.push(current_topic);

            for (let i = 0; i < topic.children.length; i++) {
                $scope.getChildren(topic.children[i], parent_branch == "" ? topic.root : parent_branch + " > " + topic.root, current_topic);
            }
        };

        $scope.changeParent = function(topic, new_parent) {
            // TO IMPLEMENT
            // API Call
            ctrl.activeTopic = null;
            //Reload topics
        };

        $scope.loadTopics();
    }
    
});

// the main topic component
angular.module('app').component('topic', {
    templateUrl: '/topics.html',
    controller: function ($http, $scope, $window, $location, sharedProperties) {
        var ctrl = this;

        // this method is used to fill the select for parent name
        $http.get(apiUrlWithSaffron + 'topics').then(
            function (response) {
                ctrl.topics =  [];
                for (let t = 0; t < response.data.length; t++) {

                    // to avoid looking for the current topic again
                    // I am registering the ctrl.topic here by matching the API
                    if (sharedProperties.getTopic() === response.data[t].topicString) {
                        ctrl.topic = response.data[t];
                    }

                    ctrl.topics.push({
                        "topic_string": response.data[t].topicString,
                        "topic_id": response.data[t].topicString,
                        "pos": (t + 1)
                    });
                }

                // get parent name
                $http.get(apiUrlWithSaffron + 'topics/' + ctrl.topic.topicString + '/parent').then(
                    function (response) {
                        ctrl.parent_id = response.data.root;
                        ctrl.current_parent_id = response.data.root;
                    },
                    function (response) {
                        console.log(response);
                        console.log("Failed to get parent name");
                    }
                );
            },

            function (response) {
                console.log(response);
                console.log("Failed to get topics");
            }
        );
        
        // function to save updates on topic name or topic parent
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

            $http.post(apiUrlWithSaffron + 'topics/changeroot', JsonData).then(
                function (response) {
                    console.log(response);
                    console.log("Post topic: " + new_topic_name);
                    $window.location.href = '/' + saffronDatasetName + '/topic/' + new_topic_name;
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to put topic");
                }
            );

        };

        // function to delete the main topic on topic page
        $scope.ApiDeleteTopic = function(topic_string, $event){
            $event.preventDefault();
            $http.post(apiUrlWithSaffron + 'topics/' + topic_string + '/rejected').then(
                function (response) {
                    console.log(response);
                    $window.location.href = '/' + saffronDatasetName + '/';
                },
                function (response) {
                    console.log(response);
                    console.log("Failed to delete topic");
                }
            );
        };
    }
});

// the related topics component
angular.module('app').component('relatedtopics', {
    templateUrl: '/topic-list.html',
    bindings: {
        topic: '<',
        doc: '<',
        author: '<'
    },
    controller: function ($http, $scope, sharedProperties) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTopics = function () {

            // if on topic page, show related topics
            if (ctrl.topic) {
                ctrl.title = "Related topics";

                var url = apiUrlWithSaffron + 'topicsimilarity/' + sharedProperties.getTopic();
                $http.get(url).then(function (response) {
                    response = response.data.topicsList;
                    response.sort((a, b) => (a.similarity < b.similarity) ? 1 : -1);
                    response = response.slice(ctrl.n2, ctrl.n2+20);
                    ctrl.topics = [];
                    for (t = 0; t < response.length; t++) {
                        ctrl.topics.push({
                            "topic_string": response[t].topicString2,
                            "score": Math.round(response[t].similarity * 100) + "%",
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.length / 2,
                            "right": t >= response.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });

            } else 

            // if on a document page, show top topics from the document
            if (ctrl.doc) {
                ctrl.title = "Main topics";

                var url = apiUrlWithSaffron + 'docs/' + ctrl.doc;
                $http.get(url).then(function (response) {
                    response = response.data.topicsList;
                    response.sort((a, b) => (a.occurences < b.occurences) ? 1 : -1);
                    response = response.slice(ctrl.n2, ctrl.n2+20);
                    ctrl.topics = [];
                    for (t = 0; t < response.length; t++) {
                        ctrl.topics.push({
                            "topic_string": response[t].topic,
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.length / 2,
                            "right": t >= response.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            } else 

            // if on an author page, show top topics from that author <!-- not API ready, still from the JSON files -->
            if (ctrl.author) {
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

        // enabling multiple selections
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

        // adding selected status to item
        ctrl.checkedStatus = false;
        $scope.checkStatus = function(){
            angular.forEach(ctrl.topics,function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        // accept one or multiple topics
        $scope.ApiAcceptTopics = function($event, topics){
            $event.preventDefault();
            topics ? topics.checked = true : '';
            acceptRejectTopics($http, ctrl.topics, ctrl.topic, "accepted");
        };

        // reject one or multiple topics
        $scope.ApiRejectTopics = function($event, topics){
            $event.preventDefault();
            topics ? topics.checked = true : '';
            acceptRejectTopics($http, ctrl.topics, ctrl.topic, "rejected");
        };
    }
});

// the breadcrumbs controller
angular.module('app').controller('Breadcrumbs', function ($scope, $http, $location, sharedProperties) {
    $scope.parents = [];

    function getParents(topicName) {
        var url = apiUrlWithSaffron + 'topics/' + topicName + '/parent';
        $http.get(url).then(function (response) {
            if(response.data.root){
                $scope.parents.unshift(response.data.root);
                getParents(response.data.root);
            }
        });
    }
    getParents(sharedProperties.getTopic());
});


// the runs controller
angular.module('app').controller('runs', function ($scope, $http, $location, sharedProperties) {
    $scope.parents = [];
    console.log("HERE")
    function getRuns() {
        var url = apiUrl;
        $http.get(url).then(function (response) {
            console.log(response)
            console.log(response.data)

            for (t = 0; t < response.data.length; t++) {
                console.log(response.data[t])
                $scope.parents.push(response.data[t])
            }
        });
    }
    getRuns();

    // reject one or multiple topics
    $scope.deleteRun = function($event, id){
        $event.preventDefault();
        deleteOneRun($http, id);
    };

});

// angular.module('app').controller('deleteRun', function ($scope, $http, $location, sharedProperties) {
//
//     $scope.parents = parents;
//     $scope.deleteRun = function(id) {
//         console.log("HERE")
//         if(confirm("Are you sure you want to delete this run?")) {
//             var index = $scope.parents.indexOf(id);
//             if(index >= 0) {
//                 $scope.parents.splice(index, 1);
//             }
//             $http.get("/api/v1/" + parents);
//         }
//     };
// });

// the child topics component
angular.module('app').component('childtopics', {
    templateUrl: '/topic-list.html',
    bindings: {
        topic: '<'
    },
    controller: function ($http, $scope, sharedProperties) {
        var ctrl = this;
        ctrl.title = "Child topics";

        var url = apiUrlWithSaffron + 'topics/' + sharedProperties.getTopic() + '/children';
        $http.get(url).then(function (response) {
            ctrl.topics = [];
            for (t = 0; t < response.data.children.length; t++) {
                if (response.data.children[t].status !== "rejected") {
                    ctrl.topics.push({
                        "topic_string": response.data.children[t].root,
                        "score": Math.round(response.data.children[t].linkScore * 100) + "%",
                        "pos": (t + 1),
                        "left": t < response.data.children.length / 2,
                        "right": t >= response.data.children.length / 2
                    });    
                }
            }
        });

        // Functionality for the new Saffron
        // editing abilities

        // enabling multiple selections
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

        // adding selected status to item
        ctrl.checkedStatus = false;
        $scope.checkStatus = function(){
            angular.forEach(ctrl.topics,function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        // accept one or multiple topics
        $scope.ApiAcceptTopics = function($event, topics){
            $event.preventDefault();
            topics ? topics.checked = true : '';
            acceptRejectTopics($http, ctrl.topics, ctrl.topic, "accepted");
        };

        // reject one or multiple topics
        $scope.ApiRejectTopics = function($event, topics){
            $event.preventDefault();
            topics ? topics.checked = true : '';
            acceptRejectTopics($http, ctrl.topics, ctrl.topic, "rejected");
        };
    }
});

// search results: needs to implement the highlight
angular.module('app').component('searchresults', {
    templateUrl: '/search-results.html',
    controller: function ($http, $sce) {
        var ctrl = this;
        ctrl.title = "Topics";
        $http.get(apiUrlWithSaffron + 'search/' + searchQuery).then(function (response) {
            ctrl.results = response.data;
            // removed because we don't have a way to highlight the topic in the file through the API yet
            // for (i in ctrl.results) {
            //     ctrl.results[i].contents_highlighted = $sce.trustAsHtml(ctrl.results[i].contents.split(searchQuery).join("<b>" + searchQuery + "</b>"));
            // }
        });
    }
});


// search results: needs to implement the highlight
angular.module('app').component('homepage', {
    templateUrl: '/home.html',
    controller: function ($http, $sce) {
        var ctrl = this;
        ctrl.title = "Topics";
        $http.get(apiUrlWithSaffron + 'search/' + searchQuery).then(function (response) {
            ctrl.results = response.data;
            // removed because we don't have a way to highlight the topic in the file through the API yet
            // for (i in ctrl.results) {
            //     ctrl.results[i].contents_highlighted = $sce.trustAsHtml(ctrl.results[i].contents.split(searchQuery).join("<b>" + searchQuery + "</b>"));
            // }
        });
    }
});


// <!-- not API ready, still from the JSON files -->
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
            ctrl.title = "Similar authors";
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

// <!-- not API ready, still from the JSON files -->
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

// <!-- not API ready, still from the JSON files -->
angular.module('app').component('author', {
    templateUrl: '/authors.html',
    controller: function () {
        var ctrl = this;
        if (author) {
            ctrl.author = author;
        }
    }
});

// <!-- not API ready, still from the JSON files -->
angular.module('app').component('doc', {
    templateUrl: '/docs.html',
    controller: function () {
        var ctrl = this;
        if (doc) {
            ctrl.doc = doc;
        }
    }
});

// <!-- not API ready, still from the JSON files -->
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
