const apiUrl = '/api/v1/run/';
const apiUrlWithSaffron = apiUrl + saffronDatasetName + '/';

angular.module('app', ['ngMaterial', 'ui.bootstrap'])
    // this service is used to collect all public values
    // to be able to pass them among controllers
    .factory('sharedProperties', function($location) {
        urlArray = $location.absUrl().split('/');

        return {
            getterm: function() {
                return decodeURI(urlArray[5]);
            },
            setterm: function(value) {
                term = value;
            }
        };
    });

// general function to accept or reject child/related terms
function acceptRejectterms($http, terms, mainterm, status) {
    termsContainer = [];
    angular.forEach(terms, function(item, index) {
        if (item.checked === true) {
            terms.splice(index, 1);
            termsContainer.push({ "term1": item.term_string, "term2": mainterm, "status": status });
        }
    });

    let finalterms = { "terms": termsContainer };
    console.log(finalterms);
    console.log(apiUrlWithSaffron + 'terms/update');

    $http.post(apiUrlWithSaffron + 'terms/update', finalterms).then(
        function(response) {
            console.log(response);
            console.log("Changed status: " + finalterms);
        },
        function(response) {
            console.log(response);
            console.log("Failed to change status of terms");
        }
    );
}

// general function to accept or reject child/related terms
function deleteOneRun($http, id) {

    $http.delete(apiUrl + id).then(
        function(response) {
            console.log(response);
            console.log("Changed status: " + id);
        },
        function(response) {
            console.log(response);
            console.log("Failed to change status of terms");
        }
    ).then(function() { location.reload(); });

}


// Rerun function
function rerunTaxonomy($http, runId) {

    console.log(runId);

    $http.post(apiUrl + runId).then(
        function(response) {
            console.log(response);
            console.log("Reran:  " + runId);
        },
        function(response) {
            console.log(response);
            console.log("Failed to change status of terms");
        }
    );
}


angular.module('app').component('header', {
    templateUrl: '/header-component.html',
    controller: function($location, $scope, $window) {
        var ctrl = this;
        ctrl.menuList = [];
        ctrl.menuList.push({
            "href": "/" + saffronDatasetName + "/",
            "text": "Home"
        });


        ctrl.searchAction = "/" + saffronDatasetName + "/search/";
    },
    bindings: {
        //expects the value "yes"
        menu: '@',
        search: '@'
    }
});

// getting the top terms to fill the right sidebar on homepage of a run
angular.module('app').component('topterms', {
    templateUrl: '/top-terms.html',
    controller: function($http) {
        let ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTerms = function() {
            $http.get(apiUrlWithSaffron + "terms").then(
                function(response) {
                    response = response.data;
                    response.sort((a, b) => (a.score < b.score) ? 1 : -1);
                    response = response.slice(ctrl.n2, ctrl.n2 + 30);
                    ctrl.terms = [];
                    for (t = 0; t < response.length; t++) {
                        if (response[t].status !== "rejected") {
                            ctrl.terms.push({
                                "term_string": response[t].termString,
                                "pos": (t + 1 + ctrl.n2)
                            });
                        }
                    }
                    ctrl.n = ctrl.n2;
                },
                function(response) {
                    console.log(response);
                    console.log("Failed to get top terms");
                }
            );
        };
        this.termForward = function() {
            ctrl.n2 += 30;
            this.loadTerms();
        };
        this.termBack = function() {
            ctrl.n2 -= 30;
            this.loadTerms();
        };
        this.loadTerms();
    }
});

