# /speckit.constitution
Fill in the constitution with the bare minimum non-negotiable rules for implementation of containerized RESTfull based micro-service application implemented in Java. Exclude the security and encryption of the communication with the microservice.

# /speckit.specify
The project is building a standalone REST microservice, which handles the telecom subscribers' profile information for the online charging system. The REST API is specified in `app-spec/ocs-provisioning-api.yml` OpenAPI specification. It contains CRUD operations on the following entities - subscriber (described in `app-spec/docs/entities/Subscriber.md`), subscription (described in `app-spec/docs/entities/Subscription.md`), group (described in `app-spec/docs/entities/Group.md`), balance (described in `app-spec/docs/entities/Balance.md`), notificationAddress (described in `app-spec/docs/entities/NotificationAddress.md`), timer (described in `app-spec/docs/entities/Timer.md`) and accountHistory (described in `app-spec/docs/entities/AccountHistory.md`). The relationship between the entities is given in `app-spec/data-model.md`. Use the referenced files to extract and define the necessary requirements. Define a list of all cases and for each of them propose a reasonable behavior.  If there are unclear or underspecified requirements, mark them for clarification. The REST application will be running in a docker containerized environment and will use an external RDBMS to store the information. 
The project shall deliver the following:
- REST application source code and binary;
- Dockerfile to build a docker image;
- Docker compose file to run the rest service. It shall contain two containers - one for the REST application and one for the backend RDBMS.
- Script, which deploys the backend RDBMS database schema. The script shall fix the schema in case of missing tables.

# /speckit.plan
The project will be developed using Java 17 and will integrate Spring Boot for application setup, Apache Camel for routing and integration, and Spring Data JPA for database access. Maven will be used as the build tool, with plugins configured to generate interface classes from the OpenAPI specification and to build the application into a Docker container. The backend will use a MySQL database running in a separate Docker container to ensure modularity and ease of deployment.

# /speckit.tasks
Break the specification into simple implementable tasks.

# /speckit.implement 
Implement tasks T001 through T013. For each completed task, update the tasks.md file to reflect its implementation status. If any task is underspecified, request clarification before proceeding.