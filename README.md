## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!


### My solution

At a first look at the challenge the main problem to solve here is how we schedule the payments, doing a bit of research online think we can have 2 approaches to solve this problem:

1) Using some sort of async messages queue, like Kafka, RabbitMQ, etc. This approach will basically require that on invoice creation we will need to send a message to the queue this should be processed at a certain time and then once everything is fine then the consumer would need to reschedule it if the payment fails.
The advantage to this method is that we can more easily scale the consumers to the messages/invoices. The disadvantage is that this solution is a lot more complicated and requires a more infrastructure to be setup.
This sadly doesn't work out of the box since processing messages usually happens real time, but we need them to only be processed on a certain day/schedule which would require some sort of intermediary service, like the one described here for example https://medium.com/@fkarakas/kafka-message-scheduler-69823cc62f8c#:~:text=A%20schedule%20is%20just%20a,specific%20headers%20and%20a%20payload.&text=The%20scheduler%20reads%20the%20topic,planned%20for%20the%20current%20day.

2) Using a recurring job that will start running on the first day of the month and will process all the invoices that are due to be paid. This approach is the one I decided to go with since it's simpler and it doesn't require any external service to be running and fits in the time available for the challenge.

**Observations**:

- Ideally the invoices should be country and currency based as you can have cases where you need to use a different payment provider for different countries (for example in Serbia you can't have the money leave the country)
and countries can change currency(f.ex. Croatia at the end of this year). Decided to ignore this for now, in order to save a bit of time.
- Because there can be some error cases that can't be treated automatically, such as missing customer, wrong currency etc. decided to add a FAILED invoice status, which will then have to be treated manually.

**Description of solution**:

Decided to go with a recurring job that will run on the first and second day of the month, and just return on any other days.

This job will pick a batch of invoices that are in pending(100 by default) and will process all of them, updating the invoice status correctly. Due to
running in small batches we need the job to run multiple times per day in order to process all invoices (which is the main cause I didn't go with a simple cron scheduler).

But an advantage of running this job like this is that we can potentially run multiple instances of the app and have them process the invoices in parallel, but we will
need to run the job logic as a transaction to prevent invoices being processed multiple times.

Also added a retry logic, where for a given exception, such as a network error, the job will retry the invoice 3 times before marking it as failed.

**Assumptions**:
- Invoices can only be split by currency, not by country. And we only have 1 payment provider per currency.
- We only care about the timezone in which the server is running, not the timezone of the customer.

**Improvements**:

- Add integration tests that actually call down to the database to verify that the all queries are correct. (I didn't have time to do this)
- Add some sort of configuration capabilities for the app, such as having a yaml file or loading the config from an external service.
- Update the invoices to also refer/belong to a country to take into account possible business cases where we have payment providers per country.
- Add some sort of load/error handling/backoff mechanism to the 3rd party payment provider. But this can maybe be included in the payment provider itself.
- Update the BillingService code to be run in a transaction (didn't have time to do this as I'm not familiar with Exposed and having coroutines inside a transaction might not work)
- Another optimisation could be to do a bulk update of the invoices in the database instead of updating them one by one.

**Time spent**: Sadly didn't have as much time as I wanted to work on this, but I think I spent around 6 hours on this on Sunday, and a bit on Monday cleaning things up a bit and finishing writing the readme update.