angular.module('app').component('editterms', {
    templateUrl: '/edit-terms-component.html',
    controller: function ($http, $scope, $modal, $window, $location, sharedProperties) {
        var ctrl = this;
        ctrl.errorMessage = "";

        $http.get(apiUrlWithSaffron + 'terms').then(
            function (response) {
                ctrl.terms =  [];
                ctrl.rejected = [];
                for (let t = 0; t < response.data.length; t++) {
                    if (response.data[t].status !== "rejected") {
                        ctrl.terms.push({
                            "term_string": response.data[t].termString,
                            "term_id": response.data[t].id,
                            "status": response.data[t].status
                        });
                    } else {
                        ctrl.rejected.push({
                            "term_string": response.data[t].termString,
                            "term_id": response.data[t].id,
                            "status": response.data[t].status
                        });
                    }
                }
            },

            function (response) {
                console.log(response);
                console.log("Failed to get terms");
            }
        );

        // enabling multiple selections
        $scope.checkAll = function() {
            $scope.checkedAll = (!$scope.checkedAll);
            ctrl.checkedStatus = ($scope.checkedAll);

            if($scope.checkedAll){
                angular.forEach(ctrl.terms,function(item){
                    item.checked = true;
                });
            } else {
                angular.forEach(ctrl.terms,function(item){
                    item.checked = false;
                });
            }
        };

        // adding selected status to item
        ctrl.checkedStatus = false;
        $scope.checkStatus = function(termId,checkedValue){
            ctrl.checkedStatus = false;
            angular.forEach(ctrl.terms,function(item) {
                if (item.term_id === termId){
                    item.checked = checkedValue;
                }
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                }
            }, termId, checkedValue);
        };

        // accept one or multiple terms only in the UI
        $scope.acceptTerms = function($event, term){
            $event.preventDefault();
            if (term == null) {
                angular.forEach(ctrl.terms,function(element){
                    if (element.checked === true) {
                        element.status = "accepted";
                        element.checked = false;
                    }
                });
                ctrl.checkedStatus = false;
                $scope.checkedAll = false;
            } else {
                ctrl.terms.forEach(function(element) {
                    if (element.term_string === term.term_string) {
                        element.status = "accepted";
                        element.checked = false;
                        $scope.checkStatus(element.term_id,false);
                    }
                }, term);
            }
        };

        // reject one or multiple terms only in the UI
        $scope.rejectTerms = function($event, term){
            $event.preventDefault();
            if (term == null) {
                angular.forEach(ctrl.terms,function(element){
                    if (element.checked === true) {
                        element.status = "rejected";
                        element.checked = false;
                    }
                });
                ctrl.checkedStatus = false;
                $scope.checkedAll = false;
            } else {
                ctrl.terms.forEach(function(element) {
                    if (element.term_string === term.term_string) {
                        element.status = "rejected";
                        element.checked = false;
                        $scope.checkStatus(element.term_id,false);
                    }
                }, term);
            }
        };

        // reject one or multiple terms only in the UI
        $scope.revertTermDecision = function($event, term){
            $event.preventDefault();
            if (term == null) {
                angular.forEach(ctrl.terms,function(element){
                    if (element.checked === true) {
                        element.status = "none";
                        element.checked = false;
                    }
                });
                ctrl.checkedStatus = false;
                $scope.checkedAll = false;
            } else {
                ctrl.terms.forEach(function(element) {
                    if (element.term_string === term.term_string) {
                        element.status = "none";
                        element.checked = false;
                        $scope.checkStatus(element.term_id,false);
                    }
                }, term);
            }
        };

        $scope.showConfirm = function() {

            var modalInstance = $modal.open({
              animation: $scope.animationsEnabled,
              templateUrl: 'modal.html',
              controller: function($scope, $modalInstance) {
                $scope.ok = function() {
                    $modalInstance.close();
                };

                $scope.cancel = function() {
                    $modalInstance.dismiss('cancel');
                };
              },
              resolve: {
              }
            });

            modalInstance.result.then(function(selectedItem) {
                $scope.saveTerms();
            }, function() {
                // Saving cancelled
            });
          };

        // send all modifications to the API
        $scope.saveTerms = function() {
            var requestTerms = []
            ctrl.terms.forEach(function(term) {
                if (term.status !== undefined) {
                    requestTerms.push({
                        "term": term.term_string,
                        "status": term.status
                    });
                }
            });
            let requestData = {
                "terms": requestTerms
            };

            $http.post(apiUrlWithSaffron + 'terms/update', requestData).then(
                function (response) {
                    console.log(response);
                    console.log("Terms' status update successfully");
                    $window.location.href = '/' + saffronDatasetName + '/edit';
                },
                function (response) {
                    console.log(response);
                    ctrl.errorMessage = "An error has occurred while updating the term status. Please try again later or contact the administration.";
                    console.log("Failed to update terms' status");
                }
            );
        }
    }
});

