# SPRING BASE #


### What Is ? ###

It’s a project that contains and groups a set of modules where each  module will supply a specific functionality for develop a service using spring framework.



The modules contained in this project are:

* commons-commons: This library contains the commons classes that will be used in by different common libraries.


* commons-view: This module divided in two other modules (api and core) will contain the necessary classes, interfaces and annotations to allow to build a View from a model classs. Tha api will contains only the interface and annotations that would be used in the api part of a spring service.


* commons-mongo: This library, divided in api and core as the one before, will contain the classes, enums, interfaces and annotations needed to perform dynamic searched in mongo.


* commons-redis: This library, divided into api and core, will allow to read and store data from a redis server in a commons way doing transparent for the user what data structure is stored in redis.


* commons-jpa: Divided into api and core, this library contains the necessary objects to perform dynamic searches in a database using HQL.


* commons-api: This library contains necessary dependencies that a spring service api will need.


* commons-app: This library contains necessary dependencies and object to develop the core of a spring service.