angular.module('app').component('editparents', {
    templateUrl: '/edit-parents-component.html',
    controller: function ($http, $scope) {
        var ctrl = this;
        ctrl.message = null;

        $scope.loadTerms = function() {
             ctrl.terms = [];
            $http.get(apiUrlWithSaffron).then(
                function (response) {
                    $scope.getChildren(response.data, "", null, 0);
                },
                function (error) {
                    console.log(error);
                    console.log("Failed to get taxonomy structure");
                }
            );
        }

        $scope.getChildren = function(term, parent_branch, parent, depth) {
            var current_term = {
                "term_string": term.root,
                "branch": parent_branch,
                "term_id": term.root,
                "parent": parent,
                "status": term.status,
                "collapsed_branch" : "-".repeat(depth) + term.root
            }
            if (current_term["term_id"] === "HEAD_TERM") {
                current_term["term_string"] = "Root";
                current_term["collapsed_branch"] = "Root";
            }
            ctrl.terms.push(current_term);

            for (let i = 0; i < term.children.length; i++) {
                $scope.getChildren(term.children[i], parent_branch == "" ? current_term["term_string"] : parent_branch + " > " + current_term["term_string"], current_term, depth+1);
            }
        };

        $scope.changeParentStatus = function(term, status) {
            ctrl.activeTerm = null;

            var requestData = {
             "terms": [
                {
                  "term_child": term.term_id,
                  "term_parent": term.parent.term_id,
                  "status": status
                }
              ]
            };



            $http.post(apiUrlWithSaffron + "terms/updaterelationship", requestData).then(
                function (response) {
                    term.status = status;
                },
                function (error) {
                    ctrl.message = {
                    "text": "An error has ocurred while changing the status of '" + term.term_string + "' parent relationship. Try again later or contact the administration.",
                    "type": "danger",
                    "term": requestData.terms[0].id
                    }
                }
            )
        }

        $scope.changeParent = function(term, new_parent) {

            if (term.parent == new_parent) {
                ctrl.message = {
                    "text": "'" + term.term_string + "' parent kept the same.",
                    "type": "warning",
                    "term": requestData.terms[0].id
                }
                ctrl.activeTerm = null;
                return;
            }
            var requestData = {
                    "terms" : [{
                        "id": term.term_id,
                        "new_id": term.term_id,
                        "current_parent": term.parent.term_id,
                        "new_parent": new_parent.term_id
                    }]
                };

            $http.post(apiUrlWithSaffron + 'terms/changeroot', requestData).then(
                function (response) {
                    console.log(response);
                    console.log("Parent term update successfully");

                    //Reload terms
                    ctrl.message = {
                        "text": "'" + term.term_string + "' parent successfuly changed from '" + term.parent.term_string + "' to '" + new_parent.term_string +"'",
                        "type": "success",
                        "term": requestData.terms[0].id
                    }
                    ctrl.activeTerm = null;
                    $scope.loadTerms();
                },
                function (response) {
                    if (response.data === "The selected move parent target is a member of a child term and cannot be moved") {
                        ctrl.message = {
                            "text": "It is not possible to change the parent of a term to one of its children: circular inheritance problem. Choose an antecedent parent or a term in a parallel branch instead.",
                            "type": "error",
                            "term": requestData.terms[0].id
                        };
                    } else {
                        ctrl.message = {
                            "text": "An error has occurred. Please try again later or contact the administration.",
                            "type": "error",
                            "term": requestData.terms[0].id
                        }
                    }
                    ctrl.activeTerm = null;
                }
            );

        };
        $scope.loadTerms();
    }
});

// the main term component
angular.module('app').component('term', {
    templateUrl: '/terms.html',
    controller: function($http, $scope, $window, $location, sharedProperties) {
        var ctrl = this;

        // this method is used to fill the select for parent name

        $http.get(apiUrlWithSaffron + 'terms').then(
            function(response) {
                ctrl.terms = [];
                for (let t = 0; t < response.data.length; t++) {

                    // to avoid looking for the current term again
                    // I am registering the ctrl.term here by matching the API
                    if (sharedProperties.getterm() === response.data[t].termString) {
                        ctrl.term = response.data[t];
                    }

                    ctrl.terms.push({
                        "term_string": response.data[t].termString,
                        "term_id": response.data[t].termString,
                        "pos": (t + 1)
                    });
                }

                // get parent name
                $http.get(apiUrlWithSaffron + 'terms/' + ctrl.term.termString + '/parent').then(
                    function(response) {
                        ctrl.parent_id = response.data.root;
                        ctrl.current_parent_id = response.data.root;
                    },
                    function(response) {
                        console.log(response);
                        console.log("Failed to get parent name");
                    }
                );
            },

            function(response) {
                console.log(response);
                console.log("Failed to get terms");
            }
        );

        // function to save updates on term name or term parent
        $scope.ApiSaveterm = function(old_term_id, new_term_name, old_term_parent, new_term_parent, $event) {
            $event.preventDefault();
            let JsonData = {
                "terms": [{
                    "id": old_term_id,
                    "new_id": new_term_name,
                    "current_parent": old_term_parent,
                    "new_parent": new_term_parent
                }]
            };

            $http.post(apiUrlWithSaffron + 'terms/changeroot', JsonData).then(
                function(response) {
                    console.log(response);
                    console.log("Post term: " + new_term_name);
                    $window.location.href = '/' + saffronDatasetName + '/term/' + new_term_name;
                },
                function(response) {
                    console.log(response);
                    console.log("Failed to put term");
                }
            );

        };

        // function to delete the main term on term page
        $scope.ApiDeleteterm = function(term_string, $event) {
            $event.preventDefault();
            $http.post(apiUrlWithSaffron + 'terms/' + term_string + '/rejected').then(
                function(response) {
                    console.log(response);
                    $window.location.href = '/' + saffronDatasetName + '/';
                },
                function(response) {
                    console.log(response);
                    console.log("Failed to delete term");
                }
            );
        };

        ctrl.checkRerun = false;
        $scope.checkRerunStatus = function() {
            angular.forEach(ctrl.terms, function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        $scope.reRun = function() {
            console.log("here");
            var url = apiUrl + 'rerun/' + saffronDatasetName;
            $http.post(url, '').then(function(response) {
                console.log(response);

            }).then(function(resp) {
                console.log("here" + saffronDatasetName);
                //$window.location.href = '/execute?name=run6' + saffronDatasetName;
                $window.location.href = '/execute?name=run6';
            });
        };

    }
});

// the related terms component
angular.module('app').component('relatedterms', {
    templateUrl: '/term-list.html',
    bindings: {
        term: '<',
        doc: '<',
        author: '<'
    },
    controller: function($http, $scope, sharedProperties) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTerms = function() {

            // if on term page, show related terms
            if (ctrl.term) {
                ctrl.title = "Related terms";

                var url = apiUrlWithSaffron + 'termsimilarity/' + sharedProperties.getterm();
                $http.get(url).then(function(response) {
                    response = response.data;
                    response.sort((a, b) => (a.similarity < b.similarity) ? 1 : -1);
                    response = response.slice(ctrl.n2, ctrl.n2 + 20);
                    ctrl.terms = [];
                    for (t = 0; t < response.length; t++) {
                        ctrl.terms.push({
                            "term_string": response[t].termString2,
                            "score": Math.round(response[t].similarity * 100) + "%",
                            "pos": (t + 1 + ctrl.n2),
                            "left": t < response.length / 2,
                            "right": t >= response.length / 2
                        });
                    }
                    ctrl.n = ctrl.n2;
                });

            } else {

                // if on a document page, show top terms from the document
                if (ctrl.doc) {
                    ctrl.title = "Main terms";

                    var url = apiUrlWithSaffron + 'docs/' + ctrl.doc;
                    $http.get(url).then(function(response) {
                        response = response.data.terms;
                        response.sort((a, b) => (a.occurences < b.occurences) ? 1 : -1);
                        response = response.slice(ctrl.n2, ctrl.n2 + 20);
                        ctrl.terms = [];
                        for (t = 0; t < response.length; t++) {
                            ctrl.terms.push({
                                "term_string": response[t].term,
                                "pos": (t + 1 + ctrl.n2),
                                "left": t < response.length / 2,
                                "right": t >= response.length / 2
                            });
                        }
                        ctrl.n = ctrl.n2;
                    });
                } else {
                    ctrl.title = "Main terms";
                    var url = apiUrlWithSaffron + 'termauthors/' + ctrl.author;
                    $http.get(url).then(function(response) {
                            response.data.sort((a, b) => (a.score < b.score) ? 1 : -1);
                            response.data = response.data.slice(ctrl.n2, ctrl.n2 + 20);
                            ctrl.terms = [];
                            for (t = 0; t < response.data.length; t++) {
                                ctrl.terms.push({
                                    "term_string": response.data[t].termId,
                                    "pos": (t + 1 + ctrl.n2),
                                    "left": t < response.data.length / 2,
                                    "right": t >= response.data.length / 2
                                });
                            }
                            ctrl.n = ctrl.n2;
                      });
                }
            }
        }

        this.termForward = function() {
            ctrl.n2 += 20;
            this.loadTerms();
        }
        this.termBack = function() {
            ctrl.n2 -= 20;
            this.loadTerms();
        }
        this.loadTerms();

        // Functionality for the new Saffron
        // editing abilities

        // enabling multiple selections
        $scope.checkAll = function() {
            $scope.checkedAll = (!$scope.checkedAll);
            ctrl.checkedStatus = ($scope.checkedAll);

            if ($scope.checkedAll) {
                angular.forEach(ctrl.terms, function(item) {
                    item.checked = true;
                });
            } else {
                angular.forEach(ctrl.terms, function(item) {
                    item.checked = false;
                });
            }
        };

        // adding selected status to item
        ctrl.checkedStatus = false;
        $scope.checkStatus = function() {
            angular.forEach(ctrl.terms, function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        // accept one or multiple terms
        $scope.ApiAcceptterms = function($event, terms) {
            $event.preventDefault();
            terms ? terms.checked = true : '';
            acceptRejectterms($http, ctrl.terms, ctrl.term, "accepted");
        };

        // reject one or multiple terms
        $scope.ApiRejectterms = function($event, terms) {
            $event.preventDefault();
            terms ? terms.checked = true : '';
            acceptRejectterms($http, ctrl.terms, ctrl.term, "rejected");
        };

        // adding selected status to item

    }
});

// the breadcrumbs controller
angular.module('app').controller('Breadcrumbs', function($scope, $http, $location, sharedProperties) {
    $scope.parents = [];

    function getParents(termName) {
        if (termName !== 'HEAD_TERM') {
            var url = apiUrlWithSaffron + 'terms/' + termName + '/parent';
            $http.get(url).then(function(response) {
                if (response.data.root !== $scope.parents[0]) {
                    $scope.parents.unshift(response.data.root);
                    getParents(response.data.root);
                }
            });
        }

    }
    getParents(sharedProperties.getterm());
});

// retrain Saffron
angular.module('app').controller('edit', function($scope, $modal, $http, $window, $location) {

    $scope.retrain = function() {

        var modalInstance = $modal.open({
            animation: $scope.animationsEnabled,
            templateUrl: 'modal.html',
            controller: function($scope, $modalInstance) {
                $scope.ok = function() {
                    $modalInstance.close();
                };

                $scope.cancel = function() {
                    $modalInstance.dismiss('cancel');
                };
            },
            resolve: {}
        });

        modalInstance.result.then(function(selectedItem) {
            var url = apiUrl + "rerun/" + saffronDatasetName;
            $window.location.href = apiUrl + "rerun/" + saffronDatasetName;
        }, function() {
            // Saving cancelled
        });
    }
});


// the runs controller
angular.module('app').controller('runs', function($scope, $http, $location, sharedProperties) {
    $scope.parents = [];

    function getRuns() {
        var url = apiUrl;
        $http.get(url).then(function(response) {
            console.log(response)
            console.log(response.data)

            runIds = []

            for (var i in response.data) {
                value = response.data[i]
                runIds.push(value['id'])
            }


            for (t = 0; t < response.data.length; t++) {
                console.log(response.data[t])
                $scope.parents.push(response.data[t])
            }
        });

    }
    getRuns();

    $scope.checkRun = function($event) {
        var run_id = document.forms["formRun"]["newRunName"].value;
        if (runIds.includes(run_id)) {
            document.getElementById("saffronRunMessage").innerHTML = "<strong> A saffron-run with id  &quot; " + run_id + "   &quot; already exists. Please use a different name. </strong>"
            document.getElementById('saffronRunMessage').className = "alert alert-danger";
            $event.preventDefault();
        }
    };

    // reject one or multiple terms
    $scope.deleteRun = function($event, id) {
        $event.preventDefault();
        if (confirm("Are you sure you want to delete this run?")) {
            deleteOneRun($http, id);
        }
    };
});

// the child terms component
angular.module('app').component('childterms', {
    templateUrl: '/term-list.html',
    bindings: {
        term: '<'
    },
    controller: function($http, $scope, sharedProperties) {
        var ctrl = this;
        ctrl.title = "Child terms";

        var url = apiUrlWithSaffron + 'terms/' + sharedProperties.getterm() + '/children';
        $http.get(url).then(function(response) {
            ctrl.terms = [];
            console.log(response)
            for (t = 0; t < response.data.children.length; t++) {
                if (response.data.children[t].status !== "rejected") {
                    ctrl.terms.push({
                        "term_string": response.data.children[t].root,
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

            if ($scope.checkedAll) {
                angular.forEach(ctrl.terms, function(item) {
                    item.checked = true;
                });
            } else {
                angular.forEach(ctrl.terms, function(item) {
                    item.checked = false;
                });
            }
        };

        // adding selected status to item
        ctrl.checkedStatus = false;
        $scope.checkStatus = function() {
            angular.forEach(ctrl.terms, function(item) {
                if (item.checked === true) {
                    ctrl.checkedStatus = true;
                    return false;
                }
            });
        };

        // accept one or multiple terms
        $scope.ApiAcceptterms = function($event, terms) {
            $event.preventDefault();
            terms ? terms.checked = true : '';
            acceptRejectterms($http, ctrl.terms, ctrl.term, "accepted");
        };

        // reject one or multiple terms
        $scope.ApiRejectterms = function($event, terms) {
            $event.preventDefault();
            terms ? terms.checked = true : '';
            acceptRejectterms($http, ctrl.terms, ctrl.term, "rejected");
        };
    }
});

// search results: needs to implement the highlight
angular.module('app').component('searchresults', {
    templateUrl: '/search-results.html',
    controller: function($http, $sce) {
        var ctrl = this;
        ctrl.title = "terms";
        $http.get(apiUrlWithSaffron + 'search/' + searchQuery).then(function(response) {
            ctrl.results = response.data;
            // removed because we don't have a way to highlight the term in the file through the API yet
            // for (i in ctrl.results) {
            //     ctrl.results[i].contents_highlighted = $sce.trustAsHtml(ctrl.results[i].contents.split(searchQuery).join("<b>" + searchQuery + "</b>"));
            // }
        });
    }
});


// search results: needs to implement the highlight
angular.module('app').component('homepage', {
    templateUrl: '/home.html',
    controller: function($http, $sce) {
        var ctrl = this;
        ctrl.title = "terms";
        $http.get(apiUrlWithSaffron + 'search/' + searchQuery).then(function(response) {
            ctrl.results = response.data;
            // removed because we don't have a way to highlight the term in the file through the API yet
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
        term: '<',
        author: '<'
    },
    controller: function($http) {
        var ctrl = this;
        if (ctrl.term) {
            ctrl.title = "Major authors on this term";
            $http.get(apiUrlWithSaffron + 'authorterms/' + ctrl.term).then(function(response) {
                ctrl.authors = [];
                response.data.sort((a, b) => (a.tfirf < b.tfirf) ? 1 : -1);
                for (t = 0; t < response.data.length; t++) {
                	if(response.data[t] != null) {
	                    ctrl.authors.push({
	                        "id": response.data[t].author.id,
	                        "name": response.data[t].author.name,
	                        "pos": (t + 1),
	                        "left": t < response.data.length / 2,
	                        "right": t >= response.data.length / 2
	                    });
	                }
                }
            });
        } else if (ctrl.author) {
            ctrl.title = "Similar authors";
            $http.get(apiUrlWithSaffron + 'authorauthors/' + ctrl.author).then(function(response) {
                ctrl.authors = [];
                response.data.sort((a, b) => (a.similarity < b.similarity) ? 1 : -1);
                for (t = 0; t < response.data.length; t++) {
                	if(response.data[t] != null) {
	                    ctrl.authors.push({
	                        "id": response.data[t].author2_id,
	                        "name": response.data[t].author2_id,
	                        "pos": (t + 1),
	                        "left": t < response.data.length / 2,
	                        "right": t >= response.data.length / 2
	                    });
	                }
                }
            });
        }
    }
});

// <!-- not API ready, still from the JSON files -->
angular.module('app').component('relateddocuments', {
    templateUrl: '/document-list.html',
    bindings: {
        term: '<',
        author: '<'
    },
    controller: function($http, $sce) {
        var ctrl = this;
        ctrl.n = 0;
        ctrl.n2 = 0;
        this.loadTerms = function() {
            if (ctrl.term) {
                $http.get(apiUrlWithSaffron + 'docs/term/' + ctrl.term + '?n=20&offset=' + ctrl.n2).then(function(response) {
                	ctrl.docs = [];
                    for (t = 0; t < response.data.length; t++) {
                        ctrl.docs.push({
                            "doc": response.data[t],
                            "contents_highlighted": $sce.trustAsHtml(response.data[t].contents.split(ctrl.term).join("<b>" + ctrl.term + "</b>")),
                            "pos": (t + 1)
                        });
                    }
                    ctrl.n = ctrl.n2;
                });
            } else if (ctrl.author) {
                $http.get(apiUrlWithSaffron + 'docs/author/' + ctrl.author + '?n=20&offset=' + ctrl.n2).then(function(response) {
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
        this.docForward = function() {
            ctrl.n2 += 20;
            this.loadTerms();
        };
        this.docBackward = function() {
            ctrl.n2 -= 20;
            this.loadTerms();
        };
        this.loadTerms();
    }
});

// <!-- not API ready, still from the JSON files -->
angular.module('app').component('author', {
    templateUrl: '/authors.html',
    controller: function() {
        var ctrl = this;
        if (author) {
            ctrl.author = author;
        }
    }
});

// <!-- not API ready, still from the JSON files -->
angular.module('app').component('doc', {
    templateUrl: '/docs.html',
    controller: function() {
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
    controller: function($http) {
        var ctrl = this;
        ctrl.title = "Metadata";
        if (doc) {
            ctrl.doc = doc;
        }
        this.hasMetadata = function() {
            return Object.keys(ctrl.doc.metadata).length > 0;
        };
        this.hasNoMetadata = function() {
            return Object.keys(ctrl.doc.metadata).length === 0;
        };
    }
});
